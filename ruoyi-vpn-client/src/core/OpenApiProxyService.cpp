#include "OpenApiProxyService.h"
#include "AppLogger.h"

#include <QHostAddress>
#include <QHash>
#include <QMap>
#include <QNetworkAccessManager>
#include <QNetworkReply>
#include <QNetworkRequest>
#include <QSslConfiguration>
#include <QTcpSocket>
#include <QUrl>

namespace vpn {

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
    if (idx < 0) {
        return QByteArray();
    }
    return rawRequest.mid(idx + 4);
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
        const QString key = QString::fromUtf8(line.left(colon).trimmed());
        const QString value = QString::fromUtf8(line.mid(colon + 1).trimmed());
        if (!key.isEmpty()) {
            headers.insert(key, value);
        }
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

QString formatRequestLogWithTarget(const QString &targetUrl, const QString &rawRequestLog)
{
    if (targetUrl.trimmed().isEmpty()) {
        return rawRequestLog;
    }
    return QStringLiteral("URL: %1\n\n").arg(targetUrl.trimmed()) + rawRequestLog;
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
    case 201: return QStringLiteral("Created");
    case 204: return QStringLiteral("No Content");
    case 400: return QStringLiteral("Bad Request");
    case 401: return QStringLiteral("Unauthorized");
    case 403: return QStringLiteral("Forbidden");
    case 404: return QStringLiteral("Not Found");
    case 405: return QStringLiteral("Method Not Allowed");
    case 500: return QStringLiteral("Internal Server Error");
    case 502: return QStringLiteral("Bad Gateway");
    case 503: return QStringLiteral("Service Unavailable");
    default:
        return status >= 200 && status < 300 ? QStringLiteral("OK") : QStringLiteral("Error");
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
    const int bodyStart = headerEnd + 4;
    return raw.size() >= bodyStart + contentLength;
}

/** 写满 HTTP 响应后再关闭连接，避免服务端 HttpClient 读到空响应 */
void writeHttpResponseAndClose(QTcpSocket *client, const QByteArray &response)
{
    if (!client) {
        return;
    }
    client->write(response);
    client->flush();

    const auto closeWhenFlushed = [client]() {
        if (client->state() == QAbstractSocket::UnconnectedState) {
            client->deleteLater();
            return;
        }
        QObject::connect(client, &QTcpSocket::disconnected, client, &QObject::deleteLater,
                         Qt::SingleShotConnection);
        client->disconnectFromHost();
    };

    if (client->bytesToWrite() == 0) {
        closeWhenFlushed();
        return;
    }

    auto *done = new bool(false);
    QObject::connect(client, &QTcpSocket::bytesWritten, client,
                     [client, done, closeWhenFlushed](qint64) {
        if (*done || client->bytesToWrite() > 0) {
            return;
        }
        *done = true;
        delete done;
        closeWhenFlushed();
    });
}

} // namespace

OpenApiProxyService::OpenApiProxyService(QObject *parent) : QObject(parent)
{
    m_logModel = new ProxyLogModel(this);
    connect(&m_server, &QTcpServer::newConnection, this, &OpenApiProxyService::onNewConnection);
}

OpenApiProxyService::~OpenApiProxyService()
{
    stop();
}

QString OpenApiProxyService::listenEndpoint() const
{
    if (!m_running) {
        return QString();
    }
    return QStringLiteral("http://%1:%2").arg(m_listenHost).arg(m_listenPort);
}

void OpenApiProxyService::stop()
{
    if (!m_running) {
        return;
    }
    closeServer();
    m_running = false;
    emit logMessage(QStringLiteral("info"), QStringLiteral("同步代理已停止"));
    emit stopped();
}

bool OpenApiProxyService::start(const QString &upstreamUrl, const QString &listenHost, int listenPort,
                                const QStringList &allowedSourceIps)
{
    stop();
    if (upstreamUrl.trimmed().isEmpty() || listenHost.trimmed().isEmpty() || listenPort <= 0) {
        emit logMessage(QStringLiteral("error"), QStringLiteral("同步代理配置无效"));
        return false;
    }
    m_upstreamUrl = upstreamUrl.trimmed();
    m_listenHost = listenHost.trimmed();
    m_listenPort = listenPort;
    m_allowedSourceIps = allowedSourceIps;

    QHostAddress addr(m_listenHost);
    if (addr.isNull()) {
        emit logMessage(QStringLiteral("error"), QStringLiteral("监听地址无效: %1").arg(m_listenHost));
        return false;
    }
    if (!m_server.listen(addr, static_cast<quint16>(m_listenPort))) {
        emit logMessage(QStringLiteral("error"),
                        QStringLiteral("同步代理监听失败 %1:%2 - %3")
                            .arg(m_listenHost)
                            .arg(m_listenPort)
                            .arg(m_server.errorString()));
        return false;
    }
    m_running = true;
    const QString endpoint = listenEndpoint();
    emit logMessage(QStringLiteral("info"),
                    QStringLiteral("同步代理已启动 %1 → %2").arg(endpoint, m_upstreamUrl));
    emit started(endpoint);
    return true;
}

void OpenApiProxyService::clearLogs()
{
    if (m_logModel) {
        m_logModel->clear();
    }
}

