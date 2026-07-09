#pragma once

#include <QByteArray>
#include <QJsonObject>
#include <QString>

namespace vpnproxy {

class HttpUtil {
public:
    static QString sanitizeForLog(const QString &text);
    static QByteArray buildJsonResponse(int httpStatus, int code, const QString &msg,
                                        const QJsonObject &data = QJsonObject());
    static bool parseJsonBody(const QByteArray &body, QJsonObject *out, QString *error);
};

} // namespace vpnproxy
