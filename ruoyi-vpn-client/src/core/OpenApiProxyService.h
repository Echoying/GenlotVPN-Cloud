#pragma once

#include <QObject>
#include <QString>
#include <QStringList>
#include <QTcpServer>
#include <QTcpSocket>
#include <QHash>
#include "ProxyLogModel.h"

namespace vpn {

/**
 * 易安联 OpenAPI 反向代理（VPN 连通后由服务端按线路鉴权并下发配置）
 */
class OpenApiProxyService : public QObject {
    Q_OBJECT
public:
    explicit OpenApiProxyService(QObject *parent = nullptr);
    ~OpenApiProxyService() override;

    bool isRunning() const { return m_running; }
    QString listenEndpoint() const;
    ProxyLogModel *logModel() const { return m_logModel; }

    Q_INVOKABLE void stop();
    Q_INVOKABLE bool start(const QString &upstreamUrl, const QString &listenHost, int listenPort,
                           const QStringList &allowedSourceIps);
    Q_INVOKABLE void clearLogs();

signals:
    void started(const QString &endpoint);
    void stopped();
    void logMessage(const QString &type, const QString &message);

private slots:
    void onNewConnection();
    void onClientReadyRead();

private:
    void closeServer();
    bool isAllowedPeer(const QString &peerIp) const;
    QString buildTargetUrl(const QString &upstreamUrl, const QString &requestPath,
                           const QString &queryString) const;
    void forwardRequest(class QTcpSocket *client, const QByteArray &rawRequest);
    void recordLog(const QString &method, const QString &path, int status, const QString &peerIp,
                   const QString &requestLog, const QString &responseLog, bool success);

    QTcpServer m_server;
    ProxyLogModel *m_logModel = nullptr;
    QHash<QTcpSocket *, QByteArray> m_clientBuffers;
    QString m_upstreamUrl;
    QString m_listenHost;
    int m_listenPort = 0;
    QStringList m_allowedSourceIps;
    bool m_running = false;
    qint64 m_logSeq = 0;
};

} // namespace vpn
