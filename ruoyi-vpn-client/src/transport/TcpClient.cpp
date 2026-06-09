#include "TcpClient.h"
#include <QSslConfiguration>
#include <QTimer>

namespace vpn {

TcpClient::TcpClient(QObject *parent) : QObject(parent)
{
    m_connectTimer.setSingleShot(true);
    m_connectTimer.setInterval(15000);
    connect(&m_connectTimer, &QTimer::timeout, this, &TcpClient::onConnectTimeout);

    m_responseTimer.setSingleShot(true);
    m_responseTimer.setInterval(30000);
    connect(&m_responseTimer, &QTimer::timeout, this, &TcpClient::onResponseTimeout);

    connect(&m_socket, &QSslSocket::connected, this, &TcpClient::onConnected);
    connect(&m_socket, &QSslSocket::readyRead, this, &TcpClient::onReadyRead);
    connect(&m_socket, &QSslSocket::errorOccurred, this, &TcpClient::onSocketError);
    connect(&m_socket, &QSslSocket::disconnected, this, &TcpClient::onDisconnected);
}

void TcpClient::configure(const QString &host, quint16 port, bool useTls, const QString &certPinSha256)
{
    m_host = host;
    m_port = port;
    m_useTls = useTls;
    m_pinner = CertificatePinner(certPinSha256);
}

void TcpClient::sendEnvelope(const QByteArray &envelopeBytes, ResponseCallback callback)
{
    OutboundRequest request{envelopeBytes, std::move(callback)};
    if (m_waitingResponse) {
        m_queue.enqueue(std::move(request));
        return;
    }

    m_pendingCallback = std::move(request.callback);
    m_pendingEnvelope = request.envelope;
    m_waitingResponse = true;
    m_responseTimer.start();

    if (m_socket.state() == QAbstractSocket::ConnectedState) {
        writePendingEnvelope();
        return;
    }
    if (m_socket.state() == QAbstractSocket::ConnectingState) {
        m_connectTimer.start();
        return;
    }
    startConnect();
}

bool TcpClient::isConnected() const
{
    return m_socket.state() == QAbstractSocket::ConnectedState;
}

void TcpClient::startConnect()
{
    if (m_host.isEmpty() || m_port == 0) {
        failPending(QStringLiteral("未配置服务器地址"));
        return;
    }
    m_connectTimer.start();
    if (m_useTls) {
        QSslConfiguration conf = m_socket.sslConfiguration();
        conf.setProtocol(QSsl::TlsV1_3);
        m_socket.setSslConfiguration(conf);
        m_socket.connectToHostEncrypted(m_host, m_port);
    } else {
        m_socket.connectToHost(m_host, m_port);
    }
}

void TcpClient::writePendingEnvelope()
{
    if (m_pendingEnvelope.isEmpty()) {
        return;
    }
    const QByteArray frame = FrameCodec::encode(m_pendingEnvelope);
    m_pendingEnvelope.clear();
    m_socket.write(frame);
    m_socket.flush();
}

void TcpClient::onConnected()
{
    m_connectTimer.stop();
    if (m_useTls && !m_pinner.verify(&m_socket)) {
        m_socket.abort();
        failPending(QStringLiteral("证书 Pinning 校验失败"));
        return;
    }
    writePendingEnvelope();
}

void TcpClient::onReadyRead()
{
    m_readBuffer.append(m_socket.readAll());
    QByteArray body;
    while (FrameCodec::tryDecode(m_readBuffer, body)) {
        if (m_waitingResponse && m_pendingCallback) {
            m_connectTimer.stop();
            m_responseTimer.stop();
            finishActive(true, body, {});
        }
    }
}

void TcpClient::onSocketError(QAbstractSocket::SocketError)
{
    if (m_socket.state() == QAbstractSocket::ConnectingState || m_waitingResponse) {
        failPending(m_socket.errorString().isEmpty()
                        ? QStringLiteral("网络连接失败")
                        : m_socket.errorString());
    }
}

void TcpClient::onConnectTimeout()
{
    if (m_socket.state() == QAbstractSocket::ConnectingState) {
        m_socket.abort();
    }
    failPending(QStringLiteral("连接服务器超时（%1:%2）").arg(m_host).arg(m_port));
}

void TcpClient::onResponseTimeout()
{
    if (!m_waitingResponse) {
        return;
    }
    m_socket.abort();
    failPending(QStringLiteral("等待服务器响应超时"));
}

void TcpClient::onDisconnected()
{
    if (m_waitingResponse) {
        failPending(QStringLiteral("连接已断开"));
    }
}

void TcpClient::finishActive(bool ok, const QByteArray &body, const QString &err)
{
    if (m_pendingCallback) {
        m_pendingCallback(ok, body, err);
        m_pendingCallback = nullptr;
    }
    m_waitingResponse = false;
    dispatchNext();
}

void TcpClient::dispatchNext()
{
    if (m_waitingResponse || m_queue.isEmpty()) {
        return;
    }

    OutboundRequest request = m_queue.dequeue();
    m_pendingCallback = std::move(request.callback);
    m_pendingEnvelope = request.envelope;
    m_waitingResponse = true;
    m_responseTimer.start();

    if (m_socket.state() == QAbstractSocket::ConnectedState) {
        writePendingEnvelope();
    } else if (m_socket.state() == QAbstractSocket::ConnectingState) {
        m_connectTimer.start();
    } else {
        startConnect();
    }
}

void TcpClient::failPending(const QString &err)
{
    m_connectTimer.stop();
    m_responseTimer.stop();
    m_pendingEnvelope.clear();
    emit connectionError(err);
    if (m_waitingResponse && m_pendingCallback) {
        finishActive(false, {}, err);
    }
}

} // namespace vpn
