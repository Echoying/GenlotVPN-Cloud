#pragma once

#include <QJsonObject>
#include <QString>

namespace vpn {

/** 离线登录 .dat HMAC 完整性校验 */
class OfflineLoginIntegrity {
public:
    static QString defaultHmacKey();

    static QString buildCanonical(const QJsonObject &root);
    static QString signCanonical(const QString &canonical, const QString &hmacKey);
    static bool verifyPayload(const QJsonObject &root, const QString &hmacKey);
};

} // namespace vpn
