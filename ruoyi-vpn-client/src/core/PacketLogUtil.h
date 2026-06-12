#pragma once

#include <QByteArray>
#include <QString>

namespace vpn {

struct RpcResult;

/** 云端/控制器收发报文格式化并写入 AppLogger */
class PacketLogUtil {
public:
    static void logCloudSend(int messageType, const QByteArray &payload);
    static void logCloudReceive(int messageType, const QByteArray &envelopeBytes, const RpcResult &result);

    static void logControllerSend(const QString &method, const QString &path, const QByteArray &body);
    static void logControllerReceive(const QString &method, const QString &path, const QByteArray &body);
};

} // namespace vpn
