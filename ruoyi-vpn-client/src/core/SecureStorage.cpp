#include "SecureStorage.h"
#include "AppLogger.h"
#include "AppPaths.h"
#include "../platform/PasswordProtector.h"
#include <QFile>
#include <QFileDevice>
#include <QJsonDocument>
#include <QJsonObject>
#include <QJsonArray>
#include <QVariantMap>

namespace vpn {

namespace {

QString defaultConfigTemplatePath()
{
    return AppPaths::configDefaultTemplatePath();
}

QByteArray embeddedDefaultConfigJson()
{
    return QByteArray(
        "{\n"
        "  \"serverHost\": \"202.105.127.12\",\n"
        "  \"serverPort\": 9443,\n"
        "  \"useTls\": true,\n"
        "  \"certPinSha256\": \"cfaed547fc3b72894931ddcd7f94090bb909e95357510d3a79c94345f5e4d6fb\",\n"
        "  \"certPinSha256Backup\": \"\",\n"
        "  \"tcpReconnectMaxRetries\": 3,\n"
        "  \"tcpReconnectDelayMs\": 1500,\n"
        "  \"controllerAesEnabled\": true,\n"
        "  \"locale\": \"zh_CN\",\n"
        "  \"timeCheckEnabled\": true,\n"
        "  \"timeCheckMinSources\": 2,\n"
        "  \"timeCheckTimeoutMs\": 3000,\n"
        "  \"expireAtTimeZone\": \"Asia/Shanghai\",\n"
        "  \"offlineHmacKey\": \"1^z4UGNHULekb*_msnrJZUpIJC.H_*.Npw22NJ9Vtv2_Yqro\"\n"
        "}\n");
}

bool writeBytesToConfig(const QString &path, const QByteArray &bytes)
{
    QFile out(path);
    if (!out.open(QIODevice::WriteOnly | QIODevice::Truncate | QIODevice::Text)) {
        return false;
    }
    out.write(bytes);
    return true;
}

bool isConfigUsable(const QVariantMap &cfg)
{
    return !cfg.value(QStringLiteral("serverHost")).toString().trimmed().isEmpty();
}

} // namespace

SecureStorage::SecureStorage(QObject *parent)
    : QObject(parent)
    , m_settings(QStringLiteral("Genlot"), QStringLiteral("GenlotVPN"))
{
}

QString SecureStorage::configFilePath() const
{
    return AppPaths::configFilePath();
}

bool SecureStorage::ensureDefaultConfigFile()
{
    const QString path = configFilePath();
    if (QFile::exists(path) && isConfigUsable(loadConfigFile())) {
        return false;
    }

    const QString templatePath = defaultConfigTemplatePath();
    if (QFile::exists(templatePath)) {
        if (QFile::exists(path)) {
            QFile::remove(path);
        }
        if (!QFile::copy(templatePath, path)) {
            return false;
        }
        // macOS 从 .app/Resources 复制时可能带上只读权限，导致后续设置页无法保存
        QFile::setPermissions(path, QFileDevice::ReadOwner | QFileDevice::WriteOwner
                                         | QFileDevice::ReadUser | QFileDevice::WriteUser
                                         | QFileDevice::ReadGroup | QFileDevice::ReadOther);
        return true;
    }

    return writeBytesToConfig(path, embeddedDefaultConfigJson());
}

QVariantMap SecureStorage::loadConfigFile() const
{
    QVariantMap result;
    QFile file(configFilePath());
    if (!file.open(QIODevice::ReadOnly)) {
        return result;
    }
    const QJsonObject cfg = QJsonDocument::fromJson(file.readAll()).object();
    result[QStringLiteral("serverHost")] = cfg.value(QStringLiteral("serverHost")).toString();
    result[QStringLiteral("serverPort")] = cfg.value(QStringLiteral("serverPort")).toInt(9443);
    result[QStringLiteral("useTls")] = cfg.value(QStringLiteral("useTls")).toBool(false);
    result[QStringLiteral("certPinSha256")] = cfg.value(QStringLiteral("certPinSha256")).toString();
    result[QStringLiteral("certPinSha256Backup")] = cfg.value(QStringLiteral("certPinSha256Backup")).toString();
    result[QStringLiteral("tcpReconnectMaxRetries")] =
        cfg.contains(QStringLiteral("tcpReconnectMaxRetries"))
            ? cfg.value(QStringLiteral("tcpReconnectMaxRetries")).toInt()
            : kDefaultTcpReconnectMaxRetries;
    result[QStringLiteral("tcpReconnectDelayMs")] =
        cfg.contains(QStringLiteral("tcpReconnectDelayMs"))
            ? cfg.value(QStringLiteral("tcpReconnectDelayMs")).toInt()
            : kDefaultTcpReconnectDelayMs;
    if (cfg.contains(QStringLiteral("controllerAesEnabled"))) {
        result[QStringLiteral("controllerAesEnabled")] = cfg.value(QStringLiteral("controllerAesEnabled")).toBool();
    } else {
        result[QStringLiteral("controllerAesEnabled")] = true;
    }
    result[QStringLiteral("locale")] = cfg.value(QStringLiteral("locale")).toString(QStringLiteral("zh_CN"));
    result[QStringLiteral("timeCheckEnabled")] =
        cfg.contains(QStringLiteral("timeCheckEnabled"))
            ? cfg.value(QStringLiteral("timeCheckEnabled")).toBool()
            : true;
    result[QStringLiteral("timeCheckMinSources")] =
        cfg.contains(QStringLiteral("timeCheckMinSources"))
            ? cfg.value(QStringLiteral("timeCheckMinSources")).toInt(2)
            : 2;
    result[QStringLiteral("timeCheckTimeoutMs")] =
        cfg.contains(QStringLiteral("timeCheckTimeoutMs"))
            ? cfg.value(QStringLiteral("timeCheckTimeoutMs")).toInt(3000)
            : 3000;
    result[QStringLiteral("expireAtTimeZone")] =
        cfg.value(QStringLiteral("expireAtTimeZone")).toString(QStringLiteral("Asia/Shanghai"));
    result[QStringLiteral("offlineHmacKey")] = cfg.value(QStringLiteral("offlineHmacKey")).toString();
    if (cfg.contains(QStringLiteral("timeCheckUrls"))) {
        const QJsonValue urlsValue = cfg.value(QStringLiteral("timeCheckUrls"));
        if (urlsValue.isArray()) {
            QStringList urls;
            for (const QJsonValue &item : urlsValue.toArray()) {
                const QString url = item.toString().trimmed();
                if (!url.isEmpty()) {
                    urls.append(url);
                }
            }
            result[QStringLiteral("timeCheckUrls")] = urls;
        }
    }
    return result;
}

bool SecureStorage::readConfigObject(QJsonObject *out) const
{
    if (!out) {
        return false;
    }
    *out = QJsonObject();
    QFile file(configFilePath());
    if (!file.open(QIODevice::ReadOnly)) {
        return false;
    }
    *out = QJsonDocument::fromJson(file.readAll()).object();
    return true;
}

bool SecureStorage::writeConfigObject(const QJsonObject &cfg)
{
    QFile file(configFilePath());
    if (!file.open(QIODevice::WriteOnly | QIODevice::Truncate | QIODevice::Text)) {
        return false;
    }
    file.write(QJsonDocument(cfg).toJson(QJsonDocument::Indented));
    return true;
}

bool SecureStorage::saveConfigServer(const QString &host, int port, bool useTls,
                                     const QString &certPinSha256,
                                     const QString &certPinSha256Backup)
{
    QJsonObject cfg;
    readConfigObject(&cfg);
    cfg[QStringLiteral("serverHost")] = host.trimmed();
    cfg[QStringLiteral("serverPort")] = port;
    cfg[QStringLiteral("useTls")] = useTls;

    const QString pin = certPinSha256.trimmed();
    if (!pin.isEmpty()) {
        cfg[QStringLiteral("certPinSha256")] = pin;
    } else {
        cfg.remove(QStringLiteral("certPinSha256"));
    }
    const QString backupPin = certPinSha256Backup.trimmed();
    if (!backupPin.isEmpty()) {
        cfg[QStringLiteral("certPinSha256Backup")] = backupPin;
    } else {
        cfg.remove(QStringLiteral("certPinSha256Backup"));
    }

    return writeConfigObject(cfg);
}

bool SecureStorage::saveConfigReconnect(int maxRetries, int delayMs)
{
    QJsonObject cfg;
    readConfigObject(&cfg);
    cfg[QStringLiteral("tcpReconnectMaxRetries")] = maxRetries;
    cfg[QStringLiteral("tcpReconnectDelayMs")] = delayMs;
    return writeConfigObject(cfg);
}

bool SecureStorage::saveConfigLocale(const QString &locale)
{
    QJsonObject cfg;
    readConfigObject(&cfg);
    cfg[QStringLiteral("locale")] = locale.trimmed();
    return writeConfigObject(cfg);
}

void SecureStorage::saveServer(const QString &host, quint16 port, bool useTls)
{
    m_settings.setValue(QStringLiteral("server/host"), host);
    m_settings.setValue(QStringLiteral("server/port"), port);
    m_settings.setValue(QStringLiteral("server/useTls"), useTls);
}

QVariantMap SecureStorage::loadServer() const
{
    QVariantMap m;
    m[QStringLiteral("host")] = m_settings.value(QStringLiteral("server/host"), QStringLiteral("127.0.0.1")).toString();
    m[QStringLiteral("port")] = m_settings.value(QStringLiteral("server/port"), 9443).toUInt();
    m[QStringLiteral("useTls")] = m_settings.value(QStringLiteral("server/useTls"), false).toBool();
    return m;
}

void SecureStorage::clearRememberedUser()
{
    PasswordProtector::clear();
    m_settings.remove(QStringLiteral("user/username"));
    m_settings.remove(QStringLiteral("user/password"));
    m_settings.remove(QStringLiteral("user/passwordProtected"));
    m_settings.setValue(QStringLiteral("user/remember"), false);
}

QString SecureStorage::loadRememberedPassword() const
{
    const QByteArray protectedB64 = m_settings.value(QStringLiteral("user/passwordProtected")).toByteArray();
    if (!protectedB64.isEmpty()) {
        const QByteArray plain = PasswordProtector::unprotect(QByteArray::fromBase64(protectedB64));
        if (!plain.isEmpty()) {
            return QString::fromUtf8(plain);
        }
        AppLogger::instance()->warn(QStringLiteral("[存储] 安全存储解密记住密码失败，可能已换用户登录"));
        return {};
    }

    // 兼容旧版明文，下次保存时会迁移为平台安全存储
    return m_settings.value(QStringLiteral("user/password")).toString();
}

void SecureStorage::saveRememberedUser(const QString &username, const QString &password, bool remember)
{
    if (!remember) {
        clearRememberedUser();
        return;
    }

    m_settings.setValue(QStringLiteral("user/username"), username.trimmed());
    m_settings.setValue(QStringLiteral("user/remember"), true);
    m_settings.remove(QStringLiteral("user/password"));

    if (!PasswordProtector::isAvailable()) {
        AppLogger::instance()->error(QStringLiteral("[存储] 当前平台无安全存储，无法记住密码"));
        m_settings.remove(QStringLiteral("user/passwordProtected"));
        m_settings.setValue(QStringLiteral("user/remember"), false);
        return;
    }

    const QByteArray protectedBytes = PasswordProtector::protect(password.toUtf8());
    if (protectedBytes.isEmpty()) {
        AppLogger::instance()->error(QStringLiteral("[存储] 安全存储加密记住密码失败"));
        m_settings.remove(QStringLiteral("user/passwordProtected"));
        m_settings.setValue(QStringLiteral("user/remember"), false);
        return;
    }

    m_settings.setValue(QStringLiteral("user/passwordProtected"),
                       QString::fromLatin1(protectedBytes.toBase64()));
}

QVariantMap SecureStorage::loadRememberedUser() const
{
    QVariantMap m;
    const bool remember = m_settings.value(QStringLiteral("user/remember"), false).toBool();
    m[QStringLiteral("remember")] = remember;
    if (!remember) {
        m[QStringLiteral("username")] = QString();
        m[QStringLiteral("password")] = QString();
        return m;
    }
    m[QStringLiteral("username")] = m_settings.value(QStringLiteral("user/username")).toString();
    m[QStringLiteral("password")] = loadRememberedPassword();
    return m;
}

} // namespace vpn
