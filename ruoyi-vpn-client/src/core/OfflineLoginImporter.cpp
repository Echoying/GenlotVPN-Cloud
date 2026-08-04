#include "OfflineLoginImporter.h"

#include "AppLogger.h"
#include "AppPaths.h"
#include "TrustedTimeProvider.h"
#include "crypto/BcryptVerifier.h"
#include "crypto/OfflineLoginIntegrity.h"
#include <QDir>
#include <QFile>
#include <QFileInfo>
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
    return AppPaths::offlineLoginDir();
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

QVariantList OfflineLoginImporter::scanDirectory(const QString &dir, QString *errorOut,
                                                 const TrustedTimeProvider *timeProvider,
                                                 const QString &hmacKey)
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
    QStringList tamperedMessages;
    for (const QString &fileName : files) {
        const QString filePath = directory.absoluteFilePath(fileName);
        OfflineLoginPayload payload;
        QString parseError;
        bool tampered = false;
        if (!parseFromFile(filePath, &payload, &parseError, timeProvider, &tampered, hmacKey)) {
            AppLogger::instance()->debug(
                QStringLiteral("[离线登录] 跳过文件 %1: %2").arg(fileName, parseError));
            if (tampered) {
                tamperedMessages.append(parseError);
            }
            continue;
        }

        QVariantMap line = payload.line;
        line.insert(QStringLiteral("offlineFilePath"), filePath);
        line.insert(QStringLiteral("localUserName"), payload.localUserName);
        result.append(line);
    }
    if (!tamperedMessages.isEmpty() && errorOut) {
        *errorOut = tamperedMessages.join(QStringLiteral("\n"));
    }
    return result;
}

bool OfflineLoginImporter::parseFromFile(const QString &filePath, OfflineLoginPayload *out,
                                         QString *errorOut, const TrustedTimeProvider *timeProvider,
                                         bool *tamperedOut, const QString &hmacKey)
{
    if (tamperedOut) {
        *tamperedOut = false;
    }

    const QString effectiveHmacKey =
        hmacKey.isEmpty() ? OfflineLoginIntegrity::defaultHmacKey() : hmacKey;
    auto fail = [errorOut](const QString &msg) {
        if (errorOut) {
            *errorOut = msg;
        }
        return false;
    };

    if (!out) {
        return fail(QObject::tr("内部错误"));
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

    const QString payloadHmac = readString(root, "payload_hmac", "payloadHmac");
    if (payloadHmac.isEmpty()) {
        return fail(QObject::tr("离线登录文件缺少完整性校验信息"));
    }
    if (!OfflineLoginIntegrity::verifyPayload(root, effectiveHmacKey)) {
        if (tamperedOut) {
            *tamperedOut = true;
        }
        AppLogger::instance()->warn(
            QStringLiteral("[离线登录] HMAC 校验失败：%1（若未修改文件，请检查 config.json 的 offlineHmacKey "
                           "是否与服务端 vpn.offline.hmac.key 一致，或重新导出）")
                .arg(QFileInfo(filePath).fileName()));
        return fail(QObject::tr("请勿篡改文件：%1").arg(QFileInfo(filePath).fileName()));
    }

    const QString localPasswordHash =
        readString(root, "local_password_hash", "localPasswordHash");
    if (localPasswordHash.isEmpty()) {
        return fail(QObject::tr("离线登录文件缺少本地用户密码哈希"));
    }

    const QString appId = readString(root, "app_id", "appId");
    const QString appName = readString(root, "app_name", "appName");
    const QString host = readString(root, "host");
    const int srvPort = readPort(root, "srv_port", "srvPort");
    const int spaPort = readPort(root, "spa_port", "spaPort");
    const QString spaKey = readString(root, "spa_key", "spaKey");
    const QString userName = readString(root, "user_name", "userName");
    const QString password = readString(root, "password");
    const QString localUserName = readString(root, "local_user_name", "localUserName");
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
    if (localUserName.isEmpty()) {
        return fail(QObject::tr("离线登录文件缺少本地用户信息"));
    }
    if (expireAtText.isEmpty()) {
        return fail(QObject::tr("离线登录文件缺少有效时间"));
    }

    QDateTime expireAt;
    if (timeProvider) {
        expireAt = timeProvider->parseExpireAtUtc(expireAtText);
        if (!expireAt.isValid()) {
            return fail(QObject::tr("离线登录文件有效时间格式无效"));
        }
        if (timeProvider->isExpired(expireAtText)) {
            return fail(QObject::tr("离线凭证已过期，请重新导出"));
        }
    } else {
        expireAt = QDateTime::fromString(expireAtText, Qt::ISODate);
        if (!expireAt.isValid()) {
            expireAt = QDateTime::fromString(expireAtText, QStringLiteral("yyyy-MM-ddTHH:mm:ss"));
        }
        if (!expireAt.isValid()) {
            return fail(QObject::tr("离线登录文件有效时间格式无效"));
        }
        if (expireAt <= QDateTime::currentDateTime()) {
            return fail(QObject::tr("离线凭证已过期，请重新导出"));
        }
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
    out->localUserName = localUserName;
    out->localPasswordHash = localPasswordHash;
    out->expireAtText = expireAtText;
    out->expireAt = expireAt;
    return true;
}

bool OfflineLoginImporter::verifyLocalCredentials(const OfflineLoginPayload &payload,
                                                  const QString &inputUser,
                                                  const QString &inputPassword,
                                                  QString *errorOut)
{
    auto fail = [errorOut](const QString &msg) {
        if (errorOut) {
            *errorOut = msg;
        }
        return false;
    };

    if (payload.localUserName.trimmed() != inputUser.trimmed()) {
        return fail(QObject::tr("本地账号或密码不正确"));
    }
    if (!BcryptVerifier::matches(inputPassword, payload.localPasswordHash)) {
        return fail(QObject::tr("本地账号或密码不正确"));
    }
    return true;
}

} // namespace vpn
