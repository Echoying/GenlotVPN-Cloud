#include "OpenApiProxyService.h"
#include "AppLogger.h"

#include <QHostAddress>
#include <QMap>
#include <QNetworkAccessManager>
#include <QNetworkReply>
#include <QNetworkRequest>
#include <QSslConfiguration>
#include <QUrl>

namespace vpnproxy {

namespace {

QString peerIp(QTcpSocket *socket)
{
    if (!socket) {
        return QString();
    }
    return socket->peerAddress().toString().remove(QStringLiteral("::ffff:"));
}

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

QMap<QString, QString> extractHeaders(const QByteArray &rawRequest)
{
    QMap<QString, QString> headers;
    const int end = rawRequest.indexOf("\r\n\r\n");
    const QByteArray headerBlock = end >= 0 ? rawRequest.left(end) : rawRequest;
    const QList<QByteArray> lines = headerBlock.split('\n');
    for (int i = 1; i < lines.size(); ++i) {
        const QByteArray line = lines.at(i).trimmed();
        const int colon = line.indexOf(':');
        if (colon <= 0) {
            continue;
        }
        headers.insert(QString::fromUtf8(line.left(colon).trimmed()),
                       QString::fromUtf8(line.mid(colon + 1).trimmed()));
    }
    return headers;
}

QString pathAndQuery(const QString &path)
{
    const int q = path.indexOf('?');
    return q >= 0 ? path.left(q) : path;
}

QString queryString(const QString &path)
{
    const int q = path.indexOf('?');
    return q >= 0 ? path.mid(q + 1) : QString();
}

QString truncateForDisplay(const QByteArray &data, int maxLen = 16384)
{
    if (data.size() <= maxLen) {
        return QString::fromUtf8(data);
    }
    return QString::fromUtf8(data.left(maxLen))
           + QStringLiteral("\n\n... [已截断，共 %1 字节]").arg(data.size());
}

QString formatHttpResponseLog(int status, const QByteArray &body, const QString &errorText = QString())
{
    if (!errorText.isEmpty()) {
        return QStringLiteral("HTTP/1.1 502 Bad Gateway\r\nX-Proxy-Error: %1").arg(errorText);
    }
    QString log = QStringLiteral("HTTP/1.1 %1\r\nContent-Length: %2\r\n\r\n")
                      .arg(status > 0 ? status : 502)
                      .arg(body.size());
    log += truncateForDisplay(body);
    return log;
}

QString httpStatusReason(int status)
{
    switch (status) {
    case 200: return QStringLiteral("OK");
    case 503: return QStringLiteral("Service Unavailable");
    case 502: return QStringLiteral("Bad Gateway");
    case 404: return QStringLiteral("Not Found");
    case 403: return QStringLiteral("Forbidden");
    default: return status >= 200 && status < 300 ? QStringLiteral("OK") : QStringLiteral("Error");
    }
}

bool hasCompleteHttpRequest(const QByteArray &raw)
{
    const int headerEnd = raw.indexOf("\r\n\r\n");
    if (headerEnd < 0) {
        return false;
    }
    const QMap<QString, QString> headers = extractHeaders(raw);
    QString contentLengthStr;
    for (auto it = headers.constBegin(); it != headers.constEnd(); ++it) {
        if (it.key().compare(QStringLiteral("Content-Length"), Qt::CaseInsensitive) == 0) {
            contentLengthStr = it.value();
            break;
        }
    }
    if (contentLengthStr.isEmpty()) {
        return true;
    }
    bool ok = false;
    const int contentLength = contentLengthStr.toInt(&ok);
    if (!ok || contentLength < 0) {
        return true;
    }
    return raw.size() >= headerEnd + 4 + contentLength;
}

void writeHttpResponseAndClose(QTcpSocket *client, const QByteArray &response)
{
    if (!client) {
        return;
    }
    client->write(response);
    client->flush();
    QObject::connect(client, &QTcpSocket::disconnected, client, &QObject::deleteLater, Qt::SingleShotConnection);
    client->disconnectFromHost();
}

} // namespace

OpenApiProxyService::OpenApiProxyService(QObject *parent) : QObject(parent)
{
    m_logModel = new ProxyLogModel(this);
    connect(&m_server, &QTcpServer::newConnection, this, &OpenApiProxyService::onNewConnection);
}

OpenApiProxyService::~OpenApiProxyService()
{
    closeServer();
}

QString OpenApiProxyService::listenEndpoint() const
{
    if (!m_running) {
        return QString();
    }
    return QStringLiteral("http://%1:%2").arg(m_listenHost).arg(m_listenPort);
}

bool OpenApiProxyService::startListenerOnly(const QString &listenHost, int listenPort,
                                            const QStringList &allowedSourceIps)
{
    closeServer();
    m_upstreamUrl.clear();
    m_vpnReady = false;
    m_listenHost = listenHost.trimmed();
    m_listenPort = listenPort;
    m_allowedSourceIps = allowedSourceIps;

    QHostAddress addr(m_listenHost);
    if (addr.isNull() || listenPort <= 0) {
        emit logMessage(QStringLiteral("error"), QStringLiteral("代理监听配置无效"));
        return false;
    }
    if (!m_server.listen(addr, static_cast<quint16>(m_listenPort))) {
        emit logMessage(QStringLiteral("error"),
                        QStringLiteral("代理监听失败 %1:%2 - %3")
                            .arg(m_listenHost)
                            .arg(m_listenPort)
                            .arg(m_server.errorString()));
        return false;
    }
    m_running = true;
    emit started(listenEndpoint());
    emit logMessage(QStringLiteral("info"), QStringLiteral("同步代理监听已启动 %1").arg(listenEndpoint()));
    return true;
}

void OpenApiProxyService::updateUpstream(const QString &upstreamUrl)
{
    m_upstreamUrl = upstreamUrl.trimmed();
    emit logMessage(QStringLiteral("info"), QStringLiteral("代理上游已更新: %1").arg(m_upstreamUrl));
}

void OpenApiProxyService::updateAllowedSourceIps(const QStringList &allowedSourceIps)
{
    m_allowedSourceIps = allowedSourceIps;
    emit logMessage(QStringLiteral("info"),
                    QStringLiteral("代理来源 IP 白名单已更新: %1")
                        .arg(m_allowedSourceIps.isEmpty() ? QStringLiteral("(不限)")
                                                          : m_allowedSourceIps.join(QStringLiteral(", "))));
}

void OpenApiProxyService::clearUpstream()
{
    m_upstreamUrl.clear();
    m_vpnReady = false;
    emit logMessage(QStringLiteral("info"), QStringLiteral("代理上游已清除，恢复 503 待命"));
}

void OpenApiProxyService::setVpnReady(bool ready)
{
    m_vpnReady = ready;
}

void OpenApiProxyService::clearLogs()
{
    if (m_logModel) {
        m_logModel->clear();
    }
}

void OpenApiProxyService::closeServer()
{
    const auto pending = m_server.findChildren<QTcpSocket *>();
    for (QTcpSocket *socket : pending) {
        m_clientBuffers.remove(socket);
        socket->disconnectFromHost();
        socket->deleteLater();
    }
    m_clientBuffers.clear();
    if (m_server.isListening()) {
        m_server.close();
    }
    m_running = false;
}

bool OpenApiProxyService::isAllowedPeer(const QString &peerIp) const
{
    if (m_allowedSourceIps.isEmpty()) {
        return true;
    }
    for (const QString &allowed : m_allowedSourceIps) {
        if (allowed.trimmed() == peerIp) {
            return true;
        }
    }
    return false;
}

QString OpenApiProxyService::buildTargetUrl(const QString &upstreamUrl, const QString &requestPath,
                                            const QString &query) const
{
    QString base = upstreamUrl.trimmed();
    if (base.isEmpty()) {
        return QString();
    }
    while (base.endsWith(QLatin1Char('/'))) {
        base.chop(1);
    }
    QString path = requestPath.trimmed();
    if (!path.isEmpty() && !path.startsWith(QLatin1Char('/'))) {
        path.prepend(QLatin1Char('/'));
    }
    QString target = path.isEmpty() ? base : (base + path);
    if (!query.isEmpty()) {
        target += QLatin1Char('?') + query;
    }
    return target;
}

void OpenApiProxyService::recordLog(const QString &method, const QString &path, int status,
                                    const QString &peer, const QString &requestLog,
                                    const QString &responseLog, bool success)
{
    if (!m_logModel) {
        return;
    }
    ProxyLogEntry entry;
    entry.id = QString::number(++m_logSeq);
    entry.time = QDateTime::currentDateTime();
    entry.method = method;
    entry.path = path;
    entry.status = status;
    entry.peerIp = peer;
    entry.requestLog = requestLog;
    entry.responseLog = responseLog;
    entry.success = success;
    m_logModel->appendEntry(entry);
}

void OpenApiProxyService::respondServiceUnavailable(QTcpSocket *client, const QString &peer,
                                                    const QString &method, const QString &path,
                                                    const QString &requestLog)
{
    const QByteArray body = QByteArrayLiteral("{\"error\":\"vpn_not_ready\"}");
    QByteArray response = QByteArrayLiteral("HTTP/1.1 503 Service Unavailable\r\n"
                                            "Content-Type: application/json; charset=UTF-8\r\n"
                                            "Content-Length: ");
    response += QByteArray::number(body.size());
    response += QByteArrayLiteral("\r\nConnection: close\r\n\r\n");
    response += body;
    const QString responseLog = formatHttpResponseLog(503, body);
    recordLog(method, pathAndQuery(path), 503, peer, requestLog, responseLog, false);
    writeHttpResponseAndClose(client, response);
}

void OpenApiProxyService::onNewConnection()
{
    while (m_server.hasPendingConnections()) {
        QTcpSocket *client = m_server.nextPendingConnection();
        if (!client) {
            continue;
        }
        const QString ip = peerIp(client);
        if (!isAllowedPeer(ip)) {
            const QByteArray denied = QByteArrayLiteral("HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\nConnection: close\r\n\r\n");
            recordLog(QStringLiteral("DENY"), QStringLiteral("-"), 403, ip,
                      QStringLiteral("非白名单来源 IP: %1").arg(ip),
                      QStringLiteral("HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\n\r\n"), false);
            writeHttpResponseAndClose(client, denied);
            continue;
        }
        connect(client, &QTcpSocket::readyRead, this, &OpenApiProxyService::onClientReadyRead);
        connect(client, &QTcpSocket::disconnected, this, [this, client]() { m_clientBuffers.remove(client); });
    }
}

void OpenApiProxyService::onClientReadyRead()
{
    auto *client = qobject_cast<QTcpSocket *>(sender());
    if (!client) {
        return;
    }
    m_clientBuffers[client].append(client->readAll());
    if (!hasCompleteHttpRequest(m_clientBuffers[client])) {
        return;
    }
    client->disconnect(this);
    const QByteArray rawRequest = m_clientBuffers.take(client);
    forwardRequest(client, rawRequest);
}

void OpenApiProxyService::forwardRequest(QTcpSocket *client, const QByteArray &rawRequest)
{
    const QString peer = peerIp(client);
    const QString requestLog = truncateForDisplay(rawRequest);
    const int firstLineEnd = rawRequest.indexOf("\r\n");
    if (firstLineEnd < 0) {
        writeHttpResponseAndClose(client, QByteArrayLiteral("HTTP/1.1 400 Bad Request\r\nConnection: close\r\n\r\n"));
        client->deleteLater();
        return;
    }
    QString method;
    QString path;
    if (!parseRequestLine(rawRequest.left(firstLineEnd), &method, &path)) {
        writeHttpResponseAndClose(client, QByteArrayLiteral("HTTP/1.1 400 Bad Request\r\nConnection: close\r\n\r\n"));
        client->deleteLater();
        return;
    }
    if (!path.startsWith(QStringLiteral("/enadmin/api/open/v1"))) {
        const QByteArray resp = QByteArrayLiteral("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n");
        recordLog(method, pathAndQuery(path), 404, peer, requestLog,
                  QStringLiteral("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n"), false);
        writeHttpResponseAndClose(client, resp);
        return;
    }
    if (!isForwardingReady()) {
        respondServiceUnavailable(client, peer, method, path, requestLog);
        return;
    }

    const QString targetUrl = buildTargetUrl(m_upstreamUrl, pathAndQuery(path), queryString(path));
    if (targetUrl.isEmpty()) {
        writeHttpResponseAndClose(client, QByteArrayLiteral("HTTP/1.1 502 Bad Gateway\r\nConnection: close\r\n\r\n"));
        return;
    }

    const QString fullRequestLog = QStringLiteral("URL: %1\n\n").arg(targetUrl) + requestLog;
    QNetworkAccessManager *nam = new QNetworkAccessManager(this);
    QNetworkRequest req{QUrl(targetUrl)};
    const QMap<QString, QString> headers = extractHeaders(rawRequest);
    for (auto it = headers.constBegin(); it != headers.constEnd(); ++it) {
        const QString key = it.key();
        if (key.compare(QStringLiteral("Host"), Qt::CaseInsensitive) == 0
            || key.compare(QStringLiteral("Connection"), Qt::CaseInsensitive) == 0
            || key.compare(QStringLiteral("Content-Length"), Qt::CaseInsensitive) == 0
            || key.compare(QStringLiteral("Accept-Encoding"), Qt::CaseInsensitive) == 0) {
            continue;
        }
        req.setRawHeader(key.toUtf8(), it.value().toUtf8());
    }
    if (targetUrl.startsWith(QStringLiteral("https"), Qt::CaseInsensitive)) {
        QSslConfiguration ssl = QSslConfiguration::defaultConfiguration();
        ssl.setPeerVerifyMode(QSslSocket::VerifyNone);
        req.setSslConfiguration(ssl);
    }

    const QByteArray body = extractBody(rawRequest);
    QNetworkReply *reply = nullptr;
    if (method.compare(QStringLiteral("GET"), Qt::CaseInsensitive) == 0) {
        reply = nam->get(req);
    } else if (method.compare(QStringLiteral("POST"), Qt::CaseInsensitive) == 0) {
        reply = nam->post(req, body);
    } else if (method.compare(QStringLiteral("PUT"), Qt::CaseInsensitive) == 0) {
        reply = nam->put(req, body);
    } else if (method.compare(QStringLiteral("DELETE"), Qt::CaseInsensitive) == 0) {
        reply = nam->deleteResource(req);
    } else {
        writeHttpResponseAndClose(client, QByteArrayLiteral("HTTP/1.1 405 Method Not Allowed\r\nConnection: close\r\n\r\n"));
        nam->deleteLater();
        return;
    }

    connect(reply, &QNetworkReply::finished, this,
            [this, client, reply, nam, method, targetUrl, fullRequestLog, peer]() {
        int status = reply->attribute(QNetworkRequest::HttpStatusCodeAttribute).toInt();
        const QByteArray respBody = reply->readAll();
        bool success = false;
        QString responseLog;
        if (reply->error() != QNetworkReply::NoError) {
            status = 502;
            responseLog = formatHttpResponseLog(status, QByteArray(), reply->errorString());
        } else {
            success = status >= 200 && status < 400;
            responseLog = formatHttpResponseLog(status, respBody);
        }
        recordLog(method, targetUrl, status > 0 ? status : 502, peer, fullRequestLog, responseLog, success);

        const int httpStatus = status > 0 ? status : 502;
        QByteArray response = QByteArray("HTTP/1.1 ") + QByteArray::number(httpStatus) + QByteArray(" ")
                              + httpStatusReason(httpStatus).toUtf8() + QByteArray("\r\n");
        const QByteArray contentType = reply->header(QNetworkRequest::ContentTypeHeader).toByteArray();
        if (!contentType.isEmpty()) {
            response += QByteArray("Content-Type: ") + contentType + QByteArray("\r\n");
        } else if (!respBody.isEmpty()) {
            response += QByteArray("Content-Type: application/json; charset=UTF-8\r\n");
        }
        response += QByteArray("Content-Length: ") + QByteArray::number(respBody.size())
                    + QByteArray("\r\nConnection: close\r\n\r\n");
        response.append(respBody);
        writeHttpResponseAndClose(client, response);
        reply->deleteLater();
        nam->deleteLater();
    });
}

} // namespace vpnproxy
