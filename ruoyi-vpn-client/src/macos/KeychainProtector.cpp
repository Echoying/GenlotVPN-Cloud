#include "KeychainProtector.h"

#ifdef Q_OS_MACOS
#include "../core/AppLogger.h"
#include <QString>
#include <Security/Security.h>
#include <CoreFoundation/CoreFoundation.h>
#endif

namespace vpn {

namespace {

#ifdef Q_OS_MACOS

constexpr char kToken[] = "keychain:v1";

/**
 * 定位属性只放 class/service/account。
 * 注意：kSecAttrAccessible 只作用于 data protection 钥匙串，
 * 传给 macOS 文件钥匙串在部分系统版本会直接 errSecParam(-50)，
 * 表现就是「某些 macOS 上记不住密码」。
 */
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

/** 只记状态码与系统描述，不记任何密码内容 */
void logStatus(const QString &op, OSStatus status)
{
    QString detail;
    if (CFStringRef msg = SecCopyErrorMessageString(status, nullptr)) {
        detail = QString::fromCFString(msg);
        CFRelease(msg);
    }
    AppLogger::instance()->warn(QStringLiteral("[Keychain] %1 失败: OSStatus=%2 %3")
                                    .arg(op)
                                    .arg(static_cast<int>(status))
                                    .arg(detail));
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

    CFDataRef data = CFDataCreate(kCFAllocatorDefault,
                                  reinterpret_cast<const UInt8 *>(plainText.constData()),
                                  static_cast<CFIndex>(plainText.size()));
    if (!data) {
        return {};
    }

    CFDictionaryRef query = makeQuery(false);
    if (!query) {
        CFRelease(data);
        return {};
    }

    // 先更新既有项：旧项若由其他版本的可执行文件创建，删除可能被 ACL 拒绝，
    // 直接 SecItemAdd 就会拿到 errSecDuplicateItem 而保存失败
    const void *updKeys[] = { kSecValueData };
    const void *updValues[] = { data };
    CFDictionaryRef updAttrs = CFDictionaryCreate(kCFAllocatorDefault, updKeys, updValues, 1,
                                                  &kCFTypeDictionaryKeyCallBacks,
                                                  &kCFTypeDictionaryValueCallBacks);
    OSStatus status = updAttrs ? SecItemUpdate(query, updAttrs) : errSecAllocate;
    if (updAttrs) {
        CFRelease(updAttrs);
    }
    CFRelease(query);

    if (status == errSecItemNotFound) {
        const void *keys[] = {
            kSecClass,
            kSecAttrService,
            kSecAttrAccount,
            kSecValueData,
        };
        const void *values[] = {
            kSecClassGenericPassword,
            CFSTR("com.genlot.GenlotVPN"),
            CFSTR("rememberedPassword"),
            data,
        };
        CFDictionaryRef attrs = CFDictionaryCreate(kCFAllocatorDefault, keys, values, 4,
                                                   &kCFTypeDictionaryKeyCallBacks,
                                                   &kCFTypeDictionaryValueCallBacks);
        status = attrs ? SecItemAdd(attrs, nullptr) : errSecAllocate;
        if (attrs) {
            CFRelease(attrs);
        }
    }
    CFRelease(data);

    if (status != errSecSuccess) {
        logStatus(QStringLiteral("写入记住密码"), status);
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
    CFDictionaryRef query = makeQuery(false);
    if (!query) {
        return;
    }
    const OSStatus status = SecItemDelete(query);
    CFRelease(query);
    if (status != errSecSuccess && status != errSecItemNotFound) {
        logStatus(QStringLiteral("删除记住密码"), status);
    }
#endif
}

QByteArray KeychainProtector::unprotect(const QByteArray &cipherText)
{
#ifdef Q_OS_MACOS
    Q_UNUSED(cipherText); // 非本实现令牌时也照样读 Keychain（容错）

    CFDictionaryRef query = makeQuery(true);
    if (!query) {
        return {};
    }

    CFTypeRef result = nullptr;
    const OSStatus status = SecItemCopyMatching(query, &result);
    CFRelease(query);
    if (status != errSecSuccess || !result) {
        logStatus(QStringLiteral("读取记住密码"), status);
        if (result) {
            CFRelease(result);
        }
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
