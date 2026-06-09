#include "TcpClient.h"
#include "core/AppLogger.h"
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
    connect(&m_socket, &QSslSocket::encrypted, this, &TcpClient::onEncrypted);
    connect(&m_socket, &QSslSocket::sslErrors, this, &TcpClient::onSslErrors);
    connect(&m_socket, &QSslSocket::readyRead, this, &TcpClient::onReadyRead);
    connect(&m_socket, &QSslSocket::errorOccurred, this, &TcpClient::onSocketError);
    connect(&m_socket, &QSslSocket::disconnected, this, &TcpClient::onDisconnected);
}

void TcpClient::configure(const QString &host, quint16 port, bool useTls,
                          const QString &certPinSha256, const QString &certPinSha256Backup)
{
    m_host = host;
    m_port = port;
    m_useTls = useTls;
    m_pinner = CertificatePinner(certPinSha256, certPinSha256Backup);
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

    if (m_socket.state() == QAbstractSocket::ConnectedState && (!m_useTls || m_tlsReady)) {
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
    return m_socket.state() == QAbstractSocket::ConnectedState && (!m_useTls || m_tlsReady);
}

void TcpClient::startConnect()
{
    if (m_host.isEmpty() || m_port == 0) {
        failPending(QStringLiteral("未配置服务器地址"));
        return;
    }
    if (m_useTls && !m_pinner.hasPin()) {
        failPending(QStringLiteral("已启用 TLS，请配置证书指纹（certPinSha256）"));
        return;
    }

    m_tlsReady = false;
    m_connectTimer.start();
    if (m_useTls) {
        QSslConfiguration conf = m_socket.sslConfiguration();
        conf.setProtocol(QSsl::TlsV1_3);
        conf.setPeerVerifyMode(QSslSocket::VerifyNone);
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
    if (!m_useTls) {
        onTlsReady();
    }
}

void TcpClient::onEncrypted()
{
    onTlsReady();
}

void TcpClient::onTlsReady()
{
    if (m_useTls) {
        if (!m_pinner.verify(&m_socket)) {
            AppLogger::instance()->error(QStringLiteral("[云端] 证书 Pinning 校验失败"));
            m_socket.abort();
            failPending(QStringLiteral("证书 Pinning 校验失败"));
            return;
        }
        m_tlsReady = true;
    }
    writePendingEnvelope();
}

void TcpClient::onSslErrors(const QList<QSslError> &errors)
{
    Q_UNUSED(errors);
    if (!m_useTls) {
        return;
    }
    if (m_pinner.hasPin() && m_pinner.verify(&m_socket)) {
        m_socket.ignoreSslErrors();
        return;
    }
    AppLogger::instance()->error(QStringLiteral("[云端] TLS 证书校验失败"));
    m_socket.abort();
    failPending(QStringLiteral("TLS 证书校验失败"));
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
    m_tlsReady = false;
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

    if (m_socket.state() == QAbstractSocket::ConnectedState && (!m_useTls || m_tlsReady)) {
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
    m_tlsReady = false;
    emit connectionError(err);
    if (m_waitingResponse && m_pendingCallback) {
        finishActive(false, {}, err);
    }
}

} // namespace vpn
