#include "VpnCloudService.h"
#include "AppLogger.h"
#include "TcpHmacUtils.h"
#include <QDateTime>
#include <QTimer>
#include <QUuid>
#include <QVariantMap>

#ifdef VPN_HAS_PROTO
#include "vpn/envelope.pb.h"
#include "vpn/auth.pb.h"
#include "vpn/line.pb.h"
#include "vpn/line_verify.pb.h"
#include "vpn/common.pb.h"
#endif

namespace vpn {

namespace {

bool isRetryableTcpError(const QString &err)
{
    const QString msg = err.trimmed();
    if (msg.isEmpty()) {
        return true;
    }
    if (msg.contains(QStringLiteral("Pinning"))
        || msg.contains(QStringLiteral("指纹"))
        || msg.contains(QStringLiteral("证书校验"))
        || msg.contains(QStringLiteral("未配置"))
        || msg.contains(QStringLiteral("Protobuf"))) {
        return false;
    }
    static const QStringList keys = {
        QStringLiteral("连接已断开"),
        QStringLiteral("连接服务器超时"),
        QStringLiteral("等待服务器响应超时"),
        QStringLiteral("网络连接失败"),
        QStringLiteral("TCP 连接失败"),
        QStringLiteral("Connection refused"),
        QStringLiteral("Connection reset"),
        QStringLiteral("远程主机强迫关闭"),
    };
    for (const QString &key : keys) {
        if (msg.contains(key)) {
            return true;
        }
    }
    return false;
}

void emitCloudError(const QString &message)
{
    const QString err = message.trimmed().isEmpty() ? QStringLiteral("云端请求失败") : message.trimmed();
    AppLogger::instance()->error(QStringLiteral("[云端] %1").arg(err));
}

#ifdef VPN_HAS_PROTO
QByteArray serializeProto(const google::protobuf::Message &message)
{
    const std::string bytes = message.SerializeAsString();
    return QByteArray(bytes.data(), static_cast<int>(bytes.size()));
}

QString extractRpcErrorMessage(const vpn::RpcResponse &rpc)
{
    const QString msg = QString::fromStdString(rpc.msg()).trimmed();
    if (!msg.isEmpty()) {
        return msg;
    }
    if (rpc.code() != 200) {
        return QStringLiteral("请求失败（%1）").arg(rpc.code());
    }
    return QString();
}

QVariantList mapLines(const google::protobuf::RepeatedPtrField<vpn::VpnLine> &lines)
{
    QVariantList list;
    for (const auto &line : lines) {
        QVariantMap m;
        m[QStringLiteral("appId")] = QString::fromStdString(line.app_id());
        m[QStringLiteral("appName")] = QString::fromStdString(line.app_name());
        m[QStringLiteral("host")] = QString::fromStdString(line.host());
        m[QStringLiteral("srvPort")] = QString::fromStdString(line.srv_port());
        m[QStringLiteral("spaPort")] = QString::fromStdString(line.spa_port());
        m[QStringLiteral("spaKey")] = QString::fromStdString(line.spa_key());
        list.append(m);
    }
    return list;
}
#endif

} // namespace

VpnCloudService::VpnCloudService(QObject *parent) : QObject(parent)
{
}

void VpnCloudService::configure(const QString &host, quint16 port, bool useTls, const QString &certPinSha256,
                                const QString &certPinSha256Backup)
{
    m_host = host;
    m_port = port;
    m_useTls = useTls;
    m_tcp.configure(host, port, useTls, certPinSha256, certPinSha256Backup);
}

void VpnCloudService::setReconnectPolicy(int maxRetries, int delayMs)
{
    m_reconnectMaxRetries = qMax(0, maxRetries);
    m_reconnectDelayMs = qMax(100, delayMs);
}

RpcResult VpnCloudService::parseEnvelopeResponse(const QByteArray &envelopeBytes)
{
    RpcResult result;
#ifndef VPN_HAS_PROTO
    result.msg = QStringLiteral("Protobuf 未生成");
    return result;
#else
    vpn::Envelope env;
    if (!env.ParseFromArray(envelopeBytes.constData(), envelopeBytes.size())) {
        result.msg = QStringLiteral("响应解析失败");
        return result;
    }
    vpn::RpcResponse rpc;
    if (!rpc.ParseFromString(env.payload())) {
        result.msg = QStringLiteral("RpcResponse 解析失败");
        return result;
    }
    result.code = rpc.code();
    result.msg = extractRpcErrorMessage(rpc);
    result.data = QByteArray(rpc.data().data(), static_cast<int>(rpc.data().size()));
    result.ok = (rpc.code() == 200);
    return result;
#endif
}

QByteArray VpnCloudService::buildEnvelope(int messageType, const QByteArray &payload)
{
#ifdef VPN_HAS_PROTO
    vpn::Envelope env;
    env.set_request_id(QUuid::createUuid().toString(QUuid::WithoutBraces).toStdString());
    env.set_timestamp_ms(QDateTime::currentMSecsSinceEpoch());
    const QByteArray nonce = QUuid::createUuid().toRfc4122();
    env.set_nonce(nonce.constData(), static_cast<size_t>(nonce.size()));
    env.set_type(static_cast<vpn::MessageType>(messageType));
    env.set_payload(payload.constData(), static_cast<size_t>(payload.size()));
    if (!m_accessToken.isEmpty()) {
        env.set_access_token(m_accessToken.toStdString());
    }
    if (TcpHmacUtils::needsMac(messageType) && !m_sessionKey.isEmpty()) {
        const QByteArray mac = TcpHmacUtils::sign(
            m_sessionKey,
            messageType,
            env.timestamp_ms(),
            QByteArray(env.nonce().data(), static_cast<int>(env.nonce().size())),
            payload);
        env.set_mac(mac.constData(), static_cast<size_t>(mac.size()));
    }
    return serializeProto(env);
#else
    Q_UNUSED(messageType);
    Q_UNUSED(payload);
    return {};
#endif
}

void VpnCloudService::sendRpc(int messageType, const QByteArray &payload, RpcCallback callback)
{
    sendRpcWithRetry(messageType, payload, std::move(callback), 0);
}

void VpnCloudService::sendRpcWithRetry(int messageType, const QByteArray &payload, RpcCallback callback,
                                       int retryCount)
{
#ifndef VPN_HAS_PROTO
    emitCloudError(QStringLiteral("Protobuf 代码未生成，请先编译项目"));
    emit requestFailed(QStringLiteral("Protobuf 代码未生成，请先编译项目"));
    return;
#else
    const QByteArray envelope = buildEnvelope(messageType, payload);
    m_tcp.sendEnvelope(envelope, [this, messageType, payload, callback = std::move(callback), retryCount](
                                   bool ok, const QByteArray &body, const QString &err) mutable {
        if (!ok) {
            const QString msg = err.isEmpty() ? QStringLiteral("TCP 连接失败") : err.trimmed();
            if (isRetryableTcpError(msg) && retryCount < m_reconnectMaxRetries) {
                AppLogger::instance()->warn(
                    QStringLiteral("[云端] 传输失败，%1ms 后重试 (%2/%3): %4")
                        .arg(m_reconnectDelayMs)
                        .arg(retryCount + 1)
                        .arg(m_reconnectMaxRetries)
                        .arg(msg));
                m_tcp.resetConnection();
                QTimer::singleShot(m_reconnectDelayMs, this,
                                   [this, messageType, payload, callback = std::move(callback), retryCount]() mutable {
                                       sendRpcWithRetry(messageType, payload, std::move(callback), retryCount + 1);
                                   });
                return;
            }
            emitCloudError(msg);
            emit requestFailed(msg);
            return;
        }
        if (retryCount > 0) {
            AppLogger::instance()->info(
                QStringLiteral("[云端] 重连成功（经 %1 次重试后恢复）").arg(retryCount));
        }
        const RpcResult result = parseEnvelopeResponse(body);
        if (!result.ok) {
            const QString msg = result.msg.isEmpty() ? QStringLiteral("请求失败") : result.msg.trimmed();
            emitCloudError(msg);
            emit requestFailed(msg);
            return;
        }
        callback(result);
    });
#endif
}

void VpnCloudService::fetchPublicLines()
{
#ifndef VPN_HAS_PROTO
    emitCloudError(QStringLiteral("Protobuf 未生成，请重新编译客户端"));
    emit requestFailed(QStringLiteral("Protobuf 未生成，请重新编译客户端"));
    return;
#else
    vpn::ListPublicLinesRequest req;
    sendRpc(static_cast<int>(vpn::MessageType::LIST_PUBLIC_LINES), serializeProto(req), [this](const RpcResult &r) {
        if (!r.ok) return;
        vpn::ListPublicLinesResponse data;
        data.ParseFromArray(r.data.constData(), r.data.size());
        emit linesReady(mapLines(data.lines()));
    });
#endif
}

void VpnCloudService::fetchCaptcha()
{
#ifdef VPN_HAS_PROTO
    vpn::GetCaptchaRequest req;
    sendRpc(static_cast<int>(vpn::MessageType::GET_CAPTCHA), serializeProto(req), [this](const RpcResult &r) {
        if (!r.ok) return;
        vpn::GetCaptchaResponse data;
        data.ParseFromArray(r.data.constData(), r.data.size());
        emit captchaReady(data.captcha_enabled(), QString::fromStdString(data.uuid()),
                          QString::fromStdString(data.img_base64()));
    });
#endif
}

void VpnCloudService::login(const QString &username, const QString &password,
                            const QString &appId, const QString &code, const QString &uuid)
{
#ifdef VPN_HAS_PROTO
    vpn::LoginRequest req;
    req.set_username(username.toStdString());
    req.set_password(password.toStdString());
    req.set_app_id(appId.toStdString());
    req.set_code(code.toStdString());
    req.set_uuid(uuid.toStdString());
    sendRpc(static_cast<int>(vpn::MessageType::LOGIN), serializeProto(req),
            [this](const RpcResult &r) {
        if (!r.ok) return;
        vpn::LoginResponse data;
        data.ParseFromArray(r.data.constData(), r.data.size());
        m_accessToken = QString::fromStdString(data.access_token());
        m_sessionKey = QByteArray(data.session_key().data(), static_cast<int>(data.session_key().size()));
        emit loginSucceeded(m_accessToken);
    });
#endif
}

void VpnCloudService::fetchAuthorizedLines()
{
#ifdef VPN_HAS_PROTO
    vpn::GetAuthorizedLinesRequest req;
    sendRpc(static_cast<int>(vpn::MessageType::GET_AUTHORIZED_LINES), serializeProto(req),
            [this](const RpcResult &r) {
        if (!r.ok) return;
        vpn::GetAuthorizedLinesResponse data;
        data.ParseFromArray(r.data.constData(), r.data.size());
        emit authorizedLinesReady(mapLines(data.lines()));
    });
#endif
}

void VpnCloudService::sendLineVerify(const QString &appId, const QString &lineName)
{
#ifdef VPN_HAS_PROTO
    vpn::SendLineVerifyRequest req;
    req.set_app_id(appId.toStdString());
    req.set_line_name(lineName.toStdString());
    sendRpc(static_cast<int>(vpn::MessageType::SEND_LINE_VERIFY), serializeProto(req),
            [this](const RpcResult &r) {
        if (!r.ok) return;
        vpn::SendLineVerifyResponse data;
        data.ParseFromArray(r.data.constData(), r.data.size());
        emit lineVerifySent(QString::fromStdString(data.expire_at()));
    });
#endif
}

void VpnCloudService::confirmLineVerify(const QString &appId, const QString &code)
{
#ifdef VPN_HAS_PROTO
    vpn::ConfirmLineVerifyRequest req;
    req.set_app_id(appId.toStdString());
    req.set_code(code.toStdString());
    sendRpc(static_cast<int>(vpn::MessageType::CONFIRM_LINE_VERIFY), serializeProto(req),
            [this](const RpcResult &r) {
        if (!r.ok) return;
        emit lineVerifyConfirmed();
    });
#endif
}

void VpnCloudService::fetchUserCredentials(const QString &appId)
{
#ifdef VPN_HAS_PROTO
    vpn::GetUserCredentialsRequest req;
    req.set_app_id(appId.toStdString());
    sendRpc(static_cast<int>(vpn::MessageType::GET_USER_CREDENTIALS), serializeProto(req),
            [this](const RpcResult &r) {
        if (!r.ok) return;
        vpn::GetUserCredentialsResponse data;
        data.ParseFromArray(r.data.constData(), r.data.size());
        emit userCredentialsReady(QString::fromStdString(data.username()),
                                  QString::fromStdString(data.password()));
    });
#endif
}

void VpnCloudService::changePassword(const QString &username, const QString &oldPassword,
                                     const QString &newPassword, const QString &appId)
{
#ifdef VPN_HAS_PROTO
    vpn::ChangePasswordRequest req;
    req.set_username(username.toStdString());
    req.set_old_password(oldPassword.toStdString());
    req.set_new_password(newPassword.toStdString());
    req.set_app_id(appId.toStdString());
    sendRpc(static_cast<int>(vpn::MessageType::CHANGE_PASSWORD), serializeProto(req),
            [this](const RpcResult &r) {
        if (!r.ok) return;
        emit changePasswordSucceeded();
    });
#endif
}

void VpnCloudService::clearSession()
{
    m_accessToken.clear();
    m_sessionKey.clear();
    m_tcp.resetConnection();
}

void VpnCloudService::logout()
{
#ifdef VPN_HAS_PROTO
    if (!hasSession()) {
        clearSession();
        emit logoutSucceeded();
        return;
    }
    vpn::LogoutRequest req;
    sendRpc(static_cast<int>(vpn::MessageType::LOGOUT), serializeProto(req),
            [this](const RpcResult &r) {
        clearSession();
        if (!r.ok) {
            const QString msg = r.msg.isEmpty() ? QStringLiteral("退出登录请求失败") : r.msg.trimmed();
            emitCloudError(msg);
        }
        emit logoutSucceeded();
    });
#else
    clearSession();
    emit logoutSucceeded();
#endif
}

} // namespace vpn
