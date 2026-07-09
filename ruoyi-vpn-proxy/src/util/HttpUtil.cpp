#include "HttpUtil.h"

#include <QJsonDocument>
#include <QRegularExpression>

namespace vpnproxy {

QString HttpUtil::sanitizeForLog(const QString &text)
{
    QString out = text;
    static const QRegularExpression passwordRe(
        QStringLiteral("(password|passwd|pwd|Authorization|Cookie)\\s*[:=]\\s*)([^\\s,&\"']+)"),
        QRegularExpression::CaseInsensitiveOption);
    if (passwordRe.isValid()) {
        out.replace(passwordRe, QStringLiteral("\\1***"));
    }
    return out;
}

QByteArray HttpUtil::buildJsonResponse(int httpStatus, int code, const QString &msg,
                                       const QJsonObject &data)
{
    QJsonObject root;
    root.insert(QStringLiteral("code"), code);
    root.insert(QStringLiteral("msg"), msg);
    if (!data.isEmpty()) {
        root.insert(QStringLiteral("data"), data);
    }
    const QByteArray json = QJsonDocument(root).toJson(QJsonDocument::Compact);
    QByteArray response = QByteArrayLiteral("HTTP/1.1 ");
    response += QByteArray::number(httpStatus);
    response += (httpStatus == 200) ? " OK" : " Error";
    response += "\r\nContent-Type: application/json; charset=utf-8\r\n";
    response += "Content-Length: " + QByteArray::number(json.size());
    response += "\r\nConnection: close\r\n\r\n";
    response += json;
    return response;
}

bool HttpUtil::parseJsonBody(const QByteArray &body, QJsonObject *out, QString *error)
{
    QJsonParseError parseError;
    const QJsonDocument doc = QJsonDocument::fromJson(body, &parseError);
    if (parseError.error != QJsonParseError::NoError || !doc.isObject()) {
        if (error) {
            *error = QStringLiteral("JSON 解析失败");
        }
        return false;
    }
    if (out) {
        *out = doc.object();
    }
    return true;
}

} // namespace vpnproxy
