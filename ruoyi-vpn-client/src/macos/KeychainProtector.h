#pragma once

#include <QByteArray>

namespace vpn {

/** macOS Keychain（Security.framework），按当前用户保存记住密码 */
class KeychainProtector {
public:
    static bool isAvailable();
    /** 将明文写入 Keychain，返回可存入 QSettings 的令牌 */
    static QByteArray protect(const QByteArray &plainText);
    /** 根据令牌从 Keychain 读出明文 */
    static QByteArray unprotect(const QByteArray &cipherText);
    /** 删除 Keychain 中的记住密码项 */
    static void clear();
};

} // namespace vpn
