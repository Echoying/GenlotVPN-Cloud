#include "SecureStorage.h"
#include <QVariantMap>

namespace vpn {

SecureStorage::SecureStorage(QObject *parent)
    : QObject(parent)
    , m_settings(QStringLiteral("Genlot"), QStringLiteral("GenlotVPN"))
{
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
