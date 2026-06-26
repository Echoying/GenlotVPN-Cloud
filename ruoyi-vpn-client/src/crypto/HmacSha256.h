#pragma once

#include <QByteArray>

namespace vpn {

/** HMAC-SHA256，与 Java Mac(HmacSHA256) 结果一致 */
class HmacSha256 {
public:
    static QByteArray sign(const QByteArray &key, const QByteArray &message);
};

} // namespace vpn
