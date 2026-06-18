#pragma once

#include <QByteArray>
#include <QString>

namespace vpn {

struct RpcResult;

/** 云端/控制器收发报文格式化并写入 AppLogger */
class PacketLogUtil {
public:
    /** RPC 完成时打印请求与应答字段（含敏感字段脱敏） */
    static void logCloudRpcComplete(int messageType, const QByteArray &requestPayload,
                                    const QByteArray &envelopeBytes, const RpcResult &result);

    static void logControllerSend(const QString &method, const QString &path, const QByteArray &body);
    static void logControllerReceive(const QString &method, const QString &path, const QByteArray &body);
};

} // namespace vpn
