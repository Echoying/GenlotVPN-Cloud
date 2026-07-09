#pragma once

#include <QObject>
#include <QString>
#include <QStringList>
#include <QTcpServer>
#include <QTcpSocket>
#include <QHash>
#include "ProxyLogModel.h"

namespace vpnproxy {

class OpenApiProxyService : public QObject {
    Q_OBJECT
public:
    explicit OpenApiProxyService(QObject *parent = nullptr);
    ~OpenApiProxyService() override;

    bool isRunning() const { return m_running; }
    bool isForwardingReady() const { return m_running && m_vpnReady && !m_upstreamUrl.isEmpty(); }
    QString listenEndpoint() const;
    ProxyLogModel *logModel() const { return m_logModel; }

    bool startListenerOnly(const QString &listenHost, int listenPort, const QStringList &allowedSourceIps);
    void updateUpstream(const QString &upstreamUrl);
    void updateAllowedSourceIps(const QStringList &allowedSourceIps);
    void clearUpstream();
    void setVpnReady(bool ready);
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
    void forwardRequest(QTcpSocket *client, const QByteArray &rawRequest);
    void recordLog(const QString &method, const QString &path, int status, const QString &peerIp,
                   const QString &requestLog, const QString &responseLog, bool success);
    void respondServiceUnavailable(QTcpSocket *client, const QString &peer, const QString &method,
                                   const QString &path, const QString &requestLog);

    QTcpServer m_server;
    ProxyLogModel *m_logModel = nullptr;
    QHash<QTcpSocket *, QByteArray> m_clientBuffers;
    QString m_upstreamUrl;
    QString m_listenHost;
    int m_listenPort = 0;
    QStringList m_allowedSourceIps;
    bool m_running = false;
    bool m_vpnReady = false;
    qint64 m_logSeq = 0;
};

} // namespace vpnproxy
