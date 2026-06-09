#pragma once

#include <QByteArray>
#include <cstdint>

namespace vpn {

/** 与 Java TcpHmacUtils 一致的 HMAC-SHA256 */
class TcpHmacUtils {
public:
    static QByteArray sign(const QByteArray &sessionKey, int typeValue, qint64 timestampMs,
                           const QByteArray &nonce, const QByteArray &payload);

    static bool needsMac(int messageType);
};

} // namespace vpn
