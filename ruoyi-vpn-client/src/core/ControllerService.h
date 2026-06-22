#pragma once

#include <QObject>
#include <QNetworkAccessManager>
#include <QVariantMap>
#include <QVariantList>
#include <functional>

namespace vpn {

/** 本地易安联控制器 HTTP API（127.0.0.1:30303） */
class ControllerService : public QObject {
    Q_OBJECT
public:
    explicit ControllerService(QObject *parent = nullptr);

    void setControllerAesEnabled(bool enabled) { m_controllerAesEnabled = enabled; }
    bool controllerAesEnabled() const { return m_controllerAesEnabled; }

    Q_INVOKABLE void detectServer(const QVariantMap &server);
    Q_INVOKABLE void selectServer(const QVariantMap &server);
    Q_INVOKABLE void fetchVersions(const QVariantMap &server);
    Q_INVOKABLE void loginWithAccount(const QString &username, const QString &encryptedPassword);
    Q_INVOKABLE void fetchUserInfo();
    Q_INVOKABLE void fetchGatewayList();
    Q_INVOKABLE void fetchTunnelStatus();
    Q_INVOKABLE void turnOnGateway(bool turnOn);
    Q_INVOKABLE void switchGateway(const QString &gatewayId);
    Q_INVOKABLE void fetchAppList(const QString &serviceName = QString());
    Q_INVOKABLE void controllerLogout();

signals:
    void detectSucceeded(const QVariantMap &serverData);
    void selectSucceeded(const QVariantMap &data);
    void versionsReady(const QString &serverVersion, const QString &clientVersion);
    void loginControllerSucceeded();
    void userInfoReady(const QString &username);
    void gatewayListReady(const QVariantList &gateways, bool turnOn, int tunCode);
    void tunnelStatusReady(int status, bool reConnect);
    void gatewayTurnOnFinished();
    void gatewaySwitchFinished();
    void appListReady(const QVariantList &apps);
    void operationFailed(const QString &message);

private:
    void postJson(const QString &path, const QJsonDocument &doc,
                  std::function<void(const QJsonObject &)> onSuccess);
    void getJson(const QString &path, std::function<void(const QJsonObject &)> onSuccess);
    QByteArray encodeControllerBody(const QByteArray &plainJson) const;
    QByteArray decodeControllerBody(const QByteArray &wireBody) const;
    static QVariantMap serverToJson(const QVariantMap &server);

    QNetworkAccessManager m_nam;
    QString m_baseUrl{QStringLiteral("http://127.0.0.1:30303")};
    bool m_controllerAesEnabled{true};
};

} // namespace vpn
