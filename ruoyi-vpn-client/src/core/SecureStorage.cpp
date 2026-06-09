#include "SecureStorage.h"
#include "AppLogger.h"
#include "../windows/DpapiProtector.h"
#include <QCoreApplication>
#include <QFile>
#include <QJsonDocument>
#include <QJsonObject>
#include <QVariantMap>

namespace vpn {

namespace {

QString defaultConfigTemplatePath()
{
    return QCoreApplication::applicationDirPath() + QStringLiteral("/config.default.json");
}

QByteArray embeddedDefaultConfigJson()
{
    return QByteArray(
        "{\n"
        "  \"serverHost\": \"10.9.2.177\",\n"
        "  \"serverPort\": 9443,\n"
        "  \"useTls\": true,\n"
        "  \"certPinSha256\": \"cfaed547fc3b72894931ddcd7f94090bb909e95357510d3a79c94345f5e4d6fb\",\n"
        "  \"certPinSha256Backup\": \"\"\n"
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
    return QCoreApplication::applicationDirPath() + QStringLiteral("/config.json");
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
        return QFile::copy(templatePath, path);
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
    return result;
}

bool SecureStorage::saveConfigServer(const QString &host, int port, bool useTls,
                                     const QString &certPinSha256,
                                     const QString &certPinSha256Backup)
{
    QJsonObject cfg;
    cfg[QStringLiteral("serverHost")] = host.trimmed();
    cfg[QStringLiteral("serverPort")] = port;
    cfg[QStringLiteral("useTls")] = useTls;

    const QString pin = certPinSha256.trimmed();
    if (!pin.isEmpty()) {
        cfg[QStringLiteral("certPinSha256")] = pin;
    }
    const QString backupPin = certPinSha256Backup.trimmed();
    if (!backupPin.isEmpty()) {
        cfg[QStringLiteral("certPinSha256Backup")] = backupPin;
    }

    QFile file(configFilePath());
    if (!file.open(QIODevice::WriteOnly | QIODevice::Truncate | QIODevice::Text)) {
        return false;
    }
    file.write(QJsonDocument(cfg).toJson(QJsonDocument::Indented));
    return true;
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
    m_settings.remove(QStringLiteral("user/username"));
    m_settings.remove(QStringLiteral("user/password"));
    m_settings.remove(QStringLiteral("user/passwordProtected"));
    m_settings.setValue(QStringLiteral("user/remember"), false);
}

QString SecureStorage::loadRememberedPassword() const
{
    const QByteArray protectedB64 = m_settings.value(QStringLiteral("user/passwordProtected")).toByteArray();
    if (!protectedB64.isEmpty()) {
        const QByteArray plain = DpapiProtector::unprotect(QByteArray::fromBase64(protectedB64));
        if (!plain.isEmpty()) {
            return QString::fromUtf8(plain);
        }
        AppLogger::instance()->warn(QStringLiteral("[存储] DPAPI 解密记住密码失败，可能已换用户登录"));
        return {};
    }

    // 兼容旧版明文，下次保存时会迁移为 DPAPI
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

    const QByteArray protectedBytes = DpapiProtector::protect(password.toUtf8());
    if (protectedBytes.isEmpty()) {
        AppLogger::instance()->error(QStringLiteral("[存储] DPAPI 加密记住密码失败"));
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
