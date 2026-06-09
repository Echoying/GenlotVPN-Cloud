#pragma once

#include "../transport/TcpClient.h"
#include <QObject>
#include <QString>
#include <QByteArray>
#include <QVariantList>
#include <functional>

namespace vpn {

struct RpcResult {
    bool ok = false;
    int code = 500;
    QString msg;
    QByteArray data;
};

/** 封装云端 Protobuf RPC（TCP） */
class VpnCloudService : public QObject {
    Q_OBJECT
public:
    explicit VpnCloudService(QObject *parent = nullptr);

    void configure(const QString &host, quint16 port, bool useTls, const QString &certPinSha256,
                   const QString &certPinSha256Backup = QString());

    Q_INVOKABLE void fetchPublicLines();
    Q_INVOKABLE void fetchCaptcha();
    Q_INVOKABLE void login(const QString &username, const QString &password,
                           const QString &appId, const QString &code, const QString &uuid);
    Q_INVOKABLE void fetchAuthorizedLines();
    Q_INVOKABLE void sendLineVerify(const QString &appId, const QString &lineName);
    Q_INVOKABLE void confirmLineVerify(const QString &appId, const QString &code);
    Q_INVOKABLE void fetchUserCredentials(const QString &appId);
    Q_INVOKABLE void changePassword(const QString &username, const QString &oldPassword,
                                    const QString &newPassword, const QString &appId);
    Q_INVOKABLE void logout();
    void clearSession();

    QString accessToken() const { return m_accessToken; }
    bool hasSession() const { return !m_accessToken.isEmpty(); }

signals:
    void linesReady(const QVariantList &lines);
    void captchaReady(bool enabled, const QString &uuid, const QString &imgBase64);
    void loginSucceeded(const QString &accessToken);
    void authorizedLinesReady(const QVariantList &lines);
    void lineVerifySent(const QString &expireAt);
    void lineVerifyConfirmed();
    void userCredentialsReady(const QString &username, const QString &encryptedPassword);
    void changePasswordSucceeded();
    void logoutSucceeded();
    void requestFailed(const QString &message);

private:
    using RpcCallback = std::function<void(const RpcResult &)>;

    void sendRpc(int messageType, const QByteArray &payload, RpcCallback callback);
    QByteArray buildEnvelope(int messageType, const QByteArray &payload);
    RpcResult parseEnvelopeResponse(const QByteArray &envelopeBytes);

    TcpClient m_tcp;
    QByteArray m_sessionKey;
    QString m_accessToken;
    QString m_host;
    quint16 m_port = 9443;
    bool m_useTls = false;
};

} // namespace vpn
