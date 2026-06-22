#pragma once

#include <QByteArray>
#include <QString>

namespace vpn {

/** 易安联 Agent 本地 SDK（30303）AES 加解密，与 Java AesUtils 参数一致 */
class AgentAesCrypto {
public:
    static QString encryptToBase64(const QByteArray &plainUtf8);
    static QByteArray decryptFromBase64(const QByteArray &cipherBase64);
};

} // namespace vpn
