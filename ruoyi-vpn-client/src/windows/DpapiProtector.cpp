#include "DpapiProtector.h"

#ifdef Q_OS_WIN
#include <windows.h>
#include <dpapi.h>
#endif

namespace vpn {

bool DpapiProtector::isAvailable()
{
#ifdef Q_OS_WIN
    return true;
#else
    return false;
#endif
}

QByteArray DpapiProtector::protect(const QByteArray &plainText)
{
    if (plainText.isEmpty()) {
        return {};
    }
#ifdef Q_OS_WIN
    DATA_BLOB input{};
    input.pbData = reinterpret_cast<BYTE *>(const_cast<char *>(plainText.data()));
    input.cbData = static_cast<DWORD>(plainText.size());

    DATA_BLOB output{};
    // 默认 CurrentUser 作用域，其他 Windows 用户无法解密
    if (!CryptProtectData(&input, L"GenlotVPN", nullptr, nullptr, nullptr, 0, &output)) {
        return {};
    }

    QByteArray result(reinterpret_cast<const char *>(output.pbData), static_cast<int>(output.cbData));
    LocalFree(output.pbData);
    return result;
#else
    Q_UNUSED(plainText)
    return {};
#endif
}

QByteArray DpapiProtector::unprotect(const QByteArray &cipherText)
{
    if (cipherText.isEmpty()) {
        return {};
    }
#ifdef Q_OS_WIN
    DATA_BLOB input{};
    input.pbData = reinterpret_cast<BYTE *>(const_cast<char *>(cipherText.data()));
    input.cbData = static_cast<DWORD>(cipherText.size());

    DATA_BLOB output{};
    if (!CryptUnprotectData(&input, nullptr, nullptr, nullptr, nullptr, 0, &output)) {
        return {};
    }

    QByteArray result(reinterpret_cast<const char *>(output.pbData), static_cast<int>(output.cbData));
    LocalFree(output.pbData);
    return result;
#else
    Q_UNUSED(cipherText)
    return {};
#endif
}

} // namespace vpn
