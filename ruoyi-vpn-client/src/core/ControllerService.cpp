#include "ControllerService.h"
#include "AppLogger.h"
#include "PacketLogUtil.h"
#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>
#include <QNetworkReply>
#include <QNetworkRequest>
#include <QUrl>

namespace vpn {

namespace {

QString extractControllerError(const QJsonObject &obj, const QString &fallback)
{
    const QString messages = obj.value(QStringLiteral("messages")).toString().trimmed();
    if (!messages.isEmpty()) {
        return messages;
    }
    const QString message = obj.value(QStringLiteral("message")).toString().trimmed();
    if (!message.isEmpty()) {
        return message;
    }
    const QString msg = obj.value(QStringLiteral("msg")).toString().trimmed();
    if (!msg.isEmpty()) {
        return msg;
    }
    return fallback;
}

} // namespace

ControllerService::ControllerService(QObject *parent) : QObject(parent) {}

QVariantMap ControllerService::serverToJson(const QVariantMap &server)
{
    QVariantMap obj;
    obj[QStringLiteral("host")] = server.value(QStringLiteral("host")).toString();
    obj[QStringLiteral("srvPort")] = server.value(QStringLiteral("srvPort")).toString();
    obj[QStringLiteral("spaPort")] = server.value(QStringLiteral("spaPort")).toString();
    obj[QStringLiteral("spaKey")] = server.value(QStringLiteral("spaKey")).toString();
    obj[QStringLiteral("enablePortMapping")] = false;
    obj[QStringLiteral("mappingPort")] = server.value(QStringLiteral("srvPort")).toString();
    obj[QStringLiteral("device_spa_enable")] = false;
    return obj;
}

void ControllerService::postJson(const QString &path, const QJsonDocument &doc,
                                 std::function<void(const QJsonObject &)> onSuccess)
{
    const QByteArray requestBody = doc.toJson(QJsonDocument::Compact);
    PacketLogUtil::logControllerSend(QStringLiteral("POST"), path, requestBody);
    QNetworkRequest req(QUrl(m_baseUrl + path));
    req.setHeader(QNetworkRequest::ContentTypeHeader, QStringLiteral("application/json"));
    auto *reply = m_nam.post(req, requestBody);
    connect(reply, &QNetworkReply::finished, this, [this, reply, path, onSuccess = std::move(onSuccess)]() {
        reply->deleteLater();
        const QByteArray responseBody = reply->readAll();
        PacketLogUtil::logControllerReceive(QStringLiteral("POST"), path, responseBody);
        if (reply->error() != QNetworkReply::NoError) {
            const QString err = reply->errorString();
            AppLogger::instance()->error(QStringLiteral("[控制器] POST %1 失败: %2").arg(path, err));
            emit operationFailed(err);
            return;
        }
        const QJsonObject obj = QJsonDocument::fromJson(responseBody).object();
        if (obj.value(QStringLiteral("code")).toString() != QStringLiteral("200")) {
            const QString err = extractControllerError(obj, QStringLiteral("控制器请求失败"));
            AppLogger::instance()->error(QStringLiteral("[控制器] POST %1 失败: %2").arg(path, err));
            emit operationFailed(err);
            return;
        }
        onSuccess(obj);
    });
}

void ControllerService::getJson(const QString &path, std::function<void(const QJsonObject &)> onSuccess)
{
    PacketLogUtil::logControllerSend(QStringLiteral("GET"), path, QByteArray());
    QNetworkRequest req(QUrl(m_baseUrl + path));
    auto *reply = m_nam.get(req);
    connect(reply, &QNetworkReply::finished, this, [this, reply, path, onSuccess = std::move(onSuccess)]() {
        reply->deleteLater();
        const QByteArray responseBody = reply->readAll();
        PacketLogUtil::logControllerReceive(QStringLiteral("GET"), path, responseBody);
        if (reply->error() != QNetworkReply::NoError) {
            const QString err = QStringLiteral("无法连接本地控制器(127.0.0.1:30303)，请确保易安联 Agent 已启动");
            AppLogger::instance()->error(QStringLiteral("[控制器] GET %1 失败: %2").arg(path, reply->errorString()));
            emit operationFailed(err);
            return;
        }
        const QJsonObject obj = QJsonDocument::fromJson(responseBody).object();
        if (obj.value(QStringLiteral("code")).toString() != QStringLiteral("200")) {
            const QString err = extractControllerError(obj, QStringLiteral("控制器请求失败"));
            AppLogger::instance()->error(QStringLiteral("[控制器] GET %1 失败: %2").arg(path, err));
            emit operationFailed(err);
            return;
        }
        onSuccess(obj);
    });
}

void ControllerService::detectServer(const QVariantMap &server)
{
    QJsonArray arr;
    arr.append(QJsonObject::fromVariantMap(serverToJson(server)));
    postJson(QStringLiteral("/api/v1/control/detect"), QJsonDocument(arr),
             [this, server](const QJsonObject &obj) {
        const QJsonArray data = obj.value(QStringLiteral("data")).toArray();
        if (data.isEmpty()) {
            const QString msg = extractControllerError(obj, QString());
            if (msg.isEmpty() || msg.compare(QStringLiteral("Success"), Qt::CaseInsensitive) == 0) {
                QVariantMap result = server;
                result[QStringLiteral("available")] = true;
                AppLogger::instance()->info(
                    QStringLiteral("[控制器] POST /api/v1/control/detect 成功（code=200, messages=Success, data 为空）"));
                emit detectSucceeded(result);
                return;
            }
            const QString err = QStringLiteral("探测未返回数据");
            AppLogger::instance()->error(QStringLiteral("[控制器] POST /api/v1/control/detect 失败: %1").arg(err));
            emit operationFailed(err);
            return;
        }
        emit detectSucceeded(data.first().toObject().toVariantMap());
    });
}

void ControllerService::selectServer(const QVariantMap &server)
{
    postJson(QStringLiteral("/api/v1/control/select"), QJsonDocument(QJsonObject::fromVariantMap(serverToJson(server))),
             [this](const QJsonObject &obj) {
        emit selectSucceeded(obj.value(QStringLiteral("data")).toObject().toVariantMap());
    });
}

void ControllerService::fetchVersions(const QVariantMap &server)
{
    postJson(QStringLiteral("/api/v1/version/latestServer"),
             QJsonDocument(QJsonObject::fromVariantMap(serverToJson(server))),
             [this](const QJsonObject &serverObj) {
        getJson(QStringLiteral("/api/v1/version/current"), [this, serverObj](const QJsonObject &clientObj) {
            const QString sv = serverObj.value(QStringLiteral("data")).toObject().value(QStringLiteral("version")).toString();
            const QString cv = clientObj.value(QStringLiteral("data")).toObject().value(QStringLiteral("localVersion")).toString();
            emit versionsReady(sv, cv);
        });
    });
}

void ControllerService::loginWithAccount(const QString &username, const QString &encryptedPassword)
{
    QJsonObject body;
    body[QStringLiteral("username")] = username;
    body[QStringLiteral("password")] = encryptedPassword;
    postJson(QStringLiteral("/api/v1/user/loginWithAccount"), QJsonDocument(body), [this](const QJsonObject &) {
        emit loginControllerSucceeded();
    });
}

void ControllerService::fetchUserInfo()
{
    getJson(QStringLiteral("/api/v1/user/info"), [this](const QJsonObject &obj) {
        const QString name = obj.value(QStringLiteral("data")).toObject().value(QStringLiteral("name")).toString();
        emit userInfoReady(name);
    });
}

void ControllerService::fetchGatewayList()
{
    getJson(QStringLiteral("/api/v1/gateway/list"), [this](const QJsonObject &obj) {
        const QJsonObject data = obj.value(QStringLiteral("data")).toObject();
        const QJsonArray list = data.value(QStringLiteral("list")).toArray();
        QVariantList gateways;
        for (const QJsonValue &v : list) {
            gateways.append(v.toObject().toVariantMap());
        }
        emit gatewayListReady(gateways, data.value(QStringLiteral("turnOn")).toBool(),
                              data.value(QStringLiteral("tunCode")).toInt());
    });
}

void ControllerService::fetchTunnelStatus()
{
    getJson(QStringLiteral("/api/v1/tunnel/status"), [this](const QJsonObject &obj) {
        const QJsonObject data = obj.value(QStringLiteral("data")).toObject();
        emit tunnelStatusReady(data.value(QStringLiteral("status")).toInt(),
                               data.value(QStringLiteral("reConnect")).toBool());
    });
}

void ControllerService::turnOnGateway(bool turnOn)
{
    QJsonObject body;
    body[QStringLiteral("turnOn")] = turnOn;
    postJson(QStringLiteral("/api/v1/gateway/turnOn"), QJsonDocument(body), [this](const QJsonObject &) {
        emit gatewayTurnOnFinished();
    });
}

void ControllerService::switchGateway(const QString &gatewayId)
{
    QJsonObject body;
    bool ok = false;
    const qint64 idNum = gatewayId.toLongLong(&ok);
    if (ok) {
        body[QStringLiteral("gatewayID")] = idNum;
    } else {
        body[QStringLiteral("gatewayID")] = gatewayId;
    }
    postJson(QStringLiteral("/api/v1/gateway/switch"), QJsonDocument(body), [this](const QJsonObject &) {
        emit gatewaySwitchFinished();
    });
}

void ControllerService::fetchAppList(const QString &serviceName)
{
    QJsonObject body;
    body[QStringLiteral("serviceName")] = serviceName;
    postJson(QStringLiteral("/api/v1/user/getUserGroupedServiceList"), QJsonDocument(body), [this](const QJsonObject &obj) {
        const QJsonArray data = obj.value(QStringLiteral("data")).toArray();
        QVariantList apps;
        for (const QJsonValue &v : data) {
            apps.append(v.toObject().toVariantMap());
        }
        emit appListReady(apps);
    });
}

void ControllerService::controllerLogout()
{
    postJson(QStringLiteral("/api/v1/user/logout"), QJsonDocument(QJsonObject()), [](const QJsonObject &) {});
}

} // namespace vpn
