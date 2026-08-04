#include "PasswordProtector.h"

#include <QtGlobal>

#ifdef Q_OS_WIN
#include "../windows/DpapiProtector.h"
#elif defined(Q_OS_MACOS)
#include "../macos/KeychainProtector.h"
#endif

namespace vpn {

bool PasswordProtector::isAvailable()
{
#ifdef Q_OS_WIN
    return DpapiProtector::isAvailable();
#elif defined(Q_OS_MACOS)
    return KeychainProtector::isAvailable();
#else
    return false;
#endif
}

QByteArray PasswordProtector::protect(const QByteArray &plainText)
{
#ifdef Q_OS_WIN
    return DpapiProtector::protect(plainText);
#elif defined(Q_OS_MACOS)
    return KeychainProtector::protect(plainText);
#else
    Q_UNUSED(plainText);
    return {};
#endif
}

QByteArray PasswordProtector::unprotect(const QByteArray &cipherText)
{
#ifdef Q_OS_WIN
    return DpapiProtector::unprotect(cipherText);
#elif defined(Q_OS_MACOS)
    return KeychainProtector::unprotect(cipherText);
#else
    Q_UNUSED(cipherText);
    return {};
#endif
}

void PasswordProtector::clear()
{
#ifdef Q_OS_MACOS
    KeychainProtector::clear();
#else
    // DPAPI 密文仅存 QSettings，清除条目即可
#endif
}

} // namespace vpn
