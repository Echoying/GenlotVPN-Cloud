#include "ProxySessionController.h"
#include "AppLogger.h"

#include <QJsonArray>

namespace vpnproxy {

ProxySessionController::ProxySessionController(const AppConfig &config, QObject *parent)
    : QObject(parent)
    , m_config(config)
{
    m_controller = new ControllerHttpClient(config.controllerBaseUrl, this);
    m_proxy = new OpenApiProxyService(this);
    m_sessionLogs = new SessionLogModel(this);
    m_adminEndpoint = QStringLiteral("http://%1:%2").arg(config.adminListenHost).arg(config.adminListenPort);
}

ProxyLogModel *ProxySessionController::proxyLogs() const
{
    return m_proxy ? m_proxy->logModel() : nullptr;
}

QString ProxySessionController::proxyEndpoint() const
{
    return m_proxy ? m_proxy->listenEndpoint() : QString();
}

void ProxySessionController::setSessionState(const QString &state)
{
    if (m_sessionState == state) {
        return;
    }
    m_sessionState = state;
    emit sessionStateChanged();
}

void ProxySessionController::addSessionLog(const QString &type, const QString &message)
{
    if (m_sessionLogs) {
        m_sessionLogs->append(type, message);
    }
}

QVariantMap ProxySessionController::jsonToLine(const QJsonObject &lineObj)
{
    QVariantMap line;
    line[QStringLiteral("host")] = lineObj.value(QStringLiteral("host")).toString();
    line[QStringLiteral("srvPort")] = lineObj.value(QStringLiteral("srvPort")).toString();
    line[QStringLiteral("spaPort")] = lineObj.value(QStringLiteral("spaPort")).toString();
    line[QStringLiteral("spaKey")] = lineObj.value(QStringLiteral("spaKey")).toString();
    return line;
}

QStringList ProxySessionController::parseAllowedIps(const QJsonObject &proxyObj,
                                                    const QStringList &defaults)
{
    QStringList ips;
    const QJsonArray arr = proxyObj.value(QStringLiteral("allowedSourceIps")).toArray();
    for (const QJsonValue &v : arr) {
        const QString ip = v.toString().trimmed();
        if (!ip.isEmpty()) {
            ips.append(ip);
        }
    }
    return ips.isEmpty() ? defaults : ips;
}

void ProxySessionController::bootstrap()
{
    const QStringList defaultIps = m_config.proxyAllowedSourceIps;
    if (m_proxy->startListenerOnly(m_config.proxyListenHost, m_config.proxyListenPort, defaultIps)) {
        addSessionLog(QStringLiteral("info"),
                    QStringLiteral("同步代理监听已启动 %1").arg(m_proxy->listenEndpoint()));
        emit endpointsChanged();
    } else {
        addSessionLog(QStringLiteral("error"), QStringLiteral("同步代理监听启动失败"));
    }
    addSessionLog(QStringLiteral("info"),
                  QStringLiteral("管理 API 监听 %1，等待服务端登录请求").arg(m_adminEndpoint));
    setSessionState(QStringLiteral("idle"));
    AppLogger::instance()->info(QStringLiteral("GenlotVPN-Proxy 已启动"));
}

QJsonObject ProxySessionController::handleLogin(const QJsonObject &body)
{
    if (m_busy) {
        addSessionLog(QStringLiteral("warn"), QStringLiteral("收到登录请求，但当前正在处理其他会话"));
        return {{QStringLiteral("code"), 409}, {QStringLiteral("msg"), QStringLiteral("会话忙，请先 logout")}};
    }
    const QString username = body.value(QStringLiteral("username")).toString().trimmed();
    const QString password = body.value(QStringLiteral("password")).toString();
    const QJsonObject lineObj = body.value(QStringLiteral("line")).toObject();
    const QJsonObject proxyObj = body.value(QStringLiteral("proxy")).toObject();
    const QString upstream = proxyObj.value(QStringLiteral("upstreamUrl")).toString().trimmed();

    if (username.isEmpty() || password.isEmpty() || lineObj.isEmpty() || upstream.isEmpty()) {
        addSessionLog(QStringLiteral("error"), QStringLiteral("登录请求参数不完整"));
        return {{QStringLiteral("code"), 400}, {QStringLiteral("msg"), QStringLiteral("缺少 username/password/line/proxy.upstreamUrl")}};
    }

    if (m_sessionState == QStringLiteral("ready")) {
        addSessionLog(QStringLiteral("warn"), QStringLiteral("已有活跃会话，请先 logout"));
        return {{QStringLiteral("code"), 409}, {QStringLiteral("msg"), QStringLiteral("请先 logout")}};
    }

    m_busy = true;
    setSessionState(QStringLiteral("connecting"));
    const QVariantMap line = jsonToLine(lineObj);
    addSessionLog(QStringLiteral("info"),
                  QStringLiteral("收到服务端登录请求 host=%1:%2 user=%3")
                      .arg(line.value(QStringLiteral("host")).toString(),
                           line.value(QStringLiteral("srvPort")).toString(),
                           username));

    const QStringList allowedIps = parseAllowedIps(proxyObj, m_config.proxyAllowedSourceIps);
    m_proxy->updateAllowedSourceIps(allowedIps);
    m_proxy->updateUpstream(upstream);
    m_upstreamUrl = upstream;
    emit upstreamUrlChanged();

    addSessionLog(QStringLiteral("info"), QStringLiteral("开始 30303 detect..."));
    const ControllerHttpClient::ConnectResult result = m_controller->connectLine(line, username, password);
    if (!result.ok) {
        m_proxy->clearUpstream();
        m_upstreamUrl.clear();
        emit upstreamUrlChanged();
        m_tunnelStatus = -1;
        emit tunnelStatusChanged();
        setSessionState(QStringLiteral("error"));
        addSessionLog(QStringLiteral("error"), QStringLiteral("30303 连接失败: %1").arg(result.error));
        m_busy = false;
        return {{QStringLiteral("code"), 500}, {QStringLiteral("msg"), result.error}};
    }

    m_tunnelStatus = result.tunnelStatus;
    emit tunnelStatusChanged();
    m_proxy->setVpnReady(true);
    setSessionState(QStringLiteral("ready"));
    addSessionLog(QStringLiteral("info"),
                  QStringLiteral("30303 连接成功，隧道状态=%1，同步代理已开启 %2")
                      .arg(m_tunnelStatus)
                      .arg(m_proxy->listenEndpoint()));
    m_busy = false;

    QJsonObject data;
    data.insert(QStringLiteral("proxyEndpoint"), m_proxy->listenEndpoint());
    data.insert(QStringLiteral("tunnelStatus"), m_tunnelStatus);
    data.insert(QStringLiteral("upstreamUrl"), m_upstreamUrl);
    return {{QStringLiteral("code"), 200},
            {QStringLiteral("msg"), QStringLiteral("连接成功")},
            {QStringLiteral("data"), data}};
}

QJsonObject ProxySessionController::handleLogout()
{
    addSessionLog(QStringLiteral("info"), QStringLiteral("收到服务端 logout 请求"));
    QString logoutError;
    if (!m_controller->logout(&logoutError)) {
        addSessionLog(QStringLiteral("warn"),
                      QStringLiteral("30303 logout 失败: %1").arg(logoutError));
    } else {
        addSessionLog(QStringLiteral("info"), QStringLiteral("30303 logout 成功"));
    }
    m_proxy->clearUpstream();
    m_upstreamUrl.clear();
    emit upstreamUrlChanged();
    m_tunnelStatus = -1;
    emit tunnelStatusChanged();
    setSessionState(QStringLiteral("idle"));
    m_busy = false;
    addSessionLog(QStringLiteral("info"), QStringLiteral("已退出，等待下次登录请求"));
    return {{QStringLiteral("code"), 200}, {QStringLiteral("msg"), QStringLiteral("已退出，等待下次登录")}};
}

void ProxySessionController::clearSessionLogs()
{
    if (m_sessionLogs) {
        m_sessionLogs->clear();
    }
}

void ProxySessionController::clearProxyLogs()
{
    if (m_proxy) {
        m_proxy->clearLogs();
    }
}

} // namespace vpnproxy
