#pragma once

#include <QJsonObject>
#include <QNetworkAccessManager>
#include <QObject>
#include <QString>
#include <QVariantMap>

namespace vpnproxy {

/** 易安联本地控制器 HTTP API（127.0.0.1:30303），同步调用 */
class ControllerHttpClient : public QObject {
    Q_OBJECT
public:
    explicit ControllerHttpClient(const QString &baseUrl, QObject *parent = nullptr);

    struct ConnectResult {
        bool ok = false;
        QString error;
        int tunnelStatus = -1;
    };

    ConnectResult connectLine(const QVariantMap &line, const QString &username,
                              const QString &password);
    bool logout(QString *errorOut = nullptr);

    static QVariantMap lineToJson(const QVariantMap &line);

private:
    struct HttpResult {
        bool ok = false;
        QString error;
        QJsonObject body;
    };

    HttpResult postJson(const QString &path, const QByteArray &body, int timeoutMs = 120000);
    HttpResult getJson(const QString &path, int timeoutMs = 30000);
    static QString extractError(const QJsonObject &obj, const QString &fallback);
    static bool isCodeOk(const QJsonObject &obj);

    QString m_baseUrl;
    QNetworkAccessManager m_nam;
};

} // namespace vpnproxy
