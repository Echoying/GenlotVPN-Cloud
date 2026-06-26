#pragma once

#include <QString>

namespace vpn {

/** 校验 Spring BCryptPasswordEncoder 生成的 $2a$ / $2b$ 哈希 */
class BcryptVerifier {
public:
    static bool matches(const QString &plainPassword, const QString &bcryptHash);
};

} // namespace vpn
