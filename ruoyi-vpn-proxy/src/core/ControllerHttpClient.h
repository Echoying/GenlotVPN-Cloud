#pragma once

#include <QJsonObject>
#include <QNetworkAccessManager>
#include <QNetworkReply>
#include <QObject>
#include <QString>
#include <QVariantMap>
#include <functional>

#include "SessionLogModel.h"

namespace vpnproxy {

/** 易安联本地控制器 HTTP API（127.0.0.1:30303），同步调用 */
class ControllerHttpClient : public QObject {
    Q_OBJECT
public:
    using SessionLogCallback = std::function<void(const SessionLogPayload &)>;

    explicit ControllerHttpClient(const QString &baseUrl, bool controllerAesEnabled = true,
                                  QObject *parent = nullptr);

    void setSessionLogCallback(SessionLogCallback callback);

    struct ConnectResult {
        bool ok = false;
        QString error;
        int tunnelStatus = -1;
    };

    struct DetectResult {
        bool ok = false;
        bool available = false;
        QString error;
    };

    DetectResult detectLine(const QVariantMap &line);
    ConnectResult connectLine(const QVariantMap &line, const QString &username,
                              const QString &password);
    bool logout(QString *errorOut = nullptr);

    struct UpstreamReadyResult {
        bool ok = false;
        QString error;
    };

    /** 30303 成功后轮询上游 token 接口，确认隧道内可达再向服务端返回 login 成功 */
    UpstreamReadyResult waitUpstreamTokenReachable(const QString &upstreamUrl);

    static QVariantMap lineToJson(const QVariantMap &line);

private:
    struct HttpResult {
        bool ok = false;
        QString error;
        QJsonObject body;
    };

    QByteArray encodeControllerBody(const QByteArray &plainJson) const;
    QByteArray decodeControllerBody(const QByteArray &wireBody) const;
    HttpResult postJson(const QString &path, const QByteArray &plainJsonBody, int timeoutMs,
                        const QString &stepLabel);
    HttpResult getJson(const QString &path, int timeoutMs, const QString &stepLabel);
    void logHttpStep(const QString &stepLabel, const QString &method, const QString &path,
                     qint64 elapsedMs, const QString &requestPlain, const HttpResult &result);
    void logUpstreamProbe(int attempt, int maxAttempts, qint64 elapsedMs, const QString &tokenUrl,
                          const QString &requestBody, bool reachable, const QString &responseLog);
    static QString buildTokenUrl(const QString &upstreamUrl);
    static bool isUpstreamUnreachableError(QNetworkReply::NetworkError error);
    static QString truncateForLog(const QString &text, int maxLen = 16384);
    static QString formatJsonForLog(const QByteArray &plainJson);
    static QString extractError(const QJsonObject &obj, const QString &fallback);
    static bool isCodeOk(const QJsonObject &obj);

    QString m_baseUrl;
    bool m_controllerAesEnabled = true;
    QNetworkAccessManager m_nam;
    SessionLogCallback m_sessionLogCallback;
};

} // namespace vpnproxy
