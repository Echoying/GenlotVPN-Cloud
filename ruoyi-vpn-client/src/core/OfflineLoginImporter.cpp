#include "OfflineLoginImporter.h"

#include "AppLogger.h"
#include <QCoreApplication>
#include <QDir>
#include <QFile>
#include <QJsonDocument>
#include <QJsonObject>
#include <QJsonParseError>
#include <QObject>

namespace vpn {

namespace {

QString readString(const QJsonObject &obj, const char *snakeKey, const char *camelKey = nullptr)
{
    if (obj.contains(snakeKey)) {
        return obj.value(snakeKey).toString().trimmed();
    }
    if (camelKey && obj.contains(camelKey)) {
        return obj.value(camelKey).toString().trimmed();
    }
    return QString();
}

int readPort(const QJsonObject &obj, const char *snakeKey, const char *camelKey)
{
    if (obj.contains(snakeKey)) {
        return obj.value(snakeKey).toInt();
    }
    if (camelKey && obj.contains(camelKey)) {
        return obj.value(camelKey).toInt();
    }
    return 0;
}

} // namespace

QString OfflineLoginImporter::defaultDirectory()
{
    return QCoreApplication::applicationDirPath() + QStringLiteral("/offline-login");
}

bool OfflineLoginImporter::ensureDirectoryExists(QString *errorOut)
{
    const QString dirPath = defaultDirectory();
    QDir dir(dirPath);
    if (dir.exists()) {
        return true;
    }
    if (!QDir().mkpath(dirPath)) {
        if (errorOut) {
            *errorOut = QObject::tr("无法创建离线登录目录");
        }
        return false;
    }
    return true;
}

QVariantList OfflineLoginImporter::scanDirectory(const QString &dir, QString *errorOut)
{
    QVariantList result;
    QDir directory(dir);
    if (!directory.exists()) {
        if (errorOut) {
            *errorOut = QObject::tr("离线登录目录不存在");
        }
        return result;
    }

    const QStringList files = directory.entryList(
        QStringList{QStringLiteral("*.dat")},
        QDir::Files | QDir::Readable,
        QDir::Name);
    for (const QString &fileName : files) {
        const QString filePath = directory.absoluteFilePath(fileName);
        OfflineLoginPayload payload;
        QString parseError;
        if (!parseFromFile(filePath, &payload, &parseError)) {
            AppLogger::instance()->debug(
                QStringLiteral("[离线登录] 跳过文件 %1: %2").arg(fileName, parseError));
            continue;
        }

        QVariantMap line = payload.line;
        line.insert(QStringLiteral("offlineFilePath"), filePath);
        result.append(line);
    }
    return result;
}

bool OfflineLoginImporter::parseFromFile(const QString &filePath, OfflineLoginPayload *out,
                                         QString *errorOut)
{
    auto fail = [errorOut](const QString &msg) {
        if (errorOut) {
            *errorOut = msg;
        }
        return false;
    };

    if (!out) {
        return fail(QStringLiteral("内部错误"));
    }

    QFile file(filePath);
    if (!file.open(QIODevice::ReadOnly)) {
        return fail(QObject::tr("无法打开文件"));
    }

    QJsonParseError parseError;
    const QJsonDocument doc = QJsonDocument::fromJson(file.readAll(), &parseError);
    if (parseError.error != QJsonParseError::NoError || !doc.isObject()) {
        return fail(QObject::tr("离线登录文件格式无效"));
    }

    const QJsonObject root = doc.object();

    const QString appId = readString(root, "app_id", "appId");
    const QString appName = readString(root, "app_name", "appName");
    const QString host = readString(root, "host");
    const int srvPort = readPort(root, "srv_port", "srvPort");
    const int spaPort = readPort(root, "spa_port", "spaPort");
    const QString spaKey = readString(root, "spa_key", "spaKey");
    const QString userName = readString(root, "user_name", "userName");
    const QString password = readString(root, "password");
    const QString expireAtText = readString(root, "expire_at", "expireAt");

    if (appId.isEmpty() || appName.isEmpty() || host.isEmpty()) {
        return fail(QObject::tr("离线登录文件缺少线路信息"));
    }
    if (srvPort <= 0 || spaPort <= 0) {
        return fail(QObject::tr("离线登录文件端口无效"));
    }
    if (spaKey.isEmpty()) {
        return fail(QObject::tr("离线登录文件缺少 spa_key"));
    }
    if (userName.isEmpty() || password.isEmpty()) {
        return fail(QObject::tr("离线登录文件缺少账号或密码"));
    }
    if (expireAtText.isEmpty()) {
        return fail(QObject::tr("离线登录文件缺少有效时间"));
    }

    QDateTime expireAt = QDateTime::fromString(expireAtText, Qt::ISODate);
    if (!expireAt.isValid()) {
        expireAt = QDateTime::fromString(expireAtText, QStringLiteral("yyyy-MM-ddTHH:mm:ss"));
    }
    if (!expireAt.isValid()) {
        return fail(QObject::tr("离线登录文件有效时间格式无效"));
    }
    if (expireAt <= QDateTime::currentDateTime()) {
        return fail(QObject::tr("离线凭证已过期，请重新导出"));
    }

    QVariantMap line;
    line.insert(QStringLiteral("appId"), appId);
    line.insert(QStringLiteral("appName"), appName);
    line.insert(QStringLiteral("host"), host);
    line.insert(QStringLiteral("srvPort"), srvPort);
    line.insert(QStringLiteral("spaPort"), spaPort);
    line.insert(QStringLiteral("spaKey"), spaKey);

    out->line = line;
    out->userName = userName;
    out->encryptedPassword = password;
    out->expireAt = expireAt;
    return true;
}

} // namespace vpn
