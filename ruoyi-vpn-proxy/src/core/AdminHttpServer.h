#pragma once

#include <QHash>
#include <QObject>
#include <QTcpServer>

namespace vpnproxy {

class ProxySessionController;

class AdminHttpServer : public QObject {
    Q_OBJECT
public:
    explicit AdminHttpServer(ProxySessionController *controller, QObject *parent = nullptr);
    bool start(const QString &host, int port);

private slots:
    void onNewConnection();
    void onClientReadyRead();

private:
    void handleRequest(class QTcpSocket *client, const QByteArray &rawRequest);

    ProxySessionController *m_controller = nullptr;
    QTcpServer m_server;
    QHash<class QTcpSocket *, QByteArray> m_buffers;
};

} // namespace vpnproxy
