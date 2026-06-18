#include "PacketLogUtil.h"
#include "AppLogger.h"
#include "VpnCloudService.h"
#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>
#include <QJsonValue>

#ifdef VPN_HAS_PROTO
#include "vpn/envelope.pb.h"
#include "vpn/auth.pb.h"
#include "vpn/line.pb.h"
#include "vpn/line_verify.pb.h"
#include "vpn/sync_proxy.pb.h"
#endif

namespace vpn {

namespace {

QString indentMultiline(const QString &text)
{
    return QStringLiteral("  ") + text.trimmed().replace(QStringLiteral("\n"), QStringLiteral("\n  "));
}

#ifdef VPN_HAS_PROTO

QString messageTypeName(int messageType)
{
    switch (static_cast<vpn::MessageType>(messageType)) {
    case vpn::MessageType::GET_CAPTCHA:
        return QStringLiteral("GET_CAPTCHA");
    case vpn::MessageType::LIST_PUBLIC_LINES:
        return QStringLiteral("LIST_PUBLIC_LINES");
    case vpn::MessageType::LOGIN:
        return QStringLiteral("LOGIN");
    case vpn::MessageType::CHANGE_PASSWORD:
        return QStringLiteral("CHANGE_PASSWORD");
    case vpn::MessageType::LOGOUT:
        return QStringLiteral("LOGOUT");
    case vpn::MessageType::REFRESH_TOKEN:
        return QStringLiteral("REFRESH_TOKEN");
    case vpn::MessageType::GET_AUTHORIZED_LINES:
        return QStringLiteral("GET_AUTHORIZED_LINES");
    case vpn::MessageType::SEND_LINE_VERIFY:
        return QStringLiteral("SEND_LINE_VERIFY");
    case vpn::MessageType::CONFIRM_LINE_VERIFY:
        return QStringLiteral("CONFIRM_LINE_VERIFY");
    case vpn::MessageType::GET_USER_CREDENTIALS:
        return QStringLiteral("GET_USER_CREDENTIALS");
    case vpn::MessageType::REPORT_CLIENT_LOGIN:
        return QStringLiteral("REPORT_CLIENT_LOGIN");
    case vpn::MessageType::GET_SYNC_PROXY_CONFIG:
        return QStringLiteral("GET_SYNC_PROXY_CONFIG");
    default:
        return QStringLiteral("TYPE_%1").arg(messageType);
    }
}

QString protoToLog(const google::protobuf::Message &message)
{
    return QString::fromStdString(message.ShortDebugString()).trimmed();
}

QString formatCloudRequestPayload(int messageType, const QByteArray &payload)
{
    if (payload.isEmpty()) {
        return QStringLiteral("(empty)");
    }

    switch (static_cast<vpn::MessageType>(messageType)) {
    case vpn::MessageType::GET_CAPTCHA: {
        vpn::GetCaptchaRequest req;
        if (!req.ParseFromArray(payload.constData(), payload.size())) {
            return QStringLiteral("<解析失败>");
        }
        return protoToLog(req);
    }
    case vpn::MessageType::LIST_PUBLIC_LINES: {
        vpn::ListPublicLinesRequest req;
        if (!req.ParseFromArray(payload.constData(), payload.size())) {
            return QStringLiteral("<解析失败>");
        }
        return protoToLog(req);
    }
    case vpn::MessageType::LOGIN: {
        vpn::LoginRequest req;
        if (!req.ParseFromArray(payload.constData(), payload.size())) {
            return QStringLiteral("<解析失败>");
        }
        if (!req.password().empty()) {
            req.set_password("***");
        }
        return protoToLog(req);
    }
    case vpn::MessageType::CHANGE_PASSWORD: {
        vpn::ChangePasswordRequest req;
        if (!req.ParseFromArray(payload.constData(), payload.size())) {
            return QStringLiteral("<解析失败>");
        }
        if (!req.old_password().empty()) {
            req.set_old_password("***");
        }
        if (!req.new_password().empty()) {
            req.set_new_password("***");
        }
        return protoToLog(req);
    }
    case vpn::MessageType::LOGOUT: {
        vpn::LogoutRequest req;
        if (!req.ParseFromArray(payload.constData(), payload.size())) {
            return QStringLiteral("<解析失败>");
        }
        return protoToLog(req);
    }
    case vpn::MessageType::REFRESH_TOKEN: {
        vpn::RefreshTokenRequest req;
        if (!req.ParseFromArray(payload.constData(), payload.size())) {
            return QStringLiteral("<解析失败>");
        }
        return protoToLog(req);
    }
    case vpn::MessageType::GET_AUTHORIZED_LINES: {
        vpn::GetAuthorizedLinesRequest req;
        if (!req.ParseFromArray(payload.constData(), payload.size())) {
            return QStringLiteral("<解析失败>");
        }
        return protoToLog(req);
    }
    case vpn::MessageType::SEND_LINE_VERIFY: {
        vpn::SendLineVerifyRequest req;
        if (!req.ParseFromArray(payload.constData(), payload.size())) {
            return QStringLiteral("<解析失败>");
        }
        return protoToLog(req);
    }
    case vpn::MessageType::CONFIRM_LINE_VERIFY: {
        vpn::ConfirmLineVerifyRequest req;
        if (!req.ParseFromArray(payload.constData(), payload.size())) {
            return QStringLiteral("<解析失败>");
        }
        return protoToLog(req);
    }
    case vpn::MessageType::GET_USER_CREDENTIALS: {
        vpn::GetUserCredentialsRequest req;
        if (!req.ParseFromArray(payload.constData(), payload.size())) {
            return QStringLiteral("<解析失败>");
        }
        return protoToLog(req);
    }
    case vpn::MessageType::REPORT_CLIENT_LOGIN: {
        vpn::ReportClientLoginRequest req;
        if (!req.ParseFromArray(payload.constData(), payload.size())) {
            return QStringLiteral("<解析失败>");
        }
        return protoToLog(req);
    }
    case vpn::MessageType::GET_SYNC_PROXY_CONFIG: {
        vpn::GetSyncProxyConfigRequest req;
        if (!req.ParseFromArray(payload.constData(), payload.size())) {
            return QStringLiteral("<解析失败>");
        }
        return protoToLog(req);
    }
    default:
        return QStringLiteral("<未知类型 payload %1 字节>").arg(payload.size());
    }
}

QString formatCloudResponseData(int messageType, const QByteArray &data)
{
    if (data.isEmpty()) {
        return QStringLiteral("(empty)");
    }

    switch (static_cast<vpn::MessageType>(messageType)) {
    case vpn::MessageType::GET_CAPTCHA: {
        vpn::GetCaptchaResponse resp;
        if (!resp.ParseFromArray(data.constData(), data.size())) {
            return QStringLiteral("<解析失败>");
        }
        if (!resp.img_base64().empty()) {
            resp.set_img_base64(QStringLiteral("<base64 %1 字节>")
                                    .arg(static_cast<int>(resp.img_base64().size()))
                                    .toStdString());
        }
        return protoToLog(resp);
    }
    case vpn::MessageType::LIST_PUBLIC_LINES: {
        vpn::ListPublicLinesResponse resp;
        if (!resp.ParseFromArray(data.constData(), data.size())) {
            return QStringLiteral("<解析失败>");
        }
        return protoToLog(resp);
    }
    case vpn::MessageType::LOGIN: {
        vpn::LoginResponse resp;
        if (!resp.ParseFromArray(data.constData(), data.size())) {
            return QStringLiteral("<解析失败>");
        }
        if (!resp.access_token().empty()) {
            resp.set_access_token("***");
        }
        if (!resp.session_key().empty()) {
            resp.set_session_key("***");
        }
        return protoToLog(resp);
    }
    case vpn::MessageType::CHANGE_PASSWORD: {
        vpn::ChangePasswordResponse resp;
        if (!resp.ParseFromArray(data.constData(), data.size())) {
            return QStringLiteral("<解析失败>");
        }
        return protoToLog(resp);
    }
    case vpn::MessageType::LOGOUT: {
        vpn::LogoutResponse resp;
        if (!resp.ParseFromArray(data.constData(), data.size())) {
            return QStringLiteral("<解析失败>");
        }
        return protoToLog(resp);
    }
    case vpn::MessageType::REFRESH_TOKEN: {
        vpn::RefreshTokenResponse resp;
        if (!resp.ParseFromArray(data.constData(), data.size())) {
            return QStringLiteral("<解析失败>");
        }
        return protoToLog(resp);
    }
    case vpn::MessageType::GET_AUTHORIZED_LINES: {
        vpn::GetAuthorizedLinesResponse resp;
        if (!resp.ParseFromArray(data.constData(), data.size())) {
            return QStringLiteral("<解析失败>");
        }
        return protoToLog(resp);
    }
    case vpn::MessageType::SEND_LINE_VERIFY: {
        vpn::SendLineVerifyResponse resp;
        if (!resp.ParseFromArray(data.constData(), data.size())) {
            return QStringLiteral("<解析失败>");
        }
        return protoToLog(resp);
    }
    case vpn::MessageType::CONFIRM_LINE_VERIFY: {
        vpn::ConfirmLineVerifyResponse resp;
        if (!resp.ParseFromArray(data.constData(), data.size())) {
            return QStringLiteral("<解析失败>");
        }
        return protoToLog(resp);
    }
    case vpn::MessageType::GET_USER_CREDENTIALS: {
        vpn::GetUserCredentialsResponse resp;
        if (!resp.ParseFromArray(data.constData(), data.size())) {
            return QStringLiteral("<解析失败>");
        }
        if (!resp.password().empty()) {
            resp.set_password("***");
        }
        return protoToLog(resp);
    }
    case vpn::MessageType::REPORT_CLIENT_LOGIN: {
        vpn::ReportClientLoginResponse resp;
        if (!resp.ParseFromArray(data.constData(), data.size())) {
            return QStringLiteral("<解析失败>");
        }
        return protoToLog(resp);
    }
    case vpn::MessageType::GET_SYNC_PROXY_CONFIG: {
        vpn::GetSyncProxyConfigResponse resp;
        if (!resp.ParseFromArray(data.constData(), data.size())) {
            return QStringLiteral("<解析失败>");
        }
        return protoToLog(resp);
    }
    default:
        return QStringLiteral("<未知响应 data %1 字节>").arg(data.size());
    }
}

QString formatEnvelopeMeta(const vpn::Envelope &env)
{
    QString token = QString::fromStdString(env.access_token());
    if (!token.isEmpty()) {
        token = QStringLiteral("***");
    }
    return QStringLiteral("request_id=%1 timestamp_ms=%2 type=%3 access_token=%4 mac=%5 payload_bytes=%6")
        .arg(QString::fromStdString(env.request_id()))
        .arg(env.timestamp_ms())
        .arg(messageTypeName(static_cast<int>(env.type())))
        .arg(token.isEmpty() ? QStringLiteral("-") : token)
        .arg(env.mac().empty() ? QStringLiteral("-") : QStringLiteral("***"))
        .arg(static_cast<int>(env.payload().size()));
}

#endif // VPN_HAS_PROTO

bool isSensitiveJsonKey(const QString &key)
{
    static const QStringList keys = {
        QStringLiteral("password"),
        QStringLiteral("old_password"),
        QStringLiteral("new_password"),
        QStringLiteral("spaKey"),
    };
    return keys.contains(key, Qt::CaseInsensitive);
}

QJsonValue redactJsonValue(const QString &key, const QJsonValue &value)
{
    if (isSensitiveJsonKey(key)) {
        if (value.isString() && !value.toString().isEmpty()) {
            return QStringLiteral("***");
        }
        return value;
    }
    if (value.isObject()) {
        QJsonObject obj = value.toObject();
        QJsonObject out;
        for (auto it = obj.begin(); it != obj.end(); ++it) {
            out.insert(it.key(), redactJsonValue(it.key(), it.value()));
        }
        return out;
    }
    if (value.isArray()) {
        QJsonArray arr = value.toArray();
        QJsonArray out;
        for (const QJsonValue &item : arr) {
            if (item.isObject()) {
                QJsonObject obj = item.toObject();
                QJsonObject redacted;
                for (auto it = obj.begin(); it != obj.end(); ++it) {
                    redacted.insert(it.key(), redactJsonValue(it.key(), it.value()));
                }
                out.append(redacted);
            } else {
                out.append(item);
            }
        }
        return out;
    }
    return value;
}

QString formatControllerBody(const QByteArray &body)
{
    if (body.isEmpty()) {
        return QStringLiteral("(empty)");
    }
    QJsonParseError err;
    const QJsonDocument doc = QJsonDocument::fromJson(body, &err);
    if (err.error != QJsonParseError::NoError) {
        return QString::fromUtf8(body);
    }
    if (doc.isObject()) {
        const QJsonObject obj = doc.object();
        QJsonObject out;
        for (auto it = obj.begin(); it != obj.end(); ++it) {
            out.insert(it.key(), redactJsonValue(it.key(), it.value()));
        }
        return QString::fromUtf8(QJsonDocument(out).toJson(QJsonDocument::Compact));
    }
    if (doc.isArray()) {
        QJsonArray arr = doc.array();
        QJsonArray out;
        for (const QJsonValue &item : arr) {
            if (item.isObject()) {
                QJsonObject obj = item.toObject();
                QJsonObject redacted;
                for (auto it = obj.begin(); it != obj.end(); ++it) {
                    redacted.insert(it.key(), redactJsonValue(it.key(), it.value()));
                }
                out.append(redacted);
            } else {
                out.append(item);
            }
        }
        return QString::fromUtf8(QJsonDocument(out).toJson(QJsonDocument::Compact));
    }
    return QString::fromUtf8(body);
}

} // namespace

void PacketLogUtil::logCloudRpcComplete(int messageType, const QByteArray &requestPayload,
                                        const QByteArray &envelopeBytes, const RpcResult &result)
{
#ifdef VPN_HAS_PROTO
    const QString typeName = messageTypeName(messageType);
    const QString requestBody = formatCloudRequestPayload(messageType, requestPayload);
    const QString status = result.ok ? QStringLiteral("OK") : QStringLiteral("FAIL");
    const QString rpcMsg = result.msg.trimmed().isEmpty() ? QStringLiteral("-") : result.msg.trimmed();

    QString responseBody;
    if (envelopeBytes.isEmpty()) {
        responseBody = QStringLiteral("(无应答报文)");
    } else {
        responseBody = formatCloudResponseData(messageType, result.data);
    }

    QString envelopeMeta;
    if (!envelopeBytes.isEmpty()) {
        vpn::Envelope env;
        if (env.ParseFromArray(envelopeBytes.constData(), envelopeBytes.size())) {
            envelopeMeta = formatEnvelopeMeta(env);
        } else {
            envelopeMeta = QStringLiteral("<Envelope 解析失败，%1 字节>").arg(envelopeBytes.size());
        }
    } else {
        envelopeMeta = QStringLiteral("-");
    }

    AppLogger::instance()->info(
        QStringLiteral("[云端][RPC] %1 %2 code=%3 msg=%4\n  request:\n%5\n  response:\n%6\n  envelope: %7")
            .arg(typeName, status)
            .arg(result.code)
            .arg(rpcMsg)
            .arg(indentMultiline(requestBody))
            .arg(indentMultiline(responseBody))
            .arg(envelopeMeta));
#else
    Q_UNUSED(messageType);
    Q_UNUSED(requestPayload);
    Q_UNUSED(envelopeBytes);
    Q_UNUSED(result);
#endif
}

void PacketLogUtil::logControllerSend(const QString &method, const QString &path, const QByteArray &body)
{
    AppLogger::instance()->info(
        QStringLiteral("[控制器][发送] %1 %2\n  body: %3").arg(method, path, formatControllerBody(body)));
}

void PacketLogUtil::logControllerReceive(const QString &method, const QString &path, const QByteArray &body)
{
    AppLogger::instance()->info(
        QStringLiteral("[控制器][接收] %1 %2\n  body: %3").arg(method, path, formatControllerBody(body)));
}

} // namespace vpn
