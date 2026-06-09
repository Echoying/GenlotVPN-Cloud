#include "SecureStorage.h"
#include <QCoreApplication>
#include <QFile>
#include <QJsonDocument>
#include <QJsonObject>
#include <QVariantMap>

namespace vpn {

SecureStorage::SecureStorage(QObject *parent)
    : QObject(parent)
    , m_settings(QStringLiteral("Genlot"), QStringLiteral("GenlotVPN"))
{
}

QString SecureStorage::configFilePath() const
{
    return QCoreApplication::applicationDirPath() + QStringLiteral("/config.json");
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
    return result;
}

bool SecureStorage::saveConfigHostPort(const QString &host, int port)
{
    QJsonObject cfg;
    const QVariantMap existing = loadConfigFile();
    cfg[QStringLiteral("serverHost")] = host.trimmed();
    cfg[QStringLiteral("serverPort")] = port;
    cfg[QStringLiteral("useTls")] = existing.value(QStringLiteral("useTls"), false).toBool();
    const QString pin = existing.value(QStringLiteral("certPinSha256")).toString();
    if (!pin.isEmpty()) {
        cfg[QStringLiteral("certPinSha256")] = pin;
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

void SecureStorage::saveRememberedUser(const QString &username, const QString &password, bool remember)
{
    if (!remember) {
        m_settings.remove(QStringLiteral("user/username"));
        m_settings.remove(QStringLiteral("user/password"));
        m_settings.setValue(QStringLiteral("user/remember"), false);
        return;
    }
    m_settings.setValue(QStringLiteral("user/username"), username);
    // 生产环境应使用 DPAPI/Keychain；此处简化存储
    m_settings.setValue(QStringLiteral("user/password"), password);
    m_settings.setValue(QStringLiteral("user/remember"), true);
}

QVariantMap SecureStorage::loadRememberedUser() const
{
    QVariantMap m;
    m[QStringLiteral("username")] = m_settings.value(QStringLiteral("user/username")).toString();
    m[QStringLiteral("password")] = m_settings.value(QStringLiteral("user/password")).toString();
    m[QStringLiteral("remember")] = m_settings.value(QStringLiteral("user/remember"), false).toBool();
    return m;
}

} // namespace vpn