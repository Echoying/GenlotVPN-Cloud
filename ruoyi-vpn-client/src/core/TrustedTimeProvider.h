#pragma once

#include <QDateTime>
#include <QElapsedTimer>
#include <QObject>
#include <QSet>
#include <QString>
#include <QStringList>
#include <QVariantMap>

class QNetworkAccessManager;
class QNetworkReply;
class QTimer;

namespace vpn {

enum class TimeTrustLevel {
    Unsynced,
    NetworkMedian,
    SingleSource,
    LocalFallback,
};

/**
 * 通过 HTTPS Date 头获取可信 UTC 时间，用于离线凭证 expire_at 校验。
 */
class TrustedTimeProvider : public QObject {
    Q_OBJECT
public:
    static constexpr int kDefaultTimeoutMs = 3000;
    static constexpr int kDefaultMinSources = 2;
    static constexpr int kStaleAfterMs = 10 * 60 * 1000;

    explicit TrustedTimeProvider(QObject *parent = nullptr);
    ~TrustedTimeProvider() override;

    void applyConfig(const QVariantMap &config);

    bool isEnabled() const { return m_enabled; }
    TimeTrustLevel trustLevel() const { return m_trustLevel; }
    bool usedLocalFallbackLastSync() const { return m_usedLocalFallbackLastSync; }

    QDateTime nowUtc() const;
    QDateTime parseExpireAtUtc(const QString &expireAtText) const;
    bool isExpired(const QString &expireAtText) const;

    bool isSynced() const { return m_synced; }
    bool isStale() const;
    bool isSyncing() const { return m_syncing; }

public slots:
    void sync();

signals:
    void syncFinished(bool success, bool usedLocalFallback);

private slots:
    void onRequestFinished(QNetworkReply *reply);
    void finalizeSync();

private:
    QStringList buildUrlList() const;
    void applyNetworkAnchor(const QDateTime &utc, TimeTrustLevel level);
    void applyLocalFallback();
    void tryRecordReplyDate(QNetworkReply *reply);
    bool appendEpochIfConsistent(qint64 epoch, const QString &host);
    void stopFinalizeTimer();
    static QDateTime parseHttpDate(const QByteArray &headerValue);
    static qint64 medianEpoch(const QList<qint64> &epochs);

    QNetworkAccessManager *m_nam = nullptr;
    bool m_enabled = true;
    int m_timeoutMs = kDefaultTimeoutMs;
    int m_minSources = kDefaultMinSources;
    QStringList m_configUrls;
    QString m_expireAtTimeZoneId = QStringLiteral("Asia/Shanghai");

    bool m_syncing = false;
    bool m_syncFinalized = false;
    bool m_synced = false;
    bool m_usedLocalFallbackLastSync = false;
    int m_pendingRequests = 0;
    int m_syncGeneration = 0;
    QTimer *m_finalizeTimer = nullptr;
    QSet<QNetworkReply *> m_activeReplies;
    QList<qint64> m_collectedEpochs;

    TimeTrustLevel m_trustLevel = TimeTrustLevel::Unsynced;
    QDateTime m_anchorUtc;
    QElapsedTimer m_anchorTimer;
    QDateTime m_lastSyncUtc;
};

} // namespace vpn
