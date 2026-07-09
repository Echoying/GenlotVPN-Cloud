#include "ControllerHttpClient.h"

#include "crypto/AgentAesCrypto.h"

#include <QEventLoop>
#include <QJsonArray>
#include <QJsonDocument>
#include <QNetworkReply>
#include <QNetworkRequest>
#include <QTimer>
#include <QUrl>

namespace vpnproxy {

namespace {

constexpr int kGatewayPollMax = 20;
constexpr int kGatewayPollIntervalMs = 3000;
constexpr int kProbeDetectTimeoutMs = 25000;

} // namespace

ControllerHttpClient::ControllerHttpClient(const QString &baseUrl, bool controllerAesEnabled,
                                           QObject *parent)
    : QObject(parent)
    , m_baseUrl(baseUrl.trimmed().isEmpty() ? QStringLiteral("http://127.0.0.1:30303") : baseUrl.trimmed())
    , m_controllerAesEnabled(controllerAesEnabled)
{
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
                                                                int timeoutMs)
{
    HttpResult result;
    const QByteArray requestBody = encodeControllerBody(plainJsonBody);
    if (requestBody.isEmpty() && !plainJsonBody.isEmpty()) {
        result.error = QStringLiteral("控制器请求加密失败: %1").arg(path);
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
    QTimer timer;
    timer.setSingleShot(true);
    connect(&timer, &QTimer::timeout, &loop, &QEventLoop::quit);
    connect(reply, &QNetworkReply::finished, &loop, &QEventLoop::quit);
    timer.start(timeoutMs);
    loop.exec();
    if (!timer.isActive()) {
        reply->abort();
        result.error = QStringLiteral("请求超时: %1").arg(path);
        reply->deleteLater();
        return result;
    }
    timer.stop();
    if (reply->error() != QNetworkReply::NoError) {
        result.error = QStringLiteral("无法连接本地控制器 %1: %2").arg(m_baseUrl, reply->errorString());
        reply->deleteLater();
        return result;
    }
    const QByteArray wireBody = reply->readAll();
    reply->deleteLater();
    const QByteArray plainJson = decodeControllerBody(wireBody);
    if (plainJson.isEmpty() && !wireBody.isEmpty()) {
        result.error = QStringLiteral("控制器响应解密失败: %1").arg(path);
        return result;
    }
    QJsonParseError parseError;
    const QJsonDocument doc = QJsonDocument::fromJson(plainJson, &parseError);
    if (parseError.error != QJsonParseError::NoError || !doc.isObject()) {
        result.error = QStringLiteral("控制器响应解析失败: %1").arg(path);
        return result;
    }
    result.body = doc.object();
    if (!isCodeOk(result.body)) {
        result.error = extractError(result.body, QStringLiteral("控制器请求失败"));
        return result;
    }
    result.ok = true;
    return result;
}

ControllerHttpClient::HttpResult ControllerHttpClient::getJson(const QString &path, int timeoutMs)
{
    HttpResult result;
    QNetworkRequest req(QUrl(m_baseUrl + path));
    QNetworkReply *reply = m_nam.get(req);
    QEventLoop loop;
    QTimer timer;
    timer.setSingleShot(true);
    connect(&timer, &QTimer::timeout, &loop, &QEventLoop::quit);
    connect(reply, &QNetworkReply::finished, &loop, &QEventLoop::quit);
    timer.start(timeoutMs);
    loop.exec();
    if (!timer.isActive()) {
        reply->abort();
        result.error = QStringLiteral("请求超时: %1").arg(path);
        reply->deleteLater();
        return result;
    }
    timer.stop();
    if (reply->error() != QNetworkReply::NoError) {
        result.error = QStringLiteral("无法连接本地控制器 %1: %2").arg(m_baseUrl, reply->errorString());
        reply->deleteLater();
        return result;
    }
    const QByteArray wireBody = reply->readAll();
    reply->deleteLater();
    const QByteArray plainJson = decodeControllerBody(wireBody);
    if (plainJson.isEmpty() && !wireBody.isEmpty()) {
        result.error = QStringLiteral("控制器响应解密失败: %1").arg(path);
        return result;
    }
    QJsonParseError parseError;
    const QJsonDocument doc = QJsonDocument::fromJson(plainJson, &parseError);
    if (parseError.error != QJsonParseError::NoError || !doc.isObject()) {
        result.error = QStringLiteral("控制器响应解析失败: %1").arg(path);
        return result;
    }
    result.body = doc.object();
    if (!isCodeOk(result.body)) {
        result.error = extractError(result.body, QStringLiteral("控制器请求失败"));
        return result;
    }
    result.ok = true;
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
                            kProbeDetectTimeoutMs);
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
                            QJsonDocument(QJsonObject::fromVariantMap(serverJson)).toJson(QJsonDocument::Compact));
    if (!selectRes.ok) {
        out.error = selectRes.error;
        return out;
    }

    auto serverVer = postJson(QStringLiteral("/api/v1/version/latestServer"),
                              QJsonDocument(QJsonObject::fromVariantMap(serverJson)).toJson(QJsonDocument::Compact));
    if (!serverVer.ok) {
        out.error = serverVer.error;
        return out;
    }
    auto clientVer = getJson(QStringLiteral("/api/v1/version/current"));
    if (!clientVer.ok) {
        out.error = clientVer.error;
        return out;
    }

    QJsonObject loginBody;
    loginBody[QStringLiteral("username")] = username;
    // password 保持服务端下发的字段级 AES 密文；开启传输加密时由 postJson 再整包加密
    loginBody[QStringLiteral("password")] = password;
    auto loginRes = postJson(QStringLiteral("/api/v1/user/loginWithAccount"),
                             QJsonDocument(loginBody).toJson(QJsonDocument::Compact));
    if (!loginRes.ok) {
        out.error = loginRes.error;
        return out;
    }

    QJsonObject turnOnBody;
    turnOnBody[QStringLiteral("turnOn")] = true;
    auto turnOnRes = postJson(QStringLiteral("/api/v1/gateway/turnOn"),
                              QJsonDocument(turnOnBody).toJson(QJsonDocument::Compact));
    if (!turnOnRes.ok) {
        out.error = turnOnRes.error;
        return out;
    }

    for (int attempt = 0; attempt < kGatewayPollMax; ++attempt) {
        auto gwRes = getJson(QStringLiteral("/api/v1/gateway/list"));
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
        auto tunRes = getJson(QStringLiteral("/api/v1/tunnel/status"));
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
    auto res = postJson(QStringLiteral("/api/v1/user/logout"), QByteArrayLiteral("{}"));
    if (!res.ok && errorOut) {
        *errorOut = res.error;
    }
    return res.ok;
}

} // namespace vpnproxy
