#pragma once

#include "CertificatePinner.h"
#include "FrameCodec.h"
#include <QObject>
#include <QByteArray>
#include <QList>
#include <QSslSocket>
#include <QSslError>
#include <QTimer>
#include <QQueue>
#include <functional>

namespace vpn {

/**
 * TLS/TCP 客户端，发送 Envelope 帧并等待响应
 */
class TcpClient : public QObject {
    Q_OBJECT
public:
    explicit TcpClient(QObject *parent = nullptr);

    void configure(const QString &host, quint16 port, bool useTls,
                   const QString &certPinSha256, const QString &certPinSha256Backup = QString());

    using ResponseCallback = std::function<void(bool ok, const QByteArray &rpcResponseBytes, const QString &error)>;

    void sendEnvelope(const QByteArray &envelopeBytes, ResponseCallback callback);

    bool isConnected() const;

signals:
    void connectionError(const QString &message);

private slots:
    void onConnected();
    void onEncrypted();
    void onSslErrors(const QList<QSslError> &errors);
    void onReadyRead();
    void onSocketError(QAbstractSocket::SocketError error);
    void onConnectTimeout();
    void onResponseTimeout();
    void onDisconnected();

private:
    struct OutboundRequest {
        QByteArray envelope;
        ResponseCallback callback;
    };

    void startConnect();
    void onTlsReady();
    void writePendingEnvelope();
    void failPending(const QString &err);
    void finishActive(bool ok, const QByteArray &body, const QString &err);
    void dispatchNext();

    QSslSocket m_socket;
    QTimer m_connectTimer;
    QTimer m_responseTimer;
    CertificatePinner m_pinner{QString()};
    QString m_host;
    quint16 m_port = 9443;
    bool m_useTls = false;
    bool m_tlsReady = false;
    QByteArray m_readBuffer;
    QQueue<OutboundRequest> m_queue;
    ResponseCallback m_pendingCallback;
    bool m_waitingResponse = false;
    QByteArray m_pendingEnvelope;
};

} // namespace vpn
