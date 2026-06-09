#pragma once

#include <QByteArray>

namespace vpn {

/** Windows DPAPI（CryptProtectData / CryptUnprotectData），按当前用户加密 */
class DpapiProtector {
public:
    static bool isAvailable();
    static QByteArray protect(const QByteArray &plainText);
    static QByteArray unprotect(const QByteArray &cipherText);
};

} // namespace vpn
