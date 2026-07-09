#pragma once

#include <QJsonObject>
#include <QObject>
#include <QString>
#include <QVariantMap>

#include "ConfigLoader.h"
#include "ControllerHttpClient.h"
#include "OpenApiProxyService.h"
#include "ProxyLogModel.h"
#include "SessionLogModel.h"

namespace vpnproxy {

class ProxySessionController : public QObject {
    Q_OBJECT
    Q_PROPERTY(QString sessionState READ sessionState NOTIFY sessionStateChanged)
    Q_PROPERTY(QString proxyEndpoint READ proxyEndpoint NOTIFY endpointsChanged)
    Q_PROPERTY(QString adminEndpoint READ adminEndpoint NOTIFY endpointsChanged)
    Q_PROPERTY(QString upstreamUrl READ upstreamUrl NOTIFY upstreamUrlChanged)
    Q_PROPERTY(int tunnelStatus READ tunnelStatus NOTIFY tunnelStatusChanged)
    Q_PROPERTY(SessionLogModel *sessionLogs READ sessionLogs CONSTANT)
    Q_PROPERTY(ProxyLogModel *proxyLogs READ proxyLogs CONSTANT)

public:
    explicit ProxySessionController(const AppConfig &config, QObject *parent = nullptr);

    QString sessionState() const { return m_sessionState; }
    QString proxyEndpoint() const;
    QString adminEndpoint() const { return m_adminEndpoint; }
    QString upstreamUrl() const { return m_upstreamUrl; }
    int tunnelStatus() const { return m_tunnelStatus; }
    SessionLogModel *sessionLogs() const { return m_sessionLogs; }
    ProxyLogModel *proxyLogs() const;

    OpenApiProxyService *proxyService() const { return m_proxy; }
    const AppConfig &config() const { return m_config; }

    Q_INVOKABLE void bootstrap();
    Q_INVOKABLE QJsonObject handleLogin(const QJsonObject &body);
    Q_INVOKABLE QJsonObject handleProbe(const QJsonObject &body);
    Q_INVOKABLE QJsonObject handleLogout();
    Q_INVOKABLE void clearSessionLogs();
    Q_INVOKABLE void clearProxyLogs();

signals:
    void sessionStateChanged();
    void endpointsChanged();
    void upstreamUrlChanged();
    void tunnelStatusChanged();

private:
    void setSessionState(const QString &state);
    void addSessionLog(const QString &type, const QString &message);
    static QVariantMap jsonToLine(const QJsonObject &lineObj);
    static QStringList parseAllowedIps(const QJsonObject &proxyObj, const QStringList &defaults);

    AppConfig m_config;
    ControllerHttpClient *m_controller = nullptr;
    OpenApiProxyService *m_proxy = nullptr;
    SessionLogModel *m_sessionLogs = nullptr;
    QString m_sessionState{QStringLiteral("idle")};
    QString m_adminEndpoint;
    QString m_upstreamUrl;
    int m_tunnelStatus = -1;
    bool m_busy = false;
};

} // namespace vpnproxy
