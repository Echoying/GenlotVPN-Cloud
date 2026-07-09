#include "SingleInstance.h"
#include "AppLogger.h"

#include <QLocalServer>
#include <QLocalSocket>

namespace vpnproxy {

SingleInstance::SingleInstance(const QString &serverName, QObject *parent)
    : QObject(parent)
    , m_serverName(serverName)
{
}

bool SingleInstance::tryRun()
{
    QLocalSocket probe;
    probe.connectToServer(m_serverName);
    if (probe.waitForConnected(400)) {
        probe.write("activate");
        probe.flush();
        probe.waitForBytesWritten(500);
        probe.disconnectFromServer();
        AppLogger::instance()->info(QStringLiteral("[单实例] 检测到已有进程"));
        return false;
    }

    QLocalServer::removeServer(m_serverName);
    m_server = new QLocalServer(this);
    if (!m_server->listen(m_serverName)) {
        QLocalServer::removeServer(m_serverName);
        if (!m_server->listen(m_serverName)) {
            AppLogger::instance()->warn(QStringLiteral("[单实例] 无法创建锁，允许多开"));
            delete m_server;
            m_server = nullptr;
            return true;
        }
    }
    connect(m_server, &QLocalServer::newConnection, this, &SingleInstance::onNewConnection);
    return true;
}

void SingleInstance::onNewConnection()
{
    QLocalSocket *socket = m_server ? m_server->nextPendingConnection() : nullptr;
    if (!socket) {
        return;
    }
    connect(socket, &QLocalSocket::readyRead, this, [this, socket]() {
        socket->readAll();
        emit activateRequested();
        socket->disconnectFromServer();
        socket->deleteLater();
    });
    connect(socket, &QLocalSocket::disconnected, socket, &QLocalSocket::deleteLater);
}

} // namespace vpnproxy
