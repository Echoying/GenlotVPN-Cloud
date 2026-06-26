#pragma once

#include <QDateTime>
#include <QObject>
#include <QString>
#include <QVariantList>
#include <QVariantMap>

namespace vpn {

class TrustedTimeProvider;

struct OfflineLoginPayload {
    QVariantMap line;
    QString userName;
    QString encryptedPassword;
    QString localUserName;
    QString localPasswordHash;
    QDateTime expireAt;
};

/**
 * 解析管理端导出的离线登录 .dat 文件（JSON）
 */
class OfflineLoginImporter {
public:
    static QString defaultDirectory();
    static bool ensureDirectoryExists(QString *errorOut = nullptr);
    static QVariantList scanDirectory(const QString &dir, QString *errorOut = nullptr,
                                    const TrustedTimeProvider *timeProvider = nullptr,
                                    const QString &hmacKey = QString());
    static bool parseFromFile(const QString &filePath, OfflineLoginPayload *out, QString *errorOut,
                              const TrustedTimeProvider *timeProvider = nullptr,
                              bool *tamperedOut = nullptr, const QString &hmacKey = QString());
    static bool verifyLocalCredentials(const OfflineLoginPayload &payload, const QString &inputUser,
                                       const QString &inputPassword, QString *errorOut);
};

} // namespace vpn
