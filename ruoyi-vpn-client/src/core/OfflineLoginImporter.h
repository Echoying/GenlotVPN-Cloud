#pragma once

#include <QDateTime>
#include <QObject>
#include <QString>
#include <QVariantList>
#include <QVariantMap>

namespace vpn {

struct OfflineLoginPayload {
    QVariantMap line;
    QString userName;
    QString encryptedPassword;
    QString localUserName;
    QString localEncryptedPassword;
    QDateTime expireAt;
};

/**
 * 解析管理端导出的离线登录 .dat 文件（JSON）
 */
class OfflineLoginImporter {
public:
    static QString defaultDirectory();
    static bool ensureDirectoryExists(QString *errorOut = nullptr);
    static QVariantList scanDirectory(const QString &dir, QString *errorOut = nullptr);
    static bool parseFromFile(const QString &filePath, OfflineLoginPayload *out, QString *errorOut);
    static bool verifyLocalCredentials(const OfflineLoginPayload &payload, const QString &inputUser,
                                       const QString &inputPassword, QString *errorOut);
};

} // namespace vpn
