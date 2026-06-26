#include "TrustedTimeProvider.h"

#include "AppLogger.h"

#include <QLocale>
#include <QNetworkAccessManager>
#include <QNetworkReply>
#include <QNetworkRequest>
#include <QTimeZone>
#include <QTimer>
#include <QUrl>
#include <algorithm>

namespace vpn {

namespace {

struct HttpDateParseResult {
    QDateTime utc;
    QString matchedFormat;
    bool ok = false;
};

// RFC 7231 IMF-fixdate；实测 6 个默认源均返回 "ddd, dd MMM yyyy HH:mm:ss GMT"
HttpDateParseResult parseHttpDateDetailed(const QByteArray &headerValue)
{
    HttpDateParseResult result;
    const QString text = QString::fromLatin1(headerValue).trimmed();
    if (text.isEmpty()) {
        return result;
    }

    const QLocale enUs(QLocale::English, QLocale::UnitedStates);
    static const QStringList httpFormats = {
        QStringLiteral("ddd, dd MMM yyyy HH:mm:ss 'GMT'"),
        QStringLiteral("ddd, d MMM yyyy HH:mm:ss 'GMT'"),
        QStringLiteral("ddd, dd MMM yyyy HH:mm:ss 'UTC'"),
        QStringLiteral("ddd, d MMM yyyy HH:mm:ss 'UTC'"),
    };
    for (const QString &fmt : httpFormats) {
        const QDateTime wall = enUs.toDateTime(text, fmt);
        if (wall.isValid()) {
            result.utc = QDateTime(wall.date(), wall.time(), QTimeZone::utc());
            result.matchedFormat = fmt;
            result.ok = true;
            return result;
        }
    }

    const QDateTime rfc2822 = QDateTime::fromString(text, Qt::RFC2822Date);
    if (rfc2822.isValid()) {
        result.utc = rfc2822.toUTC();
        result.matchedFormat = QStringLiteral("Qt::RFC2822Date");
        result.ok = true;
    }
    return result;
}

QStringList defaultGlobalUrls()
{
    return {
        QStringLiteral("https://www.cloudflare.com"),
        QStringLiteral("https://www.microsoft.com"),
        QStringLiteral("https://time.google.com"),
    };
}

QStringList chinaSupplementUrls()
{
    return {
        QStringLiteral("https://www.baidu.com"),
        QStringLiteral("https://www.qq.com"),
    };
}

bool isChinaLikeTimeZone()
{
    const QTimeZone tz = QTimeZone::systemTimeZone();
    const QByteArray id = tz.id();
    if (id.contains("Shanghai") || id.contains("Chongqing") || id.contains("Hong_Kong")
        || id.contains("Taipei") || id.contains("Urumqi")) {
        return true;
    }
    return tz.offsetFromUtc(QDateTime::currentDateTimeUtc()) == 8 * 3600;
}

QStringList readStringList(const QVariantMap &config, const char *key)
{
    QStringList result;
    const QVariant value = config.value(QString::fromLatin1(key));
    if (value.typeId() == QMetaType::QStringList) {
        return value.toStringList();
    }
    if (value.typeId() == QMetaType::QVariantList) {
        for (const QVariant &item : value.toList()) {
            const QString text = item.toString().trimmed();
            if (!text.isEmpty()) {
                result.append(text);
            }
        }
    }
    return result;
}

} // namespace

TrustedTimeProvider::TrustedTimeProvider(QObject *parent)
    : QObject(parent)
    , m_nam(new QNetworkAccessManager(this))
{
    m_finalizeTimer = new QTimer(this);
    m_finalizeTimer->setSingleShot(true);
    connect(m_finalizeTimer, &QTimer::timeout, this, [this]() {
        if (m_syncing && !m_syncFinalized) {
            finalizeSync();
        }
    });
}

TrustedTimeProvider::~TrustedTimeProvider() = default;

void TrustedTimeProvider::applyConfig(const QVariantMap &config)
{
    if (config.contains(QStringLiteral("timeCheckEnabled"))) {
        m_enabled = config.value(QStringLiteral("timeCheckEnabled")).toBool();
    }
    if (config.contains(QStringLiteral("timeCheckTimeoutMs"))) {
        m_timeoutMs = config.value(QStringLiteral("timeCheckTimeoutMs")).toInt();
        if (m_timeoutMs < 500) {
            m_timeoutMs = kDefaultTimeoutMs;
        }
    }
    if (config.contains(QStringLiteral("timeCheckMinSources"))) {
        m_minSources = config.value(QStringLiteral("timeCheckMinSources")).toInt();
        if (m_minSources < 1) {
            m_minSources = 1;
        }
    }
    m_configUrls = readStringList(config, "timeCheckUrls");
    const QString zone = config.value(QStringLiteral("expireAtTimeZone")).toString().trimmed();
    if (!zone.isEmpty()) {
        m_expireAtTimeZoneId = zone;
    }
}

QStringList TrustedTimeProvider::buildUrlList() const
{
    if (!m_configUrls.isEmpty()) {
        return m_configUrls;
    }
    if (isChinaLikeTimeZone()) {
        QStringList urls = chinaSupplementUrls();
        urls.append(defaultGlobalUrls());
        return urls;
    }
    return defaultGlobalUrls();
}

QDateTime TrustedTimeProvider::parseHttpDate(const QByteArray &headerValue)
{
    return parseHttpDateDetailed(headerValue).utc;
}

qint64 TrustedTimeProvider::medianEpoch(const QList<qint64> &epochs)
{
    if (epochs.isEmpty()) {
        return 0;
    }
    QList<qint64> sorted = epochs;
    std::sort(sorted.begin(), sorted.end());
    const int mid = sorted.size() / 2;
    if (sorted.size() % 2 == 1) {
        return sorted[mid];
    }
    return (sorted[mid - 1] + sorted[mid]) / 2;
}

QDateTime TrustedTimeProvider::nowUtc() const
{
    if (m_synced && m_anchorUtc.isValid() && m_anchorTimer.isValid()) {
        return m_anchorUtc.addMSecs(m_anchorTimer.elapsed());
    }
    return QDateTime::currentDateTimeUtc();
}

QDateTime TrustedTimeProvider::parseExpireAtUtc(const QString &expireAtText) const
{
    QDateTime wall = QDateTime::fromString(expireAtText, Qt::ISODate);
    if (!wall.isValid()) {
        wall = QDateTime::fromString(expireAtText, QStringLiteral("yyyy-MM-ddTHH:mm:ss"));
    }
    if (!wall.isValid()) {
        return {};
    }

    const QTimeZone zone(m_expireAtTimeZoneId.toUtf8());
    if (!zone.isValid()) {
        return wall.toUTC();
    }
    const QDateTime zoned(wall.date(), wall.time(), zone);
    return zoned.toUTC();
}

bool TrustedTimeProvider::isExpired(const QString &expireAtText) const
{
    const QDateTime expireUtc = parseExpireAtUtc(expireAtText);
    if (!expireUtc.isValid()) {
        return true;
    }
    return expireUtc <= nowUtc();
}

bool TrustedTimeProvider::isStale() const
{
    if (!m_synced || !m_lastSyncUtc.isValid()) {
        return true;
    }
    return m_lastSyncUtc.secsTo(QDateTime::currentDateTimeUtc()) * 1000 > kStaleAfterMs;
}

void TrustedTimeProvider::applyNetworkAnchor(const QDateTime &utc, TimeTrustLevel level)
{
    m_anchorUtc = utc;
    m_anchorTimer.restart();
    m_lastSyncUtc = QDateTime::currentDateTimeUtc();
    m_trustLevel = level;
    m_synced = true;
    m_usedLocalFallbackLastSync = false;
}

void TrustedTimeProvider::applyLocalFallback()
{
    m_anchorUtc = QDateTime::currentDateTimeUtc();
    m_anchorTimer.restart();
    m_lastSyncUtc = m_anchorUtc;
    m_trustLevel = TimeTrustLevel::LocalFallback;
    m_synced = true;
    m_usedLocalFallbackLastSync = true;
    AppLogger::instance()->warn(
        QStringLiteral("[校时] 网络校时失败，回退本机 UTC 时间：%1")
            .arg(m_anchorUtc.toString(Qt::ISODate)));
}

void TrustedTimeProvider::stopFinalizeTimer()
{
    if (m_finalizeTimer) {
        m_finalizeTimer->stop();
    }
}

bool TrustedTimeProvider::appendEpochIfConsistent(qint64 epoch, const QString &host)
{
    if (m_collectedEpochs.size() >= 2) {
        const qint64 roughMedian = medianEpoch(m_collectedEpochs);
        if (qAbs(epoch - roughMedian) > 30) {
            AppLogger::instance()->warn(
                QStringLiteral("[校时] %1 Date 与中位数偏差超过 30 秒，已忽略").arg(host));
            return false;
        }
    }
    m_collectedEpochs.append(epoch);
    return true;
}

void TrustedTimeProvider::tryRecordReplyDate(QNetworkReply *reply)
{
    if (!reply || !m_syncing || m_syncFinalized) {
        return;
    }
    if (reply->property("syncGeneration").toInt() != m_syncGeneration) {
        return;
    }
    const QByteArray dateHeader = reply->rawHeader("Date");
    if (dateHeader.isEmpty()) {
        return;
    }
    if (reply->property("dateRecorded").toBool()) {
        return;
    }
    const QString host = reply->url().host();
    AppLogger::instance()->info(
        QStringLiteral("[校时] %1 Date 原始：%2").arg(host, QString::fromLatin1(dateHeader)));

    const HttpDateParseResult parsed = parseHttpDateDetailed(dateHeader);
    if (!parsed.ok) {
        reply->setProperty("dateRecorded", true);
        AppLogger::instance()->warn(
            QStringLiteral("[校时] %1 Date 格式校验失败，未匹配已知格式").arg(host));
        return;
    }
    reply->setProperty("dateRecorded", true);
    if (!appendEpochIfConsistent(parsed.utc.toSecsSinceEpoch(), host)) {
        return;
    }
    AppLogger::instance()->info(
        QStringLiteral("[校时] %1 Date 格式校验通过（%2）=> %3 UTC")
            .arg(host, parsed.matchedFormat, parsed.utc.toString(Qt::ISODate)));
    reply->abort();
}

void TrustedTimeProvider::sync()
{
    if (m_syncing) {
        AppLogger::instance()->info(QStringLiteral("[校时] 校时进行中，忽略重复请求"));
        return;
    }

    stopFinalizeTimer();
    ++m_syncGeneration;
    const int generation = m_syncGeneration;
    m_syncing = true;
    m_syncFinalized = false;
    m_collectedEpochs.clear();
    m_pendingRequests = 0;
    for (QNetworkReply *reply : m_activeReplies) {
        reply->abort();
    }
    m_activeReplies.clear();

    if (!m_enabled) {
        applyLocalFallback();
        m_syncing = false;
        emit syncFinished(true, true);
        return;
    }

    const QStringList urls = buildUrlList();
    if (urls.isEmpty()) {
        applyLocalFallback();
        m_syncing = false;
        emit syncFinished(true, true);
        return;
    }

    m_pendingRequests = urls.size();
    AppLogger::instance()->info(
        QStringLiteral("[校时] 开始获取网络时间，共 %1 个源，超时 %2ms，至少 %3 个源有效")
            .arg(urls.size())
            .arg(m_timeoutMs)
            .arg(m_minSources));
    for (const QString &urlText : urls) {
        AppLogger::instance()->info(QStringLiteral("[校时] 正在请求 %1").arg(urlText));
        QNetworkRequest request{QUrl(urlText)};
        request.setAttribute(QNetworkRequest::RedirectPolicyAttribute,
                             QNetworkRequest::NoLessSafeRedirectPolicy);
        request.setHeader(QNetworkRequest::UserAgentHeader, QStringLiteral("GenlotVPN"));
        QNetworkReply *reply = m_nam->get(request);
        reply->setProperty("syncGeneration", generation);
        m_activeReplies.insert(reply);
        reply->setProperty("dateRecorded", false);

        connect(reply, &QNetworkReply::metaDataChanged, this, [this, reply]() {
            tryRecordReplyDate(reply);
        });

        auto *timer = new QTimer(reply);
        timer->setSingleShot(true);
        timer->setInterval(m_timeoutMs);
        connect(timer, &QTimer::timeout, reply, [reply]() { reply->abort(); });
        timer->start();

        connect(reply, &QNetworkReply::finished, this, [this, reply]() { onRequestFinished(reply); });
    }

    m_finalizeTimer->start(m_timeoutMs + 800);
}

void TrustedTimeProvider::onRequestFinished(QNetworkReply *reply)
{
    if (!reply) {
        return;
    }
    if (reply->property("syncGeneration").toInt() != m_syncGeneration) {
        reply->deleteLater();
        return;
    }
    if (!m_syncing || m_syncFinalized) {
        reply->deleteLater();
        return;
    }

    m_activeReplies.remove(reply);
    --m_pendingRequests;

    if (reply->error() == QNetworkReply::NoError
        || reply->error() == QNetworkReply::OperationCanceledError) {
        tryRecordReplyDate(reply);
        if (!reply->property("dateRecorded").toBool() && reply->rawHeader("Date").isEmpty()) {
            AppLogger::instance()->warn(
                QStringLiteral("[校时] %1 响应无 Date 头").arg(reply->url().host()));
            reply->setProperty("dateRecorded", true);
        }
    } else {
        AppLogger::instance()->warn(
            QStringLiteral("[校时] %1 请求失败：%2")
                .arg(reply->url().host(), reply->errorString()));
    }
    reply->deleteLater();

    if (m_pendingRequests <= 0) {
        finalizeSync();
    }
}

void TrustedTimeProvider::finalizeSync()
{
    if (m_syncFinalized) {
        return;
    }
    m_syncFinalized = true;
    m_syncing = false;
    stopFinalizeTimer();

    QList<qint64> epochs = m_collectedEpochs;
    if (epochs.size() >= 2) {
        const qint64 roughMedian = medianEpoch(epochs);
        QList<qint64> filtered;
        filtered.reserve(epochs.size());
        for (const qint64 epoch : epochs) {
            if (qAbs(epoch - roughMedian) <= 30) {
                filtered.append(epoch);
            }
        }
        if (!filtered.isEmpty()) {
            epochs = filtered;
        }
    }

    if (epochs.size() >= m_minSources) {
        const QDateTime utc = QDateTime::fromSecsSinceEpoch(medianEpoch(epochs), QTimeZone::utc());
        applyNetworkAnchor(utc, TimeTrustLevel::NetworkMedian);
        AppLogger::instance()->info(
            QStringLiteral("[校时] 网络 UTC 时间已同步：%1（%2 个源，取中位数）")
                .arg(utc.toString(Qt::ISODate))
                .arg(epochs.size()));
        emit syncFinished(true, false);
        return;
    }

    if (epochs.size() == 1) {
        const QDateTime utc = QDateTime::fromSecsSinceEpoch(epochs.first(), QTimeZone::utc());
        applyNetworkAnchor(utc, TimeTrustLevel::SingleSource);
        AppLogger::instance()->warn(
            QStringLiteral("[校时] 仅 1 个网络时间源可用，已同步：%1 UTC").arg(utc.toString(Qt::ISODate)));
        emit syncFinished(true, false);
        return;
    }

    if (m_synced && m_trustLevel != TimeTrustLevel::LocalFallback && m_anchorUtc.isValid()
        && !isStale()) {
        AppLogger::instance()->warn(
            QStringLiteral("[校时] 本次校时源不足（%1 个），保留上次网络 UTC：%2")
                .arg(epochs.size())
                .arg(m_anchorUtc.toString(Qt::ISODate)));
        emit syncFinished(true, false);
        return;
    }

    applyLocalFallback();
    emit syncFinished(true, true);
}

} // namespace vpn