void OpenApiProxyService::recordLog(const QString &method, const QString &path, int status,
                                    const QString &peerIp, const QString &requestLog,
                                    const QString &responseLog, bool success)
{
    if (!m_logModel) {
        return;
    }
    ProxyLogEntry entry;
    entry.id = QStringLiteral("%1").arg(++m_logSeq);
    entry.time = QDateTime::currentDateTime();
    entry.method = method;
    entry.path = path;
    entry.status = status;
    entry.peerIp = peerIp;
    entry.requestLog = requestLog;
    entry.responseLog = responseLog;
    entry.success = success;
    m_logModel->appendEntry(entry);
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
                                            const QString &queryString) const
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
    if (!queryString.isEmpty()) {
        target += QLatin1Char('?') + queryString;
    }
    return target;
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
            emit logMessage(QStringLiteral("warn"), QStringLiteral("拒绝非白名单访问: %1").arg(ip));
            const QByteArray deniedResponse = "HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\nConnection: close\r\n\r\n";
            recordLog(QStringLiteral("DENY"), QStringLiteral("-"), 403, ip,
                      QStringLiteral("非白名单来源 IP: %1").arg(ip),
                      QStringLiteral("HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\n\r\n"), false);
            writeHttpResponseAndClose(client, deniedResponse);
            continue;
        }
        connect(client, &QTcpSocket::readyRead, this, &OpenApiProxyService::onClientReadyRead);
        connect(client, &QTcpSocket::disconnected, this, [this, client]() {
            m_clientBuffers.remove(client);
        });
    }
}

void OpenApiProxyService::onClientReadyRead()
{
    auto *client = qobject_cast<QTcpSocket *>(sender());
    if (!client) {
        return;
    }
    m_clientBuffers[client].append(client->readAll());
    const QByteArray &buffer = m_clientBuffers[client];
    if (!hasCompleteHttpRequest(buffer)) {
        return;
    }
    client->disconnect(this);
    const QByteArray rawRequest = buffer;
    m_clientBuffers.remove(client);
    forwardRequest(client, rawRequest);
}

void OpenApiProxyService::forwardRequest(QTcpSocket *client, const QByteArray &rawRequest)
{
    const QString peer = peerIp(client);
    const QString requestLog = truncateForDisplay(rawRequest);

    const int firstLineEnd = rawRequest.indexOf("\r\n");
    if (firstLineEnd < 0) {
        recordLog(QStringLiteral("-"), QStringLiteral("-"), 400, peer, requestLog,
                  QStringLiteral("HTTP/1.1 400 Bad Request\r\n无效请求行"), false);
        client->deleteLater();
        return;
    }
    QString method;
    QString path;
    if (!parseRequestLine(rawRequest.left(firstLineEnd), &method, &path)) {
        recordLog(QStringLiteral("-"), QStringLiteral("-"), 400, peer, requestLog,
                  QStringLiteral("HTTP/1.1 400 Bad Request\r\n无法解析请求行"), false);
        client->deleteLater();
        return;
    }
    if (!path.startsWith(QStringLiteral("/enadmin/api/open/v1"))) {
        const QString responseLog = QStringLiteral("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n");
        recordLog(method, pathAndQuery(path), 404, peer, requestLog, responseLog, false);
        writeHttpResponseAndClose(client, "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n");
        return;
    }

    const QString targetUrl = buildTargetUrl(m_upstreamUrl, pathAndQuery(path), queryString(path));
    if (targetUrl.isEmpty()) {
        const QString responseLog = QStringLiteral("HTTP/1.1 502 Bad Gateway\r\n上游 URL 无效");
        recordLog(method, pathAndQuery(path), 502, peer, requestLog, responseLog, false);
        writeHttpResponseAndClose(client, "HTTP/1.1 502 Bad Gateway\r\nContent-Length: 0\r\nConnection: close\r\n\r\n");
        return;
    }

    const QString fullRequestLog = formatRequestLogWithTarget(targetUrl, requestLog);

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
        const QString responseLog = QStringLiteral("HTTP/1.1 405 Method Not Allowed\r\nContent-Length: 0\r\n\r\n");
        recordLog(method, targetUrl, 405, peer, fullRequestLog, responseLog, false);
        writeHttpResponseAndClose(client, "HTTP/1.1 405 Method Not Allowed\r\nContent-Length: 0\r\nConnection: close\r\n\r\n");
        nam->deleteLater();
        return;
    }

    connect(reply, &QNetworkReply::finished, this,
            [this, client, reply, nam, method, targetUrl, fullRequestLog, peer]() {
        int status = reply->attribute(QNetworkRequest::HttpStatusCodeAttribute).toInt();
        const QByteArray body = reply->readAll();
        QString responseLog;
        bool success = false;
        if (reply->error() != QNetworkReply::NoError) {
            status = 502;
            responseLog = formatHttpResponseLog(status, QByteArray(), reply->errorString());
        } else {
            success = status >= 200 && status < 400;
            responseLog = formatHttpResponseLog(status, body);
        }
        recordLog(method, targetUrl, status > 0 ? status : 502, peer, fullRequestLog, responseLog, success);

        const int httpStatus = status > 0 ? status : 502;
        QByteArray response = QByteArray("HTTP/1.1 ")
                              + QByteArray::number(httpStatus)
                              + QByteArray(" ")
                              + httpStatusReason(httpStatus).toUtf8()
                              + QByteArray("\r\n");
        const QByteArray contentType = reply->header(QNetworkRequest::ContentTypeHeader).toByteArray();
        if (!contentType.isEmpty()) {
            response += QByteArray("Content-Type: ") + contentType + QByteArray("\r\n");
        } else if (!body.isEmpty()) {
            response += QByteArray("Content-Type: application/json; charset=UTF-8\r\n");
        }
        response += QByteArray("Content-Length: ")
                    + QByteArray::number(body.size())
                    + QByteArray("\r\nConnection: close\r\n\r\n");
        response.append(body);
        writeHttpResponseAndClose(client, response);
        emit logMessage(QStringLiteral("info"),
                        QStringLiteral("代理 %1 %2 → %3").arg(method, targetUrl).arg(status));
        reply->deleteLater();
        nam->deleteLater();
    });
}

} // namespace vpn
