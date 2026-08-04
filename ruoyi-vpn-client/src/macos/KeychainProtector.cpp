#include "KeychainProtector.h"

#ifdef Q_OS_MACOS
#include <Security/Security.h>
#include <CoreFoundation/CoreFoundation.h>
#endif

namespace vpn {

namespace {

#ifdef Q_OS_MACOS

constexpr char kToken[] = "keychain:v1";

CFDictionaryRef makeQuery(bool includeData)
{
    const void *keys[5];
    const void *values[5];
    CFIndex count = 0;

    keys[count] = kSecClass;
    values[count] = kSecClassGenericPassword;
    ++count;

    keys[count] = kSecAttrService;
    values[count] = CFSTR("com.genlot.GenlotVPN");
    ++count;

    keys[count] = kSecAttrAccount;
    values[count] = CFSTR("rememberedPassword");
    ++count;

    if (includeData) {
        keys[count] = kSecReturnData;
        values[count] = kCFBooleanTrue;
        ++count;
        keys[count] = kSecMatchLimit;
        values[count] = kSecMatchLimitOne;
        ++count;
    }

    return CFDictionaryCreate(kCFAllocatorDefault, keys, values, count,
                              &kCFTypeDictionaryKeyCallBacks, &kCFTypeDictionaryValueCallBacks);
}

void deleteExisting()
{
    CFDictionaryRef query = makeQuery(false);
    if (!query) {
        return;
    }
    SecItemDelete(query);
    CFRelease(query);
}

#endif

} // namespace

bool KeychainProtector::isAvailable()
{
#ifdef Q_OS_MACOS
    return true;
#else
    return false;
#endif
}

QByteArray KeychainProtector::protect(const QByteArray &plainText)
{
#ifdef Q_OS_MACOS
    if (plainText.isEmpty()) {
        return {};
    }

    deleteExisting();

    CFDataRef data = CFDataCreate(kCFAllocatorDefault,
                                  reinterpret_cast<const UInt8 *>(plainText.constData()),
                                  static_cast<CFIndex>(plainText.size()));
    if (!data) {
        return {};
    }

    const void *keys[] = {
        kSecClass,
        kSecAttrService,
        kSecAttrAccount,
        kSecValueData,
        kSecAttrAccessible,
    };
    const void *values[] = {
        kSecClassGenericPassword,
        CFSTR("com.genlot.GenlotVPN"),
        CFSTR("rememberedPassword"),
        data,
        kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
    };
    CFDictionaryRef attrs = CFDictionaryCreate(kCFAllocatorDefault, keys, values, 5,
                                               &kCFTypeDictionaryKeyCallBacks,
                                               &kCFTypeDictionaryValueCallBacks);
    CFRelease(data);
    if (!attrs) {
        return {};
    }

    const OSStatus status = SecItemAdd(attrs, nullptr);
    CFRelease(attrs);
    if (status != errSecSuccess) {
        return {};
    }
    return QByteArray(kToken);
#else
    Q_UNUSED(plainText);
    return {};
#endif
}

void KeychainProtector::clear()
{
#ifdef Q_OS_MACOS
    deleteExisting();
#endif
}

QByteArray KeychainProtector::unprotect(const QByteArray &cipherText)
{
#ifdef Q_OS_MACOS
    if (cipherText != QByteArray(kToken) && !cipherText.isEmpty()) {
        // 非本实现令牌时仍尝试读 Keychain（容错）
    }

    CFDictionaryRef query = makeQuery(true);
    if (!query) {
        return {};
    }

    CFTypeRef result = nullptr;
    const OSStatus status = SecItemCopyMatching(query, &result);
    CFRelease(query);
    if (status != errSecSuccess || !result) {
        return {};
    }

    CFDataRef data = static_cast<CFDataRef>(result);
    const QByteArray plain(reinterpret_cast<const char *>(CFDataGetBytePtr(data)),
                           static_cast<int>(CFDataGetLength(data)));
    CFRelease(result);
    return plain;
#else
    Q_UNUSED(cipherText);
    return {};
#endif
}

} // namespace vpn
