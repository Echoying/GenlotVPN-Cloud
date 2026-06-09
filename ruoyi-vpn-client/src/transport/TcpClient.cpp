#include "TcpClient.h"
#include "core/AppLogger.h"
#include <QSslConfiguration>
#include <QTimer>
#include <QStringList>

namespace vpn {

namespace {

QString humanizeConnectError(const QString &raw, bool useTls)
{
    const QString err = raw.trimmed();
    if (err.isEmpty()) {
        return useTls ? QStringLiteral("TLS 连接失败，请检查证书指纹与服务端 TLS 配置")
                      : QStringLiteral("网络连接失败");
    }

    const QString lower = err.toLower();
    const bool protocolRelated = lower.contains(QStringLiteral("protocol"))
                                 || lower.contains(QStringLiteral("ssl"))
                                 || lower.contains(QStringLiteral("tls"))
                                 || err.contains(QStringLiteral("协议"));
    if (err.contains(QStringLiteral("不支持的功能"))
        || lower.contains(QStringLiteral("unsupported function"))) {
        return QStringLiteral(
            "TLS 握手失败（Windows 与服务器 TLS 特性不兼容）。请重启 ruoyi-vpn-auth 使服务端支持 TLS 1.2+1.3，"
            "并确认 config.json 中 useTls 为 true。");
    }
    if (protocolRelated) {
        if (useTls) {
            return QStringLiteral("TLS 握手失败（%1）").arg(err);
        }
        return QStringLiteral(
            "连接失败：服务端已启用 TLS，请在 config.json 设 useTls 为 true 并填写 certPinSha256。");
    }
    return err;
}

QString sslProtocolLabel(QSsl::SslProtocol protocol)
{
    switch (protocol) {
    case QSsl::TlsV1_0:
        return QStringLiteral("TLSv1.0");
    case QSsl::TlsV1_1:
        return QStringLiteral("TLSv1.1");
    case QSsl::TlsV1_2:
        return QStringLiteral("TLSv1.2");
    case QSsl::TlsV1_3:
        return QStringLiteral("TLSv1.3");
    case QSsl::DtlsV1_0:
        return QStringLiteral("DTLSv1.0");
    case QSsl::DtlsV1_2:
        return QStringLiteral("DTLSv1.2");
    case QSsl::DtlsV1_2OrLater:
        return QStringLiteral("DTLSv1.2+");
    case QSsl::TlsV1_2OrLater:
        return QStringLiteral("TLSv1.2+");
    case QSsl::SecureProtocols:
        return QStringLiteral("SecureProtocols");
    case QSsl::AnyProtocol:
        return QStringLiteral("AnyProtocol");
    default:
        return QStringLiteral("Unknown(%1)").arg(static_cast<int>(protocol));
    }
}

} // namespace

void TcpClient::logLocalTlsCapabilities()
{
    auto *logger = AppLogger::instance();
    if (!QSslSocket::supportsSsl()) {
        logger->warn(QStringLiteral("[TLS] 本机未检测到可用的 SSL/TLS 后端"));
        return;
    }

    const QString backend = QSslSocket::activeBackend();
    const QString runtimeVer = QSslSocket::sslLibraryVersionString();
    const QString buildVer = QSslSocket::sslLibraryBuildVersionString();

    const QStringList backends = QSslSocket::availableBackends();
    if (!backends.isEmpty()) {
        logger->info(QStringLiteral("[TLS] 可用后端: %1").arg(backends.join(QStringLiteral(", "))));
    }

    QStringList versions;
    for (const QSsl::SslProtocol protocol : QSslSocket::supportedProtocols(backend)) {
        versions.append(sslProtocolLabel(protocol));
    }
    versions.removeDuplicates();

    logger->info(QStringLiteral("[TLS] 当前后端: %1").arg(backend.isEmpty() ? QStringLiteral("(未知)") : backend));
    logger->info(QStringLiteral("[TLS] TLSv1.2 可用: %1").arg(
        QSslSocket::isProtocolSupported(QSsl::TlsV1_2, backend) ? QStringLiteral("是") : QStringLiteral("否")));
    logger->info(QStringLiteral("[TLS] TLSv1.3 可用: %1").arg(
        QSslSocket::isProtocolSupported(QSsl::TlsV1_3, backend) ? QStringLiteral("是") : QStringLiteral("否")));
    if (!runtimeVer.isEmpty()) {
        logger->info(QStringLiteral("[TLS] 运行时库: %1").arg(runtimeVer));
    }
    if (!buildVer.isEmpty() && buildVer != runtimeVer) {
        logger->info(QStringLiteral("[TLS] 构建时库: %1").arg(buildVer));
    }
    if (versions.isEmpty()) {
        logger->warn(QStringLiteral("[TLS] 本机支持的协议版本: （无）"));
    } else {
        logger->info(QStringLiteral("[TLS] 本机支持的协议版本: %1").arg(versions.join(QStringLiteral(", "))));
    }
}

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

void TcpClient::resetConnection()
{
    m_connectTimer.stop();
    m_responseTimer.stop();
    m_queue.clear();
    m_pendingEnvelope.clear();
    m_pendingCallback = nullptr;
    m_waitingResponse = false;
    m_readBuffer.clear();
    m_tlsReady = false;
    if (m_socket.state() != QAbstractSocket::UnconnectedState) {
        m_socket.abort();
    }
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
        QSslConfiguration conf = QSslConfiguration::defaultConfiguration();
        conf.setProtocol(QSsl::TlsV1_2OrLater);
        conf.setPeerVerifyMode(QSslSocket::VerifyNone);
        m_socket.setSslConfiguration(conf);
        m_socket.connectToHostEncrypted(m_host, m_port, QString());
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
    if (!m_useTls) {
        return;
    }
    for (const QSslError &e : errors) {
        AppLogger::instance()->warn(QStringLiteral("[云端] TLS 告警: %1").arg(e.errorString()));
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
        failPending(humanizeConnectError(m_socket.errorString(), m_useTls));
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
