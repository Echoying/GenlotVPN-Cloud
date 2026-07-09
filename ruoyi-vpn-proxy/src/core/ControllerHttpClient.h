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
    explicit ControllerHttpClient(const QString &baseUrl, bool controllerAesEnabled = true,
                                  QObject *parent = nullptr);

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

    static QVariantMap lineToJson(const QVariantMap &line);

private:
    struct HttpResult {
        bool ok = false;
        QString error;
        QJsonObject body;
    };

    QByteArray encodeControllerBody(const QByteArray &plainJson) const;
    QByteArray decodeControllerBody(const QByteArray &wireBody) const;
    HttpResult postJson(const QString &path, const QByteArray &plainJsonBody, int timeoutMs = 120000);
    HttpResult getJson(const QString &path, int timeoutMs = 30000);
    static QString extractError(const QJsonObject &obj, const QString &fallback);
    static bool isCodeOk(const QJsonObject &obj);

    QString m_baseUrl;
    bool m_controllerAesEnabled = true;
    QNetworkAccessManager m_nam;
};

} // namespace vpnproxy
