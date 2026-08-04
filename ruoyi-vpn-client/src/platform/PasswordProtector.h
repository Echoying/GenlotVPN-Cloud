#pragma once

#include <QByteArray>

namespace vpn {

/** 记住密码加密：Windows=DPAPI，macOS=Keychain */
class PasswordProtector {
public:
    static bool isAvailable();
    static QByteArray protect(const QByteArray &plainText);
    static QByteArray unprotect(const QByteArray &cipherText);
    static void clear();
};

} // namespace vpn
