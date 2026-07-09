#pragma once

#include <QByteArray>
#include <QString>

namespace vpnproxy {

/** 易安联 Agent 本地 SDK（30303）AES 加解密，与 Java AesUtils / VPN 客户端参数一致。
 *  key/IV 为厂商协议固定常量，非业务密钥；客户端必须持有以便与 Agent 互通。 */
class AgentAesCrypto {
public:
    static QString encryptToBase64(const QByteArray &plainUtf8);
    static QByteArray decryptFromBase64(const QByteArray &cipherBase64);
};

} // namespace vpnproxy
