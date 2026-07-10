#include "ControllerHttpClient.h"

#include "crypto/AgentAesCrypto.h"

#include <QElapsedTimer>
#include <QEventLoop>
#include <QJsonArray>
#include <QJsonDocument>
#include <QNetworkReply>
#include <QNetworkRequest>
#include <QSslConfiguration>
#include <QSslSocket>
#include <QThread>
#include <QTimer>
#include <QUrl>

namespace vpnproxy {

namespace {

constexpr int kGatewayPollMax = 20;
constexpr int kGatewayPollIntervalMs = 3000;
constexpr int kProbeDetectTimeoutMs = 25000;
constexpr int kDefaultPostTimeoutMs = 120000;
constexpr int kDefaultGetTimeoutMs = 30000;
constexpr int kUpstreamReadyPollMax = 10;
constexpr int kUpstreamReadyPollIntervalMs = 1000;
constexpr int kUpstreamReadyPollTimeoutMs = 10000;
constexpr char kTokenPath[] = "/enadmin/api/open/v1/token";

} // namespace

ControllerHttpClient::ControllerHttpClient(const QString &baseUrl, bool controllerAesEnabled,
                                           QObject *parent)
    : QObject(parent)
    , m_baseUrl(baseUrl.trimmed().isEmpty() ? QStringLiteral("http://127.0.0.1:30303") : baseUrl.trimmed())
    , m_controllerAesEnabled(controllerAesEnabled)
{
}

void ControllerHttpClient::setSessionLogCallback(SessionLogCallback callback)
{
    m_sessionLogCallback = std::move(callback);
}

QVariantMap ControllerHttpClient::lineToJson(const QVariantMap &line)
{
    QVariantMap obj;
    obj[QStringLiteral("host")] = line.value(QStringLiteral("host")).toString();
    obj[QStringLiteral("srvPort")] = line.value(QStringLiteral("srvPort")).toString();
    obj[QStringLiteral("spaPort")] = line.value(QStringLiteral("spaPort")).toString();
    obj[QStringLiteral("spaKey")] = line.value(QStringLiteral("spaKey")).toString();
    obj[QStringLiteral("enablePortMapping")] = false;
    obj[QStringLiteral("mappingPort")] = line.value(QStringLiteral("srvPort")).toString();
    obj[QStringLiteral("device_spa_enable")] = false;
    return obj;
}

QString ControllerHttpClient::extractError(const QJsonObject &obj, const QString &fallback)
{
    const QString messages = obj.value(QStringLiteral("messages")).toString().trimmed();
    if (!messages.isEmpty()) {
        return messages;
    }
    const QString message = obj.value(QStringLiteral("message")).toString().trimmed();
    if (!message.isEmpty()) {
        return message;
    }
    return fallback;
}

bool ControllerHttpClient::isCodeOk(const QJsonObject &obj)
{
    const QString code = obj.value(QStringLiteral("code")).toString().trimmed();
    return code == QStringLiteral("200");
}

QString ControllerHttpClient::truncateForLog(const QString &text, int maxLen)
{
    if (text.size() <= maxLen) {
        return text;
    }
    return text.left(maxLen) + QStringLiteral("\n... [已截断，共 %1 字符]").arg(text.size());
}

QString ControllerHttpClient::formatJsonForLog(const QByteArray &plainJson)
{
    if (plainJson.isEmpty()) {
        return QStringLiteral("(空)");
    }
    QJsonParseError parseError;
    const QJsonDocument doc = QJsonDocument::fromJson(plainJson, &parseError);
    if (parseError.error == QJsonParseError::NoError) {
        const QByteArray compact = doc.toJson(QJsonDocument::Compact);
        return truncateForLog(QString::fromUtf8(compact));
    }
    return truncateForLog(QString::fromUtf8(plainJson));
}

void ControllerHttpClient::logHttpStep(const QString &stepLabel, const QString &method,
                                       const QString &path, qint64 elapsedMs,
                                       const QString &requestPlain, const HttpResult &result)
{
    if (!m_sessionLogCallback) {
        return;
    }
    const QString label = stepLabel.isEmpty() ? path : stepLabel;
    SessionLogPayload payload;
    payload.type = result.ok ? QStringLiteral("info") : QStringLiteral("error");
    payload.message = QStringLiteral("[30303 %1] %2 %3").arg(label, method, path);
    payload.requestLog = requestPlain.isEmpty() ? QStringLiteral("(无)") : requestPlain;
    payload.elapsedMs = elapsedMs;
    if (result.ok) {
        payload.responseLog = truncateForLog(
            QString::fromUtf8(QJsonDocument(result.body).toJson(QJsonDocument::Compact)));
    } else {
        payload.responseLog = result.error;
    }
    m_sessionLogCallback(payload);
}

void ControllerHttpClient::logUpstreamProbe(int attempt, int maxAttempts, qint64 elapsedMs,
                                            const QString &tokenUrl, const QString &requestBody,
                                            bool reachable, const QString &responseLog)
{
    if (!m_sessionLogCallback) {
        return;
    }
    SessionLogPayload payload;
    payload.type = (reachable || attempt < maxAttempts) ? QStringLiteral("info") : QStringLiteral("error");
    payload.message = QStringLiteral("[上游就绪检测 %1/%2] POST %3")
                          .arg(attempt)
                          .arg(maxAttempts)
                          .arg(tokenUrl);
    payload.requestLog = requestBody;
    payload.responseLog = responseLog;
    payload.elapsedMs = elapsedMs;
    m_sessionLogCallback(payload);
}

QString ControllerHttpClient::buildTokenUrl(const QString &upstreamUrl)
{
    QString base = upstreamUrl.trimmed();
    while (base.endsWith(QLatin1Char('/'))) {
        base.chop(1);
    }
    if (base.endsWith(QString::fromLatin1(kTokenPath))) {
        return base;
    }
    if (base.endsWith(QStringLiteral("/enadmin/api/open/v1"))) {
        return base + QStringLiteral("/token");
    }
    return base + QString::fromLatin1(kTokenPath);
}

bool ControllerHttpClient::isUpstreamUnreachableError(QNetworkReply::NetworkError error)
{
    switch (error) {
    case QNetworkReply::NoError:
        return false;
    case QNetworkReply::TimeoutError:
    case QNetworkReply::OperationCanceledError:
    case QNetworkReply::ConnectionRefusedError:
    case QNetworkReply::RemoteHostClosedError:
    case QNetworkReply::HostNotFoundError:
    case QNetworkReply::NetworkSessionFailedError:
    case QNetworkReply::TemporaryNetworkFailureError:
    case QNetworkReply::UnknownNetworkError:
        return true;
    default:
        // 收到 HTTP 响应但状态码异常、SSL 握手失败等，视为已连通
        return false;
    }
}

QByteArray ControllerHttpClient::encodeControllerBody(const QByteArray &plainJson) const
{
    if (!m_controllerAesEnabled) {
        return plainJson;
    }
    const QString cipher = AgentAesCrypto::encryptToBase64(plainJson);
    if (cipher.isEmpty() && !plainJson.isEmpty()) {
        return QByteArray();
    }
    return cipher.toLatin1();
}

QByteArray ControllerHttpClient::decodeControllerBody(const QByteArray &wireBody) const
{
    if (!m_controllerAesEnabled) {
        return wireBody;
    }
    const QByteArray plain = AgentAesCrypto::decryptFromBase64(wireBody);
    if (plain.isEmpty() && !wireBody.isEmpty()) {
        return QByteArray();
    }
    return plain;
}

ControllerHttpClient::HttpResult ControllerHttpClient::postJson(const QString &path,
                                                                const QByteArray &plainJsonBody,
                                                                int timeoutMs,
                                                                const QString &stepLabel)
{
    QElapsedTimer timer;
    timer.start();

    HttpResult result;
    const QString requestPlain = formatJsonForLog(plainJsonBody);
    const QByteArray requestBody = encodeControllerBody(plainJsonBody);
    if (requestBody.isEmpty() && !plainJsonBody.isEmpty()) {
        result.error = QStringLiteral("控制器请求加密失败: %1").arg(path);
        logHttpStep(stepLabel, QStringLiteral("POST"), path, timer.elapsed(), requestPlain, result);
        return result;
    }

    QNetworkRequest req(QUrl(m_baseUrl + path));
    if (m_controllerAesEnabled) {
        req.setHeader(QNetworkRequest::ContentTypeHeader, QStringLiteral("text/plain; charset=UTF-8"));
    } else {
        req.setHeader(QNetworkRequest::ContentTypeHeader, QStringLiteral("application/json"));
    }
    QNetworkReply *reply = m_nam.post(req, requestBody);
    QEventLoop loop;
    QTimer waitTimer;
    waitTimer.setSingleShot(true);
    connect(&waitTimer, &QTimer::timeout, &loop, &QEventLoop::quit);
    connect(reply, &QNetworkReply::finished, &loop, &QEventLoop::quit);
    waitTimer.start(timeoutMs);
    loop.exec();
    if (!waitTimer.isActive()) {
        reply->abort();
        result.error = QStringLiteral("请求超时: %1").arg(path);
        reply->deleteLater();
        logHttpStep(stepLabel, QStringLiteral("POST"), path, timer.elapsed(), requestPlain, result);
        return result;
    }
    waitTimer.stop();
    if (reply->error() != QNetworkReply::NoError) {
        result.error = QStringLiteral("无法连接本地控制器 %1: %2").arg(m_baseUrl, reply->errorString());
        reply->deleteLater();
        logHttpStep(stepLabel, QStringLiteral("POST"), path, timer.elapsed(), requestPlain, result);
        return result;
    }
    const QByteArray wireBody = reply->readAll();
    reply->deleteLater();
    const QByteArray plainJson = decodeControllerBody(wireBody);
    if (plainJson.isEmpty() && !wireBody.isEmpty()) {
        result.error = QStringLiteral("控制器响应解密失败: %1").arg(path);
        logHttpStep(stepLabel, QStringLiteral("POST"), path, timer.elapsed(), requestPlain, result);
        return result;
    }
    QJsonParseError parseError;
    const QJsonDocument doc = QJsonDocument::fromJson(plainJson, &parseError);
    if (parseError.error != QJsonParseError::NoError || !doc.isObject()) {
        result.error = QStringLiteral("控制器响应解析失败: %1").arg(path);
        logHttpStep(stepLabel, QStringLiteral("POST"), path, timer.elapsed(), requestPlain, result);
        return result;
    }
    result.body = doc.object();
    if (!isCodeOk(result.body)) {
        result.error = extractError(result.body, QStringLiteral("控制器请求失败"));
        logHttpStep(stepLabel, QStringLiteral("POST"), path, timer.elapsed(), requestPlain, result);
        return result;
    }
    result.ok = true;
    logHttpStep(stepLabel, QStringLiteral("POST"), path, timer.elapsed(), requestPlain, result);
    return result;
}

ControllerHttpClient::HttpResult ControllerHttpClient::getJson(const QString &path, int timeoutMs,
                                                               const QString &stepLabel)
{
    QElapsedTimer timer;
    timer.start();

    HttpResult result;
    const QString requestPlain = QStringLiteral("(无)");
    QNetworkRequest req(QUrl(m_baseUrl + path));
    QNetworkReply *reply = m_nam.get(req);
    QEventLoop loop;
    QTimer waitTimer;
    waitTimer.setSingleShot(true);
    connect(&waitTimer, &QTimer::timeout, &loop, &QEventLoop::quit);
    connect(reply, &QNetworkReply::finished, &loop, &QEventLoop::quit);
    waitTimer.start(timeoutMs);
    loop.exec();
    if (!waitTimer.isActive()) {
        reply->abort();
        result.error = QStringLiteral("请求超时: %1").arg(path);
        reply->deleteLater();
        logHttpStep(stepLabel, QStringLiteral("GET"), path, timer.elapsed(), requestPlain, result);
        return result;
    }
    waitTimer.stop();
    if (reply->error() != QNetworkReply::NoError) {
        result.error = QStringLiteral("无法连接本地控制器 %1: %2").arg(m_baseUrl, reply->errorString());
        reply->deleteLater();
        logHttpStep(stepLabel, QStringLiteral("GET"), path, timer.elapsed(), requestPlain, result);
        return result;
    }
    const QByteArray wireBody = reply->readAll();
    reply->deleteLater();
    const QByteArray plainJson = decodeControllerBody(wireBody);
    if (plainJson.isEmpty() && !wireBody.isEmpty()) {
        result.error = QStringLiteral("控制器响应解密失败: %1").arg(path);
        logHttpStep(stepLabel, QStringLiteral("GET"), path, timer.elapsed(), requestPlain, result);
        return result;
    }
    QJsonParseError parseError;
    const QJsonDocument doc = QJsonDocument::fromJson(plainJson, &parseError);
    if (parseError.error != QJsonParseError::NoError || !doc.isObject()) {
        result.error = QStringLiteral("控制器响应解析失败: %1").arg(path);
        logHttpStep(stepLabel, QStringLiteral("GET"), path, timer.elapsed(), requestPlain, result);
        return result;
    }
    result.body = doc.object();
    if (!isCodeOk(result.body)) {
        result.error = extractError(result.body, QStringLiteral("控制器请求失败"));
        logHttpStep(stepLabel, QStringLiteral("GET"), path, timer.elapsed(), requestPlain, result);
        return result;
    }
    result.ok = true;
    logHttpStep(stepLabel, QStringLiteral("GET"), path, timer.elapsed(), requestPlain, result);
    return result;
}

ControllerHttpClient::DetectResult ControllerHttpClient::detectLine(const QVariantMap &line)
{
    DetectResult out;
    const QVariantMap serverJson = lineToJson(line);

    QJsonArray detectArr;
    detectArr.append(QJsonObject::fromVariantMap(serverJson));
    auto detectRes = postJson(QStringLiteral("/api/v1/control/detect"),
                            QJsonDocument(detectArr).toJson(QJsonDocument::Compact),
                            kProbeDetectTimeoutMs,
                            QStringLiteral("detect"));
    if (!detectRes.ok) {
        out.error = detectRes.error;
        return out;
    }
    const QJsonArray detectData = detectRes.body.value(QStringLiteral("data")).toArray();
    if (detectData.isEmpty()) {
        out.error = QStringLiteral("线路探测失败，无返回数据");
        return out;
    }
    out.ok = true;
    out.available = detectData.first().toObject().value(QStringLiteral("available")).toBool();
    if (!out.available) {
        out.error = QStringLiteral("线路探测失败，服务器不可用");
    }
    return out;
}

ControllerHttpClient::ConnectResult ControllerHttpClient::connectLine(const QVariantMap &line,
                                                                        const QString &username,
                                                                        const QString &password)
{
    ConnectResult out;
    const QVariantMap serverJson = lineToJson(line);

    const DetectResult detectOut = detectLine(line);
    if (!detectOut.ok) {
        out.error = detectOut.error;
        return out;
    }
    if (!detectOut.available) {
        out.error = detectOut.error;
        return out;
    }

    auto selectRes = postJson(QStringLiteral("/api/v1/control/select"),
                            QJsonDocument(QJsonObject::fromVariantMap(serverJson)).toJson(QJsonDocument::Compact),
                            kDefaultPostTimeoutMs,
                            QStringLiteral("select"));
    if (!selectRes.ok) {
        out.error = selectRes.error;
        return out;
    }

    auto serverVer = postJson(QStringLiteral("/api/v1/version/latestServer"),
                              QJsonDocument(QJsonObject::fromVariantMap(serverJson)).toJson(QJsonDocument::Compact),
                              kDefaultPostTimeoutMs,
                              QStringLiteral("latestServer"));
    if (!serverVer.ok) {
        out.error = serverVer.error;
        return out;
    }
    auto clientVer = getJson(QStringLiteral("/api/v1/version/current"),
                             kDefaultGetTimeoutMs,
                             QStringLiteral("currentVersion"));
    if (!clientVer.ok) {
        out.error = clientVer.error;
        return out;
    }

    QJsonObject loginBody;
    loginBody[QStringLiteral("username")] = username;
    // password 保持服务端下发的字段级 AES 密文；开启传输加密时由 postJson 再整包加密
    loginBody[QStringLiteral("password")] = password;
    auto loginRes = postJson(QStringLiteral("/api/v1/user/loginWithAccount"),
                             QJsonDocument(loginBody).toJson(QJsonDocument::Compact),
                             kDefaultPostTimeoutMs,
                             QStringLiteral("loginWithAccount"));
    if (!loginRes.ok) {
        out.error = loginRes.error;
        return out;
    }

    QJsonObject turnOnBody;
    turnOnBody[QStringLiteral("turnOn")] = true;
    auto turnOnRes = postJson(QStringLiteral("/api/v1/gateway/turnOn"),
                              QJsonDocument(turnOnBody).toJson(QJsonDocument::Compact),
                              kDefaultPostTimeoutMs,
                              QStringLiteral("gatewayTurnOn"));
    if (!turnOnRes.ok) {
        out.error = turnOnRes.error;
        return out;
    }

    for (int attempt = 0; attempt < kGatewayPollMax; ++attempt) {
        const QString pollLabel = QStringLiteral("#%1/%2").arg(attempt + 1).arg(kGatewayPollMax);
        auto gwRes = getJson(QStringLiteral("/api/v1/gateway/list"),
                             kDefaultGetTimeoutMs,
                             QStringLiteral("gatewayList") + pollLabel);
        if (!gwRes.ok) {
            out.error = gwRes.error;
            return out;
        }
        const QJsonObject gwData = gwRes.body.value(QStringLiteral("data")).toObject();
        const int tunCode = gwData.value(QStringLiteral("tunCode")).toInt();
        if (tunCode == 200) {
            out.tunnelStatus = 2;
            out.ok = true;
            return out;
        }
        auto tunRes = getJson(QStringLiteral("/api/v1/tunnel/status"),
                              kDefaultGetTimeoutMs,
                              QStringLiteral("tunnelStatus") + pollLabel);
        if (tunRes.ok) {
            const int status = tunRes.body.value(QStringLiteral("data")).toObject().value(QStringLiteral("status")).toInt();
            out.tunnelStatus = status;
            if (status == 2) {
                out.ok = true;
                return out;
            }
        }
        QEventLoop waitLoop;
        QTimer::singleShot(kGatewayPollIntervalMs, &waitLoop, &QEventLoop::quit);
        waitLoop.exec();
    }

    out.error = QStringLiteral("网关连接超时");
    return out;
}

bool ControllerHttpClient::logout(QString *errorOut)
{
    auto res = postJson(QStringLiteral("/api/v1/user/logout"),
                        QByteArrayLiteral("{}"),
                        kDefaultPostTimeoutMs,
                        QStringLiteral("logout"));
    if (!res.ok && errorOut) {
        *errorOut = res.error;
    }
    return res.ok;
}

ControllerHttpClient::UpstreamReadyResult
ControllerHttpClient::waitUpstreamTokenReachable(const QString &upstreamUrl)
{
    UpstreamReadyResult out;
    const QString tokenUrl = buildTokenUrl(upstreamUrl);
    if (tokenUrl.isEmpty()) {
        out.error = QStringLiteral("上游地址为空，无法探测 token 接口");
        return out;
    }

    const QByteArray requestBody = QByteArrayLiteral("{\"appId\":\"probe\",\"appSecret\":\"probe\"}");
    const QString requestPlain = formatJsonForLog(requestBody);

    for (int attempt = 1; attempt <= kUpstreamReadyPollMax; ++attempt) {
        QElapsedTimer timer;
        timer.start();

        QNetworkRequest req{QUrl(tokenUrl)};
        QSslConfiguration ssl = QSslConfiguration::defaultConfiguration();
        ssl.setPeerVerifyMode(QSslSocket::VerifyNone);
        req.setSslConfiguration(ssl);
        req.setHeader(QNetworkRequest::ContentTypeHeader, QStringLiteral("application/json"));

        QNetworkReply *reply = m_nam.post(req, requestBody);
        QEventLoop loop;
        QTimer waitTimer;
        waitTimer.setSingleShot(true);
        QObject::connect(&waitTimer, &QTimer::timeout, &loop, &QEventLoop::quit);
        QObject::connect(reply, &QNetworkReply::finished, &loop, &QEventLoop::quit);
        waitTimer.start(kUpstreamReadyPollTimeoutMs);
        loop.exec();

        const qint64 elapsedMs = timer.elapsed();
        QString responseLog;
        bool reachable = false;

        if (!waitTimer.isActive()) {
            reply->abort();
            responseLog = QStringLiteral("请求超时");
        } else {
            waitTimer.stop();
            const QNetworkReply::NetworkError netError = reply->error();
            if (isUpstreamUnreachableError(netError)) {
                responseLog = netError == QNetworkReply::NoError
                                  ? QStringLiteral("未知网络错误")
                                  : reply->errorString();
            } else {
                const int httpStatus = reply->attribute(QNetworkRequest::HttpStatusCodeAttribute).toInt();
                const QByteArray body = reply->readAll();
                responseLog = QStringLiteral("HTTP %1\n%2")
                                    .arg(httpStatus > 0 ? httpStatus : 502)
                                    .arg(truncateForLog(QString::fromUtf8(body)));
                reachable = true;
            }
        }
        reply->deleteLater();

        logUpstreamProbe(attempt, kUpstreamReadyPollMax, elapsedMs, tokenUrl, requestPlain,
                         reachable, responseLog);

        if (reachable) {
            out.ok = true;
            return out;
        }

        if (attempt < kUpstreamReadyPollMax) {
            QThread::msleep(static_cast<unsigned long>(kUpstreamReadyPollIntervalMs));
        }
    }

    out.error = QStringLiteral("上游 token 接口不可达，已轮询 %1 次: %2")
                    .arg(kUpstreamReadyPollMax)
                    .arg(tokenUrl);
    return out;
}

} // namespace vpnproxy
