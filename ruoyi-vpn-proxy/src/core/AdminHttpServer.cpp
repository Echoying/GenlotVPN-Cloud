#include "AdminHttpServer.h"
#include "ProxySessionController.h"
#include "util/HttpUtil.h"

#include <QHostAddress>
#include <QJsonDocument>
#include <QTcpSocket>

namespace vpnproxy {

namespace {

bool parseRequestLine(const QByteArray &firstLine, QString *method, QString *path)
{
    const QList<QByteArray> parts = firstLine.trimmed().split(' ');
    if (parts.size() < 2) {
        return false;
    }
    *method = QString::fromUtf8(parts.at(0));
    *path = QString::fromUtf8(parts.at(1));
    return true;
}

QByteArray extractBody(const QByteArray &rawRequest)
{
    const int idx = rawRequest.indexOf("\r\n\r\n");
    return idx < 0 ? QByteArray() : rawRequest.mid(idx + 4);
}

bool hasCompleteHttpRequest(const QByteArray &raw)
{
    const int headerEnd = raw.indexOf("\r\n\r\n");
    if (headerEnd < 0) {
        return false;
    }
    const QByteArray headerBlock = raw.left(headerEnd);
    const QList<QByteArray> lines = headerBlock.split('\n');
    for (int i = 1; i < lines.size(); ++i) {
        const QByteArray line = lines.at(i).trimmed();
        const int colon = line.indexOf(':');
        if (colon <= 0) {
            continue;
        }
        const QString key = QString::fromUtf8(line.left(colon).trimmed());
        if (key.compare(QStringLiteral("Content-Length"), Qt::CaseInsensitive) == 0) {
            bool ok = false;
            const int len = QString::fromUtf8(line.mid(colon + 1).trimmed()).toInt(&ok);
            if (ok && len >= 0) {
                return raw.size() >= headerEnd + 4 + len;
            }
        }
    }
    return true;
}

void writeAndClose(QTcpSocket *client, const QByteArray &response)
{
    client->write(response);
    client->flush();
    client->disconnectFromHost();
    client->deleteLater();
}

QString normalizePath(const QString &path)
{
    const int q = path.indexOf('?');
    return q >= 0 ? path.left(q) : path;
}

} // namespace

AdminHttpServer::AdminHttpServer(ProxySessionController *controller, QObject *parent)
    : QObject(parent)
    , m_controller(controller)
{
    connect(&m_server, &QTcpServer::newConnection, this, &AdminHttpServer::onNewConnection);
}

bool AdminHttpServer::start(const QString &host, int port)
{
    QHostAddress addr(host);
    if (addr.isNull() || port <= 0) {
        return false;
    }
    return m_server.listen(addr, static_cast<quint16>(port));
}

void AdminHttpServer::onNewConnection()
{
    while (m_server.hasPendingConnections()) {
        QTcpSocket *client = m_server.nextPendingConnection();
        if (!client) {
            continue;
        }
        connect(client, &QTcpSocket::readyRead, this, &AdminHttpServer::onClientReadyRead);
        connect(client, &QTcpSocket::disconnected, this, [this, client]() { m_buffers.remove(client); });
    }
}

void AdminHttpServer::onClientReadyRead()
{
    auto *client = qobject_cast<QTcpSocket *>(sender());
    if (!client) {
        return;
    }
    m_buffers[client].append(client->readAll());
    if (!hasCompleteHttpRequest(m_buffers[client])) {
        return;
    }
    const QByteArray raw = m_buffers.take(client);
    client->disconnect(this);
    handleRequest(client, raw);
}

void AdminHttpServer::handleRequest(QTcpSocket *client, const QByteArray &rawRequest)
{
    const int firstLineEnd = rawRequest.indexOf("\r\n");
    if (firstLineEnd < 0) {
        writeAndClose(client, HttpUtil::buildJsonResponse(400, 400, QStringLiteral("无效请求")));
        return;
    }
    QString method;
    QString path;
    if (!parseRequestLine(rawRequest.left(firstLineEnd), &method, &path)) {
        writeAndClose(client, HttpUtil::buildJsonResponse(400, 400, QStringLiteral("无法解析请求行")));
        return;
    }

    const QString normPath = normalizePath(path);
    if (method.compare(QStringLiteral("POST"), Qt::CaseInsensitive) == 0
        && normPath == QStringLiteral("/api/v1/login")) {
        QJsonObject bodyObj;
        QString parseError;
        if (!HttpUtil::parseJsonBody(extractBody(rawRequest), &bodyObj, &parseError)) {
            writeAndClose(client, HttpUtil::buildJsonResponse(400, 400, parseError));
            return;
        }
        const QJsonObject result = m_controller->handleLogin(bodyObj);
        const int code = result.value(QStringLiteral("code")).toInt(500);
        const int httpStatus = code == 200 ? 200 : (code == 409 ? 409 : (code == 400 ? 400 : 500));
        writeAndClose(client, HttpUtil::buildJsonResponse(httpStatus, code,
                                                          result.value(QStringLiteral("msg")).toString(),
                                                          result.value(QStringLiteral("data")).toObject()));
        return;
    }

    if (method.compare(QStringLiteral("POST"), Qt::CaseInsensitive) == 0
        && normPath == QStringLiteral("/api/v1/logout")) {
        const QJsonObject result = m_controller->handleLogout();
        writeAndClose(client, HttpUtil::buildJsonResponse(200, 200,
                                                        result.value(QStringLiteral("msg")).toString()));
        return;
    }

    if (method.compare(QStringLiteral("POST"), Qt::CaseInsensitive) == 0
        && normPath == QStringLiteral("/api/v1/probe")) {
        QJsonObject bodyObj;
        QString parseError;
        if (!HttpUtil::parseJsonBody(extractBody(rawRequest), &bodyObj, &parseError)) {
            writeAndClose(client, HttpUtil::buildJsonResponse(400, 400, parseError));
            return;
        }
        const QJsonObject result = m_controller->handleProbe(bodyObj);
        const int code = result.value(QStringLiteral("code")).toInt(500);
        const int httpStatus = code == 200 ? 200 : (code == 400 ? 400 : 500);
        writeAndClose(client, HttpUtil::buildJsonResponse(httpStatus, code,
                                                          result.value(QStringLiteral("msg")).toString(),
                                                          result.value(QStringLiteral("data")).toObject()));
        return;
    }

    writeAndClose(client, HttpUtil::buildJsonResponse(404, 404, QStringLiteral("未找到接口")));
}

} // namespace vpnproxy
