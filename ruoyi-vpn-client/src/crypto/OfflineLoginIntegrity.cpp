#include "OfflineLoginIntegrity.h"

#include "HmacSha256.h"

#include <QByteArray>
#include <QJsonObject>

namespace vpn {

namespace {

QString fieldString(const QJsonObject &obj, const char *snakeKey, const char *camelKey = nullptr)
{
    if (obj.contains(snakeKey)) {
        return obj.value(snakeKey).toString();
    }
    if (camelKey && obj.contains(camelKey)) {
        return obj.value(camelKey).toString();
    }
    return QString();
}

int fieldInt(const QJsonObject &obj, const char *snakeKey, const char *camelKey)
{
    if (obj.contains(snakeKey)) {
        return obj.value(snakeKey).toInt();
    }
    if (camelKey && obj.contains(camelKey)) {
        return obj.value(camelKey).toInt();
    }
    return 0;
}

void appendLine(QString &out, const QString &key, const QString &value)
{
    out += key;
    out += QLatin1Char('=');
    out += value;
    out += QLatin1Char('\n');
}

void appendLine(QString &out, const QString &key, int value)
{
    appendLine(out, key, QString::number(value));
}

bool secureEquals(const QByteArray &a, const QByteArray &b)
{
    if (a.size() != b.size()) {
        return false;
    }
    char diff = 0;
    for (int i = 0; i < a.size(); ++i) {
        diff |= static_cast<char>(a[i] ^ b[i]);
    }
    return diff == 0;
}

} // namespace

QString OfflineLoginIntegrity::defaultHmacKey()
{
    return QStringLiteral("1^z4UGNHULekb*_msnrJZUpIJC.H_*.Npw22NJ9Vtv2_Yqro");
}

QString OfflineLoginIntegrity::buildCanonical(const QJsonObject &root)
{
    QString canonical;
    canonical.reserve(512);
    appendLine(canonical, QStringLiteral("app_id"),
               fieldString(root, "app_id", "appId"));
    appendLine(canonical, QStringLiteral("app_name"),
               fieldString(root, "app_name", "appName"));
    appendLine(canonical, QStringLiteral("expire_at"),
               fieldString(root, "expire_at", "expireAt"));
    appendLine(canonical, QStringLiteral("host"), fieldString(root, "host"));
    appendLine(canonical, QStringLiteral("local_password_hash"),
               fieldString(root, "local_password_hash", "localPasswordHash"));
    appendLine(canonical, QStringLiteral("local_user_name"),
               fieldString(root, "local_user_name", "localUserName"));
    appendLine(canonical, QStringLiteral("password"), fieldString(root, "password"));
    appendLine(canonical, QStringLiteral("spa_key"),
               fieldString(root, "spa_key", "spaKey"));
    appendLine(canonical, QStringLiteral("spa_port"),
               fieldInt(root, "spa_port", "spaPort"));
    appendLine(canonical, QStringLiteral("srv_port"),
               fieldInt(root, "srv_port", "srvPort"));
    appendLine(canonical, QStringLiteral("user_name"),
               fieldString(root, "user_name", "userName"));
    return canonical;
}

QString OfflineLoginIntegrity::signCanonical(const QString &canonical, const QString &hmacKey)
{
    const QByteArray mac =
        HmacSha256::sign(hmacKey.toUtf8(), canonical.toUtf8());
    return QString::fromLatin1(mac.toBase64());
}

bool OfflineLoginIntegrity::verifyPayload(const QJsonObject &root, const QString &hmacKey)
{
    const QString payloadHmac =
        fieldString(root, "payload_hmac", "payloadHmac");
    if (payloadHmac.isEmpty() || hmacKey.isEmpty()) {
        return false;
    }
    const QString canonical = buildCanonical(root);
    const QByteArray expected = QByteArray::fromBase64(signCanonical(canonical, hmacKey).toLatin1());
    const QByteArray actual = QByteArray::fromBase64(payloadHmac.toLatin1());
    return !expected.isEmpty() && secureEquals(expected, actual);
}

} // namespace vpn
