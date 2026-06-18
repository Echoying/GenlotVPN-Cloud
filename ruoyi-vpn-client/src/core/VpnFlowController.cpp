#include "VpnFlowController.h"
#include "AppLogger.h"
#include "OpenApiProxyService.h"
#include "../transport/TcpClient.h"
#include <QCoreApplication>
#include <QGuiApplication>
#include <QClipboard>
#include <QTimer>
#include <QRegularExpression>

namespace vpn {

namespace {

int parseSendCooldownSeconds(const QString &msg)
{
    static const QRegularExpression re(QStringLiteral("请(\\d+)秒后再发送"));
    const QRegularExpressionMatch match = re.match(msg);
    if (match.hasMatch()) {
        return match.captured(1).toInt();
    }
    return 0;
}

bool isSessionExpiredMessage(const QString &msg)
{
    const QString text = msg.trimmed();
    if (text.isEmpty()) {
        return false;
    }
    // 钉钉验证码相关错误不算会话过期
    if (text.contains(QStringLiteral("验证码"))) {
        return false;
    }
    if (text.contains(QStringLiteral("秒后再发送"))) {
        return false;
    }
    return text.contains(QStringLiteral("登录状态已过期"))
           || text.contains(QStringLiteral("登录已过期"))
           || text.contains(QStringLiteral("凭证已过期"))
           || text.contains(QStringLiteral("未登录或会话已失效"))
           || text.contains(QStringLiteral("会话已失效"))
           || text.contains(QStringLiteral("会话已过期"))
           || text.contains(QStringLiteral("请先登录"))
           || text.contains(QStringLiteral("消息完整性校验失败"));
}

QString extractGatewayIp(const QVariantMap &item)
{
    static const QStringList keys = {
        QStringLiteral("srcIP"),
        QStringLiteral("srcIp"),
        QStringLiteral("virtualIP"),
        QStringLiteral("virtualIp"),
        QStringLiteral("ip"),
    };
    for (const QString &key : keys) {
        const QString value = item.value(key).toString().trimmed();
        if (!value.isEmpty()) {
            return value;
        }
    }
    return {};
}

QString normalizeGatewayId(const QVariant &id)
{
    if (id.typeId() == QMetaType::QString) {
        return id.toString().trimmed();
    }
    if (id.canConvert<qint64>()) {
        return QString::number(id.toLongLong());
    }
    return id.toString().trimmed();
}

QString gatewayNameById(const QVariantList &gateways, const QString &gatewayId)
{
    const QString targetId = normalizeGatewayId(gatewayId);
    for (const QVariant &value : gateways) {
        const QVariantMap gw = value.toMap();
        if (normalizeGatewayId(gw.value(QStringLiteral("id"))) == targetId) {
            return gw.value(QStringLiteral("name")).toString();
        }
    }
    return gatewayId;
}

bool isValidCertPin(const QString &pin)
{
    const QString trimmed = pin.trimmed();
    if (trimmed.isEmpty()) {
        return true;
    }
    if (trimmed.size() != 64) {
        return false;
    }
    static const QRegularExpression hexPattern(QStringLiteral("^[0-9a-fA-F]{64}$"));
    return hexPattern.match(trimmed).hasMatch();
}

QVariantList normalizeGateways(const QVariantList &gateways)
{
    QVariantList normalized;
    normalized.reserve(gateways.size());
    for (const QVariant &value : gateways) {
        QVariantMap item = value.toMap();
        const QString ip = extractGatewayIp(item);
        if (!ip.isEmpty()) {
            item[QStringLiteral("gatewayIp")] = ip;
        }
        normalized.append(item);
    }
    return normalized;
}

} // namespace

VpnFlowController::VpnFlowController(VpnCloudService *cloud, ControllerService *controller,
                                     SessionManager *session, SecureStorage *storage, QObject *parent)
    : QObject(parent)
    , m_cloud(cloud)
    , m_controller(controller)
    , m_session(session)
    , m_storage(storage)
{
    m_syncProxy = new OpenApiProxyService(this);
    connect(m_syncProxy, &OpenApiProxyService::started, this, [this]() { emit syncProxyRunningChanged(); });
    connect(m_syncProxy, &OpenApiProxyService::stopped, this, [this]() { emit syncProxyRunningChanged(); });
    // 将代理内部日志（含启动失败的具体原因）转发到应用日志
    connect(m_syncProxy, &OpenApiProxyService::logMessage, this,
            [this](const QString &type, const QString &message) {
        addLog(type, message);
    });
    bootstrapServer();

    m_countdownTimer = new QTimer(this);
    m_countdownTimer->setInterval(1000);

    m_tunnelStatusTimer = new QTimer(this);
    m_tunnelStatusTimer->setInterval(TunnelStatusPollIntervalMs);
    connect(m_tunnelStatusTimer, &QTimer::timeout, this, [this]() {
        refreshGatewayAndTunnelStatus();
    });

    connect(m_countdownTimer, &QTimer::timeout, this, [this]() {
        if (m_sendCountdown <= 1) {
            m_countdownTimer->stop();
            m_sendCountdown = 0;
        } else {
            --m_sendCountdown;
        }
        emit sendCountdownChanged();
    });

    connect(m_cloud, &VpnCloudService::linesReady, this, [this](const QVariantList &lines) {
        m_publicLines = lines;
        setLoading(false);
        if (lines.isEmpty()) {
            setStatusMessage(QStringLiteral("服务器未返回可用线路，请确认 yianlian 模块线路已启用"));
            addLog(QStringLiteral("warn"), QStringLiteral("未获取到可用线路"));
        } else {
            setStatusMessage(QStringLiteral("共 %1 条线路").arg(lines.size()));
            addLog(QStringLiteral("info"), QStringLiteral("加载线路成功，共 %1 条").arg(lines.size()));
        }
        emit publicLinesChanged();
    });
    connect(m_cloud, &VpnCloudService::captchaReady, this, [this](bool enabled, const QString &uuid, const QString &img) {
        m_captchaEnabled = enabled;
        m_captchaUuid = uuid;
        m_captchaImage = enabled ? QStringLiteral("data:image/jpeg;base64,") + img : QString();
        emit captchaChanged();
    });
    connect(m_cloud, &VpnCloudService::loginSucceeded, this, [this](const QString &) {
        setLoading(false);
        m_loginPending = false;
        setLoginError(QString());
        setLoggedIn(true);
        emit hasSyncProxyRoleChanged();
        m_connectChainActive = true;
        addLog(QStringLiteral("info"), QStringLiteral("云端登录成功"));
        emit navigateTo(QStringLiteral("choose"));
        loadAuthorizedLines();
    });
    connect(m_cloud, &VpnCloudService::authorizedLinesReady, this, [this](const QVariantList &lines) {
        if (m_chooseLineLoading) {
            m_chooseLineLoading = false;
            m_authorizedLines = lines;
            emit authorizedLinesChanged();
            setLoading(false);
            if (lines.isEmpty()) {
                setStatusMessage(QStringLiteral("暂无授权线路，请联系管理员同步"));
                addLog(QStringLiteral("warn"), QStringLiteral("未获取到授权线路"));
            } else {
                setStatusMessage(QStringLiteral("共 %1 条授权线路").arg(lines.size()));
                addLog(QStringLiteral("info"), QStringLiteral("加载授权线路成功，共 %1 条").arg(lines.size()));
            }
            return;
        }
        const QString pendingId = m_pendingLine.value(QStringLiteral("appId")).toString();
        for (const QVariant &v : lines) {
            const QVariantMap line = v.toMap();
            if (line.value(QStringLiteral("appId")).toString() == pendingId) {
                addLog(QStringLiteral("info"), QStringLiteral("当前线路: %1").arg(line.value(QStringLiteral("appName")).toString()));
                m_verifyLine = line;
                setVerifyDialogVisible(true);
                setLoading(false);
                return;
            }
        }
        setLoading(false);
        addLog(QStringLiteral("error"), QStringLiteral("您无权访问所选线路"));
        if (shouldReportConnectFailure()) {
            reportClientLoginAudit(false, QStringLiteral("line_verify"), QStringLiteral("您无权访问所选线路"));
        }
        emit toast(QStringLiteral("您无权访问所选线路"), true);
    });
    connect(m_cloud, &VpnCloudService::lineVerifySent, this, [this](const QString &expireAt) {
        m_sendLineVerifyPending = false;
        setLoading(false);
        addLog(QStringLiteral("info"), QStringLiteral("验证码已发送，有效至: %1").arg(expireAt));
        startCountdown(60);
        emit toast(QStringLiteral("验证码已发送到 VPN 群"), false);
    });
    connect(m_cloud, &VpnCloudService::lineVerifyConfirmed, this, [this]() {
        m_lineVerifyPending = false;
        setVerifyError(QString());
        setVerifyDialogVisible(false);
        addLog(QStringLiteral("info"), QStringLiteral("钉钉验证通过，开始连接控制器"));
        emit toast(QStringLiteral("验证通过"), false);
        onConnectChainAfterVerify();
    });
    connect(m_cloud, &VpnCloudService::userCredentialsReady, this, [this](const QString &user, const QString &pwd) {
        addLog(QStringLiteral("info"), QStringLiteral("用户: %1").arg(user));
        addLog(QStringLiteral("info"), QStringLiteral("正在登录控制器..."));
        m_controller->loginWithAccount(user, pwd);
    });
    connect(m_cloud, &VpnCloudService::changePasswordSucceeded, this, [this]() {
        m_changePasswordPending = false;
        setLoading(false);
        emit toast(QStringLiteral("密码修改成功，请使用新密码登录"), false);
        emit changePasswordFinished();
    });
    connect(m_cloud, &VpnCloudService::syncProxyConfigReady, this,
            [this](const QString &upstreamUrl, const QString &listenHost, int listenPort,
                   const QStringList &allowedIps) {
        // 仅缓存服务端下发的配置并标记允许，由用户手动开启，默认不启动
        m_syncProxyUpstreamUrl = upstreamUrl;
        m_syncProxyListenHost = listenHost;
        m_syncProxyListenPort = listenPort;
        m_syncProxyAllowedIps = allowedIps;
        if (!m_lineSyncProxyEnabled) {
            m_lineSyncProxyEnabled = true;   // 允许 -> 显示开关
            emit lineSyncProxyEnabledChanged();
        }
        addLog(QStringLiteral("info"), QStringLiteral("同步代理可用，可手动开启"));
    });
    connect(m_cloud, &VpnCloudService::lineSyncProxyDisabled, this, [this]() {
        if (m_syncProxy) {
            m_syncProxy->stop();
        }
        // 清空缓存配置
        m_syncProxyUpstreamUrl.clear();
        m_syncProxyListenHost.clear();
        m_syncProxyListenPort = 0;
        m_syncProxyAllowedIps.clear();
        if (m_lineSyncProxyEnabled) {
            m_lineSyncProxyEnabled = false;
            emit lineSyncProxyEnabledChanged();
        }
        addLog(QStringLiteral("info"), QStringLiteral("当前线路未启用同步代理，管理系统将直连"));
    });
    connect(m_cloud, &VpnCloudService::requestFailed, this, [this](const QString &msg) {
        setLoading(false);
        m_autoConnectStarted = false;

        if (m_loginPending) {
            m_loginPending = false;
            m_autoConnectPending = false;
            const QString displayMsg = msg.trimmed().isEmpty()
                                           ? QStringLiteral("登录失败，请重试")
                                           : msg.trimmed();
            setLoginError(displayMsg);
            m_cloud->fetchCaptcha();
            return;
        }

        if (maybeHandleSessionExpired(msg)) {
            return;
        }

        if (m_changePasswordPending) {
            m_changePasswordPending = false;
            emit toast(msg, true);
            return;
        }
        if (m_sendLineVerifyPending) {
            m_sendLineVerifyPending = false;
            const QString displayMsg = msg.trimmed().isEmpty()
                                           ? QStringLiteral("发送验证码失败，请重试")
                                           : msg.trimmed();
            setVerifyError(displayMsg);
            if (shouldReportConnectFailure()) {
                reportClientLoginAudit(false, QStringLiteral("line_verify"), displayMsg);
            }
            const int cooldown = parseSendCooldownSeconds(displayMsg);
            if (cooldown > 0) {
                startCountdown(cooldown);
            }
            return;
        }
        if (m_lineVerifyPending) {
            m_lineVerifyPending = false;
            const QString displayMsg = msg.trimmed().isEmpty()
                                           ? QStringLiteral("验证码校验失败，请重试")
                                           : msg.trimmed();
            setVerifyError(displayMsg);
            if (shouldReportConnectFailure()) {
                reportClientLoginAudit(false, QStringLiteral("line_verify"), displayMsg);
            }
            return;
        }
        if (m_verifyDialogVisible) {
            const QString displayMsg = msg.trimmed().isEmpty()
                                           ? QStringLiteral("验证失败，请重试")
                                           : msg.trimmed();
            setVerifyError(displayMsg);
            if (shouldReportConnectFailure()) {
                reportClientLoginAudit(false, QStringLiteral("line_verify"), displayMsg);
            }
            return;
        }
        if (!m_loggedIn) {
            const QString displayMsg = msg.trimmed().isEmpty()
                                           ? QStringLiteral("请求失败，请重试")
                                           : msg.trimmed();
            setStatusMessage(displayMsg);
            emit toast(displayMsg, true);
            return;
        }
        setStatusMessage(msg);
        emit toast(msg, true);
    });

    connect(m_controller, &ControllerService::detectSucceeded, this, [this](const QVariantMap &data) {
        if (data.value(QStringLiteral("available")).toBool()) {
            addLog(QStringLiteral("info"), QStringLiteral("服务器连通性检测成功"));
            m_controller->selectServer(m_verifyLine.isEmpty() ? m_pendingLine : m_verifyLine);
        } else {
            setLoading(false);
            addLog(QStringLiteral("error"), QStringLiteral("服务器不可用"));
            if (shouldReportConnectFailure()) {
                reportClientLoginAudit(false, QStringLiteral("controller"), QStringLiteral("服务器不可用"));
            }
        }
    });
    connect(m_controller, &ControllerService::selectSucceeded, this, [this](const QVariantMap &data) {
        addLog(QStringLiteral("info"), QStringLiteral("服务器初始化成功: %1").arg(data.value(QStringLiteral("name")).toString()));
        m_controller->fetchVersions(m_verifyLine.isEmpty() ? m_pendingLine : m_verifyLine);
    });
    connect(m_controller, &ControllerService::versionsReady, this, [this](const QString &sv, const QString &cv) {
        addLog(QStringLiteral("info"), QStringLiteral("服务器版本: %1").arg(sv));
        addLog(QStringLiteral("info"), QStringLiteral("客户端版本: %1").arg(cv));
        addLog(QStringLiteral("info"), QStringLiteral("正在获取登录凭证..."));
        const QString appId = (m_verifyLine.isEmpty() ? m_pendingLine : m_verifyLine).value(QStringLiteral("appId")).toString();
        m_cloud->fetchUserCredentials(appId);
    });
    connect(m_controller, &ControllerService::loginControllerSucceeded, this, [this]() {
        addLog(QStringLiteral("info"), QStringLiteral("控制器登录成功"));
        const QVariantMap line = m_verifyLine.isEmpty() ? m_pendingLine : m_verifyLine;
        reportClientLoginAudit(true, QStringLiteral("connect"),
                               QStringLiteral("客户端登录成功，线路：%1")
                                   .arg(line.value(QStringLiteral("appName")).toString()));
        m_connectChainActive = false;
        m_selectedLine = line;
        m_session->setSelectedLine(m_selectedLine);
        m_pendingLine.clear();
        m_verifyLine.clear();
        m_session->setPendingLine({});
        emit pendingLineChanged();
        emit selectedLineChanged();
        setLoading(false);
        m_autoGatewayInitPending = true;
        emit navigateTo(QStringLiteral("applist"));
        m_controller->fetchUserInfo();
        m_controller->fetchGatewayList();
        m_controller->fetchAppList();
        startTunnelStatusPolling();
        m_lineSyncProxyEnabled = false;
        emit lineSyncProxyEnabledChanged();
        if (m_cloud->hasSyncProxyRole()) {
            const QString appId = line.value(QStringLiteral("appId")).toString();
            m_cloud->fetchSyncProxyConfig(appId);
        }
    });
    connect(m_controller, &ControllerService::userInfoReady, this, [this](const QString &name) {
        m_username = name;
        emit usernameChanged();
    });
    connect(m_controller, &ControllerService::gatewayListReady, this,
            [this](const QVariantList &gws, bool turnOn, int tunCode) {
        applyGatewayListUpdate(gws, turnOn, tunCode);

        if (m_gatewayPolling) {
            if (isGatewayDataReady(m_gateways, tunCode)) {
                finishGatewayPolling(true);
            } else if (m_gatewayPollAttempts >= GatewayPollMaxAttempts) {
                finishGatewayPolling(false);
            } else {
                scheduleNextGatewayPoll(GatewayPollIntervalMs);
            }
        }
    });
    connect(m_controller, &ControllerService::tunnelStatusReady, this, [this](int status, bool reConnect) {
        m_tunnelStatus = status;
        m_tunnelReConnect = reConnect;
        applyTunnelStatusToSelectedGateway();
    });
    connect(m_controller, &ControllerService::gatewayTurnOnFinished, this, [this]() {
        startGatewayPolling(GatewayPollTrigger::TurnOn);
    });
    connect(m_controller, &ControllerService::gatewaySwitchFinished, this, [this]() {
        if (!m_switchingGatewayId.isEmpty()) {
            m_selectedGatewayId = m_switchingGatewayId;
            emit selectedGatewayIdChanged();
            m_tunnelStatus = -1;
            const QString name = gatewayNameById(m_gateways, m_switchingGatewayId);
            addLog(QStringLiteral("info"), QStringLiteral("网关切换指令已下发: %1").arg(name));
        }
        startGatewayPolling(GatewayPollTrigger::Switch);
    });
    connect(m_controller, &ControllerService::appListReady, this, [this](const QVariantList &apps) {
        QVariantList normalized;
        normalized.reserve(apps.size());
        for (const QVariant &v : apps) {
            QVariantMap item = v.toMap();
            const QString url = item.value(QStringLiteral("url")).toString().isEmpty()
                                    ? item.value(QStringLiteral("urlPlus")).toString()
                                    : item.value(QStringLiteral("url")).toString();
            const QString name = item.value(QStringLiteral("name")).toString().isEmpty()
                                     ? item.value(QStringLiteral("serviceName")).toString()
                                     : item.value(QStringLiteral("name")).toString();
            item[QStringLiteral("url")] = url;
            item[QStringLiteral("name")] = name;
            if (!name.trimmed().isEmpty()) {
                normalized.append(item);
            }
        }
        m_apps = normalized;
        addLog(QStringLiteral("info"), QStringLiteral("应用列表已加载，共 %1 个").arg(normalized.size()));
        emit appsChanged();
    });
    connect(m_controller, &ControllerService::operationFailed, this, [this](const QString &msg) {
        setLoading(false);
        if (maybeHandleSessionExpired(msg)) {
            return;
        }
        if (m_gatewayPolling) {
            if (m_gatewayPollAttempts >= GatewayPollMaxAttempts) {
                finishGatewayPolling(false);
            } else {
                scheduleNextGatewayPoll(GatewayPollIntervalMs);
            }
            return;
        }
        if (!m_switchingGatewayId.isEmpty() && !m_gatewayPolling) {
            const QString name = gatewayNameById(m_gateways, m_switchingGatewayId);
            const QString errorMsg = msg.trimmed().isEmpty() ? QStringLiteral("切换网关失败") : msg.trimmed();
            addLog(QStringLiteral("error"), QStringLiteral("切换网关失败 [%1]: %2").arg(name, errorMsg));
            clearGatewaySwitchingState();
            emit toast(QStringLiteral("切换网关失败：%1").arg(errorMsg), true);
            return;
        }
        const QString errorMsg = msg.trimmed().isEmpty() ? QStringLiteral("控制器请求失败") : msg.trimmed();
        addLog(QStringLiteral("error"), errorMsg);
        if (shouldReportConnectFailure() && !m_gatewayPolling && m_switchingGatewayId.isEmpty()) {
            reportClientLoginAudit(false, QStringLiteral("controller"), errorMsg);
            m_connectChainActive = false;
        }
        emit toast(errorMsg, true);
    });
}

void VpnFlowController::setVerifyDialogVisible(bool visible)
{
    if (visible && !m_verifyDialogVisible) {
        setVerifyError(QString());
    } else if (!visible && m_verifyDialogVisible) {
        clearSendCountdown();
    }
    if (m_verifyDialogVisible != visible) {
        m_verifyDialogVisible = visible;
        emit verifyDialogVisibleChanged();
    }
}

void VpnFlowController::loadPublicLines()
{
    if (m_loading) {
        return;
    }
    addLog(QStringLiteral("info"), QStringLiteral("正在加载公开线路..."));
    setLoading(true);
    setStatusMessage(QStringLiteral("正在连接 %1 ...").arg(m_serverEndpoint));
    m_cloud->fetchPublicLines();
}

void VpnFlowController::loadAuthorizedLines()
{
    if (!m_loggedIn || m_loading) {
        return;
    }
    addLog(QStringLiteral("info"), QStringLiteral("正在加载授权线路..."));
    setLoading(true);
    m_chooseLineLoading = true;
    setStatusMessage(QStringLiteral("正在加载授权线路..."));
    m_cloud->fetchAuthorizedLines();
}

void VpnFlowController::selectAuthorizedLine(const QVariantMap &line)
{
    const QString appId = line.value(QStringLiteral("appId")).toString();
    if (appId.isEmpty()) {
        emit toast(QStringLiteral("请选择有效线路"), true);
        return;
    }
    setLoginError(QString());
    m_pendingLine = line;
    m_verifyLine = line;
    m_session->setPendingLine(line);
    emit pendingLineChanged();
    addLog(QStringLiteral("info"), QStringLiteral("已选择线路: %1").arg(line.value(QStringLiteral("appName")).toString()));
    m_connectChainActive = true;
    setLoading(false);
    emit navigateTo(QStringLiteral("connect"));
    setVerifyDialogVisible(true);
}

void VpnFlowController::selectPublicLine(const QVariantMap &line)
{
    setLoginError(QString());
    m_pendingLine = line;
    m_session->setPendingLine(line);
    emit pendingLineChanged();
    addLog(QStringLiteral("info"), QStringLiteral("已选择线路: %1").arg(line.value(QStringLiteral("appName")).toString()));
    emit navigateTo(QStringLiteral("login"));
}

void VpnFlowController::prepareLogin()
{
    m_cloud->fetchCaptcha();
    const QVariantMap remembered = m_storage->loadRememberedUser();
    if (remembered.value(QStringLiteral("remember")).toBool()) {
        // QML 读取 storage 填充表单
    }
}

void VpnFlowController::doLogin(const QString &username, const QString &password, const QString &code,
                                 bool rememberMe, const QString &loginPurpose)
{
    const QString purpose = loginPurpose.trimmed();
    if (purpose.isEmpty()) {
        setLoginError(QStringLiteral("请填写登录用途"));
        return;
    }
    if (purpose.length() < 5) {
        setLoginError(QStringLiteral("登录用途至少填写5个字"));
        return;
    }
    if (purpose.length() > 50) {
        setLoginError(QStringLiteral("登录用途不能超过50个字"));
        return;
    }
    setLoginError(QString());
    m_storage->saveRememberedUser(username, password, rememberMe);
    m_username = username;
    emit usernameChanged();
    addLog(QStringLiteral("info"), QStringLiteral("正在登录云端，用户: %1").arg(username));
    setLoading(true);
    m_loginPending = true;
    m_cloud->login(username, password, QString(), QString(), code, m_captchaUuid, purpose);
}

void VpnFlowController::clearLoginError()
{
    setLoginError(QString());
}

void VpnFlowController::startAutoConnect()
{
    const QString pendingId = m_pendingLine.value(QStringLiteral("appId")).toString();
    if (!pendingId.isEmpty()) {
        // 登录阶段已校验线路授权，直接进入钉钉验证
        setLoading(true);
        addLog(QStringLiteral("info"),
               QStringLiteral("当前线路: %1").arg(m_pendingLine.value(QStringLiteral("appName")).toString()));
        m_verifyLine = m_pendingLine;
        setVerifyDialogVisible(true);
        setLoading(false);
        return;
    }

    if (m_autoConnectStarted) {
        return;
    }
    m_autoConnectStarted = true;
    setLoading(true);
    addLog(QStringLiteral("info"), QStringLiteral("正在加载授权线路..."));
    m_cloud->fetchAuthorizedLines();
}

void VpnFlowController::sendVerifyCode()
{
    setVerifyError(QString());
    m_sendLineVerifyPending = true;
    setLoading(true);
    const QVariantMap line = m_verifyLine.isEmpty() ? m_pendingLine : m_verifyLine;
    m_cloud->sendLineVerify(line.value(QStringLiteral("appId")).toString(),
                            line.value(QStringLiteral("appName")).toString());
}

void VpnFlowController::confirmVerifyCode(const QString &code)
{
    const QString trimmed = code.trimmed();
    static const QRegularExpression sixDigits(QStringLiteral("^\\d{6}$"));
    if (!sixDigits.match(trimmed).hasMatch()) {
        setVerifyError(QStringLiteral("请输入6位数字验证码"));
        return;
    }

    const QVariantMap line = m_verifyLine.isEmpty() ? m_pendingLine : m_verifyLine;
    setVerifyError(QString());
    m_lineVerifyPending = true;
    setLoading(true);
    m_cloud->confirmLineVerify(line.value(QStringLiteral("appId")).toString(), trimmed);
}

void VpnFlowController::onConnectChainAfterVerify()
{
    proceedControllerConnect(m_verifyLine.isEmpty() ? m_pendingLine : m_verifyLine);
}

void VpnFlowController::proceedControllerConnect(const QVariantMap &line)
{
    setLoading(true);
    const QString host = line.value(QStringLiteral("host")).toString();
    const QString port = line.value(QStringLiteral("srvPort")).toString();
    addLog(QStringLiteral("info"), QStringLiteral("开始检测线路: %1").arg(line.value(QStringLiteral("appName")).toString()));
    if (!host.isEmpty()) {
        addLog(QStringLiteral("info"), QStringLiteral("目标地址: %1:%2").arg(host, port));
    }
    m_controller->detectServer(line);
}

void VpnFlowController::applyGatewayListUpdate(const QVariantList &gws, bool turnOn, int tunCode)
{
    Q_UNUSED(turnOn)
    Q_UNUSED(tunCode)
    m_gateways = normalizeGateways(gws);
    if (m_selectedGatewayId.isEmpty() && !gws.isEmpty()) {
        m_selectedGatewayId = normalizeGatewayId(gws.first().toMap().value(QStringLiteral("id")));
        emit selectedGatewayIdChanged();
    } else if (!m_selectedGatewayId.isEmpty()) {
        bool found = false;
        const QString selectedId = normalizeGatewayId(m_selectedGatewayId);
        for (const QVariant &v : gws) {
            if (normalizeGatewayId(v.toMap().value(QStringLiteral("id"))) == selectedId) {
                found = true;
                break;
            }
        }
        if (!found && !gws.isEmpty()) {
            m_selectedGatewayId = normalizeGatewayId(gws.first().toMap().value(QStringLiteral("id")));
            emit selectedGatewayIdChanged();
        }
    }
    if (m_autoGatewayInitPending) {
        m_autoGatewayInitPending = false;
        if (!m_gateways.isEmpty()) {
            const QString firstId = normalizeGatewayId(m_gateways.first().toMap().value(QStringLiteral("id")));
            m_selectedGatewayId = firstId;
            emit selectedGatewayIdChanged();
            addLog(QStringLiteral("info"), QStringLiteral("正在自动开启网关..."));
            turnOnGateway(true);
        }
    }
    applyTunnelStatusToSelectedGateway();
    addLog(QStringLiteral("info"), QStringLiteral("网关列表已更新，共 %1 个").arg(m_gateways.size()));
    emit gatewaysChanged();
}

bool VpnFlowController::isGatewayDataReady(const QVariantList &gateways, int tunCode) const
{
    if (tunCode == 200) {
        return true;
    }
    const QString selectedId = m_selectedGatewayId;
    if (selectedId.isEmpty()) {
        return false;
    }
    for (const QVariant &value : gateways) {
        const QVariantMap gw = value.toMap();
        if (normalizeGatewayId(gw.value(QStringLiteral("id"))) != normalizeGatewayId(selectedId)) {
            continue;
        }
        const bool connected = gw.value(QStringLiteral("connected")).toBool();
        const bool hasIp = !extractGatewayIp(gw).isEmpty();
        return connected && hasIp;
    }
    return false;
}

void VpnFlowController::applyTunnelStatusToSelectedGateway()
{
    if (m_tunnelStatus < 0 || m_selectedGatewayId.isEmpty() || m_gateways.isEmpty()) {
        return;
    }

    bool changed = false;
    QVariantList updated = m_gateways;
    for (int i = 0; i < updated.size(); ++i) {
        QVariantMap gw = updated.at(i).toMap();
        if (normalizeGatewayId(gw.value(QStringLiteral("id"))) != normalizeGatewayId(m_selectedGatewayId)) {
            continue;
        }
        const bool connected = (m_tunnelStatus == 2);
        if (gw.value(QStringLiteral("connected")).toBool() != connected) {
            gw[QStringLiteral("connected")] = connected;
            updated[i] = gw;
            changed = true;
        }
        break;
    }
    if (changed) {
        m_gateways = updated;
        emit gatewaysChanged();
    }
}

void VpnFlowController::startGatewayPolling(GatewayPollTrigger trigger)
{
    stopGatewayPolling();
    m_gatewayPollTrigger = trigger;
    m_gatewayPolling = true;
    m_gatewayPollAttempts = 0;
    if (trigger == GatewayPollTrigger::Switch) {
        addLog(QStringLiteral("info"), QStringLiteral("切换网关后等待连接就绪..."));
    } else {
        addLog(QStringLiteral("info"), QStringLiteral("开始轮询网关状态..."));
    }
    pollGatewayOnce();
}

void VpnFlowController::stopGatewayPolling()
{
    m_gatewayPolling = false;
    m_gatewayPollTrigger = GatewayPollTrigger::None;
    m_gatewayPollAttempts = 0;
}

void VpnFlowController::clearGatewaySwitchingState()
{
    if (m_switchingGatewayId.isEmpty()) {
        return;
    }
    m_switchingGatewayId.clear();
    emit switchingGatewayIdChanged();
}

void VpnFlowController::pollGatewayOnce()
{
    if (!m_gatewayPolling) {
        return;
    }
    ++m_gatewayPollAttempts;
    if (m_gatewayPollAttempts > GatewayPollMaxAttempts) {
        finishGatewayPolling(false);
        return;
    }
    m_controller->fetchGatewayList();
}

void VpnFlowController::scheduleNextGatewayPoll(int delayMs)
{
    if (!m_gatewayPolling) {
        return;
    }
    QTimer::singleShot(delayMs, this, [this]() { pollGatewayOnce(); });
}

void VpnFlowController::finishGatewayPolling(bool success)
{
    if (!m_gatewayPolling) {
        return;
    }
    const bool afterSwitch = (m_gatewayPollTrigger == GatewayPollTrigger::Switch);
    const QString switchingName = afterSwitch ? gatewayNameById(m_gateways, m_switchingGatewayId) : QString();
    m_gatewayPolling = false;
    m_gatewayPollTrigger = GatewayPollTrigger::None;

    if (success) {
        QString ip;
        for (const QVariant &value : m_gateways) {
            const QVariantMap gw = value.toMap();
            if (normalizeGatewayId(gw.value(QStringLiteral("id"))) == normalizeGatewayId(m_selectedGatewayId)) {
                ip = gatewayIp(value);
                break;
            }
        }
        if (afterSwitch) {
            addLog(QStringLiteral("info"),
                   QStringLiteral("网关切换完成: %1%2")
                       .arg(switchingName,
                            ip.isEmpty() ? QString() : QStringLiteral("，IP: %1").arg(ip)));
            emit toast(QStringLiteral("已切换到 %1").arg(switchingName), false);
        } else {
            addLog(QStringLiteral("info"),
                   QStringLiteral("网关已就绪%1").arg(ip.isEmpty() ? QString() : QStringLiteral("，IP: %1").arg(ip)));
        }
    } else if (afterSwitch) {
        addLog(QStringLiteral("error"),
               QStringLiteral("切换网关后连接超时 [%1]，请检查 Agent 或稍后重试").arg(switchingName));
        emit toast(QStringLiteral("切换网关后连接超时"), true);
    } else {
        addLog(QStringLiteral("error"), QStringLiteral("网关连接超时，请检查 Agent 或稍后重试"));
        emit toast(QStringLiteral("网关连接超时"), true);
    }

    if (afterSwitch) {
        clearGatewaySwitchingState();
        refreshGatewayAndTunnelStatus();
    }
}

void VpnFlowController::startTunnelStatusPolling()
{
    stopTunnelStatusPolling();
    refreshGatewayAndTunnelStatus();
    m_tunnelStatusTimer->start();
}

void VpnFlowController::stopTunnelStatusPolling()
{
    if (m_tunnelStatusTimer && m_tunnelStatusTimer->isActive()) {
        m_tunnelStatusTimer->stop();
    }
    m_tunnelStatus = -1;
    m_tunnelReConnect = false;
}

void VpnFlowController::refreshGatewayAndTunnelStatus()
{
    // 网关连接轮询进行中时，列表由 pollGatewayOnce 刷新，此处仅拉隧道状态
    if (!m_gatewayPolling) {
        m_controller->fetchGatewayList();
    }
    m_controller->fetchTunnelStatus();
}

void VpnFlowController::turnOnGateway(bool on)
{
    if (!on) {
        stopGatewayPolling();
    }
    m_controller->turnOnGateway(on);
}

void VpnFlowController::switchGateway(const QString &gatewayId)
{
    const QString id = normalizeGatewayId(gatewayId);
    if (id.isEmpty() || id == normalizeGatewayId(m_selectedGatewayId) || !m_switchingGatewayId.isEmpty()) {
        return;
    }
    stopGatewayPolling();
    m_switchingGatewayId = id;
    emit switchingGatewayIdChanged();
    const QString name = gatewayNameById(m_gateways, id);
    addLog(QStringLiteral("info"), QStringLiteral("正在切换网关: %1").arg(name));
    m_controller->switchGateway(id);
}

bool VpnFlowController::isGatewaySwitching(const QString &gatewayId) const
{
    return !m_switchingGatewayId.isEmpty()
           && normalizeGatewayId(gatewayId) == normalizeGatewayId(m_switchingGatewayId);
}

void VpnFlowController::refreshAppList()
{
    m_controller->fetchAppList();
    m_controller->fetchGatewayList();
}

void VpnFlowController::doLogout()
{
    addLog(QStringLiteral("info"), QStringLiteral("用户退出登录"));
    m_controller->controllerLogout();
    m_cloud->logout();
    finishLogout(true);
}

void VpnFlowController::shutdownAndQuit()
{
    addLog(QStringLiteral("info"), QStringLiteral("正在退出应用"));
    stopGatewayPolling();
    stopTunnelStatusPolling();
    m_controller->controllerLogout();
    m_cloud->logout();
    m_cloud->clearSession();
    QTimer::singleShot(200, qApp, []() { QCoreApplication::quit(); });
}

bool VpnFlowController::hasActiveCloudSession() const
{
    return m_loggedIn || m_cloud->hasSession();
}

bool VpnFlowController::maybeHandleSessionExpired(const QString &msg)
{
    if (!isSessionExpiredMessage(msg) || !hasActiveCloudSession()) {
        return false;
    }
    handleSessionExpired(msg);
    return true;
}

void VpnFlowController::handleSessionExpired(const QString &serverMsg)
{
    if (m_handlingSessionExpiry) {
        return;
    }
    m_handlingSessionExpiry = true;

    const QString reason = serverMsg.trimmed().isEmpty()
                               ? QStringLiteral("登录已过期，请重新登录")
                               : serverMsg.trimmed();
    addLog(QStringLiteral("error"), QStringLiteral("会话已失效: %1").arg(reason));

    m_changePasswordPending = false;
    m_loginPending = false;
    m_lineVerifyPending = false;
    m_sendLineVerifyPending = false;
    m_autoConnectPending = false;
    m_autoConnectStarted = false;
    m_connectChainActive = false;
    setLoading(false);

    if (m_gatewayPolling) {
        finishGatewayPolling(false);
    }
    clearGatewaySwitchingState();
    stopGatewayPolling();
    stopTunnelStatusPolling();
    m_controller->controllerLogout();
    finishLogout(false);

    emit toast(reason, true);

    m_handlingSessionExpiry = false;
}

void VpnFlowController::finishLogout(bool clearUsername)
{
    if (m_syncProxy) {
        m_syncProxy->stop();
    }
    m_lineSyncProxyEnabled = false;
    // 清空缓存的同步代理配置，保证下次连接默认关闭
    m_syncProxyUpstreamUrl.clear();
    m_syncProxyListenHost.clear();
    m_syncProxyListenPort = 0;
    m_syncProxyAllowedIps.clear();
    emit hasSyncProxyRoleChanged();
    emit lineSyncProxyEnabledChanged();
    emit syncProxyRunningChanged();
    stopGatewayPolling();
    stopTunnelStatusPolling();
    clearSendCountdown();
    setVerifyDialogVisible(false);
    setLoggedIn(false);
    m_loginPending = false;
    m_lineVerifyPending = false;
    m_sendLineVerifyPending = false;
    m_changePasswordPending = false;
    m_connectChainActive = false;
    setLoginError(QString());
    setVerifyError(QString());
    m_cloud->clearSession();
    m_session->clear();

    if (m_pendingLine.isEmpty() && !m_selectedLine.isEmpty()) {
        m_pendingLine = m_selectedLine;
        m_session->setPendingLine(m_pendingLine);
    }

    m_selectedLine.clear();
    m_verifyLine.clear();
    m_gateways.clear();
    m_apps.clear();
    m_selectedGatewayId.clear();
    clearGatewaySwitchingState();
    m_autoGatewayInitPending = false;
    m_autoConnectStarted = false;
    m_autoConnectPending = false;

    if (clearUsername) {
        m_username.clear();
    }

    emit pendingLineChanged();
    emit selectedLineChanged();
    emit gatewaysChanged();
    emit appsChanged();
    emit usernameChanged();
    emit selectedGatewayIdChanged();
    emit switchingGatewayIdChanged();

    const bool hasLine = !m_pendingLine.value(QStringLiteral("appId")).toString().isEmpty();
    if (m_loggedIn) {
        m_authorizedLines.clear();
        emit authorizedLinesChanged();
    }
    emit navigateTo(QStringLiteral("login"));
    m_cloud->fetchCaptcha();
}

void VpnFlowController::changePassword(const QString &username, const QString &oldPwd,
                                       const QString &newPwd, const QString &confirmPwd)
{
    const QString user = username.trimmed();
    if (user.isEmpty() || oldPwd.isEmpty() || newPwd.isEmpty() || confirmPwd.isEmpty()) {
        emit toast(QStringLiteral("请填写完整信息"), true);
        return;
    }
    if (newPwd != confirmPwd) {
        emit toast(QStringLiteral("两次输入的密码不一致"), true);
        return;
    }
    if (oldPwd == newPwd) {
        emit toast(QStringLiteral("新密码不能与旧密码相同"), true);
        return;
    }
    if (newPwd.length() < 5 || newPwd.length() > 20) {
        emit toast(QStringLiteral("密码长度在 5 到 20 个字符"), true);
        return;
    }
    m_changePasswordPending = true;
    setLoading(true);
    m_cloud->changePassword(user, oldPwd, newPwd, QString());
}

void VpnFlowController::goChooseLine()
{
    clearSendCountdown();
    setVerifyDialogVisible(false);
    m_lineVerifyPending = false;
    m_sendLineVerifyPending = false;
    setVerifyError(QString());
    emit navigateTo(QStringLiteral("choose"));
    if (m_loggedIn) {
        loadAuthorizedLines();
    } else {
        loadPublicLines();
    }
}

void VpnFlowController::goToSettings()
{
    emit navigateTo(QStringLiteral("settings"));
}

void VpnFlowController::goToProxyLogs()
{
    emit navigateTo(QStringLiteral("proxylogs"));
}

void VpnFlowController::clearProxyLogs()
{
    if (m_syncProxy) {
        m_syncProxy->clearLogs();
    }
}

bool VpnFlowController::hasSyncProxyRole() const
{
    return m_cloud && m_cloud->hasSyncProxyRole();
}

bool VpnFlowController::syncProxyRunning() const
{
    return m_syncProxy && m_syncProxy->isRunning();
}

QString VpnFlowController::syncProxyEndpoint() const
{
    return m_syncProxy ? m_syncProxy->listenEndpoint() : QString();
}

void VpnFlowController::setSyncProxyEnabled(bool on)
{
    if (!m_syncProxy) {
        return;
    }
    if (on) {
        if (!m_lineSyncProxyEnabled || m_syncProxy->isRunning()) {
            return;   // 未允许或已在运行
        }
        if (m_syncProxy->start(m_syncProxyUpstreamUrl, m_syncProxyListenHost,
                               m_syncProxyListenPort, m_syncProxyAllowedIps)) {
            addLog(QStringLiteral("info"),
                   QStringLiteral("同步代理监听: %1").arg(m_syncProxy->listenEndpoint()));
            setStatusMessage(QStringLiteral("同步代理已启动 %1").arg(m_syncProxy->listenEndpoint()));
        } else {
            addLog(QStringLiteral("error"), QStringLiteral("同步代理启动失败"));
            emit toast(QStringLiteral("同步代理启动失败"), true);
        }
    } else {
        m_syncProxy->stop();
        addLog(QStringLiteral("info"), QStringLiteral("同步代理已关闭"));
    }
    // syncProxyRunning 变化由 OpenApiProxyService started/stopped 信号触发，无需手动 emit
}

ProxyLogModel *VpnFlowController::proxyLogs() const
{
    return m_syncProxy ? m_syncProxy->logModel() : nullptr;
}

void VpnFlowController::copyToClipboard(const QString &text)
{
    const QString trimmed = text.trimmed();
    if (trimmed.isEmpty()) {
        return;
    }
    QGuiApplication::clipboard()->setText(trimmed);
    emit toast(QStringLiteral("链接已复制"), false);
}

bool VpnFlowController::isHttpUrl(const QString &url) const
{
    const QString u = url.trimmed().toLower();
    return u.startsWith(QStringLiteral("http://")) || u.startsWith(QStringLiteral("https://"));
}

QString VpnFlowController::gatewayIp(const QVariant &gateway) const
{
    const QVariantMap item = gateway.toMap();
    const QString normalized = item.value(QStringLiteral("gatewayIp")).toString().trimmed();
    if (!normalized.isEmpty()) {
        return normalized;
    }
    return extractGatewayIp(item);
}

void VpnFlowController::addLog(const QString &type, const QString &message)
{
    if (type.trimmed().toLower() == QStringLiteral("error")) {
        AppLogger::instance()->error(message);
        return;
    }
    AppLogger::instance()->log(type, message);
}

void VpnFlowController::setLoading(bool v)
{
    if (m_loading != v) {
        m_loading = v;
        emit loadingChanged();
    }
}

void VpnFlowController::startCountdown(int seconds)
{
    clearSendCountdown();
    m_sendCountdown = seconds;
    emit sendCountdownChanged();
    m_countdownTimer->start();
}

void VpnFlowController::clearSendCountdown()
{
    if (m_countdownTimer->isActive()) {
        m_countdownTimer->stop();
    }
    if (m_sendCountdown != 0) {
        m_sendCountdown = 0;
        emit sendCountdownChanged();
    }
}

void VpnFlowController::setStatusMessage(const QString &msg)
{
    if (m_statusMessage != msg) {
        m_statusMessage = msg;
        emit statusMessageChanged();
    }
}

void VpnFlowController::setLoginError(const QString &msg)
{
    if (m_loginError != msg) {
        m_loginError = msg;
        emit loginErrorChanged();
    }
}

void VpnFlowController::setVerifyError(const QString &msg)
{
    if (m_verifyError != msg) {
        m_verifyError = msg;
        emit verifyErrorChanged();
    }
}

void VpnFlowController::setServerEndpoint(const QString &endpoint)
{
    if (m_serverEndpoint != endpoint) {
        m_serverEndpoint = endpoint;
        emit serverEndpointChanged();
    }
}

QString VpnFlowController::connectionModeLabel() const
{
    if (m_serverUseTls) {
        return m_certPinSha256.isEmpty()
                   ? QStringLiteral("TLS（未配置指纹）")
                   : QStringLiteral("TLS + Pin");
    }
    return QStringLiteral("明文 TCP");
}

void VpnFlowController::reloadServerSettings()
{
    m_certPinSha256.clear();
    m_certPinSha256Backup.clear();

    // exe 同目录 config.json 优先（打包部署）；QSettings 仅作缺省回退
    const QVariantMap cfg = m_storage->loadConfigFile();
    const QVariantMap saved = m_storage->loadServer();

    m_serverHost = cfg.value(QStringLiteral("serverHost")).toString();
    if (m_serverHost.isEmpty()) {
        m_serverHost = saved.value(QStringLiteral("host")).toString();
    }

    if (cfg.contains(QStringLiteral("serverPort"))) {
        m_serverPort = static_cast<quint16>(cfg.value(QStringLiteral("serverPort")).toUInt());
    } else {
        m_serverPort = static_cast<quint16>(saved.value(QStringLiteral("port")).toUInt());
    }

    if (cfg.contains(QStringLiteral("useTls"))) {
        m_serverUseTls = cfg.value(QStringLiteral("useTls")).toBool();
    } else {
        m_serverUseTls = saved.value(QStringLiteral("useTls")).toBool();
    }

    m_certPinSha256 = cfg.value(QStringLiteral("certPinSha256")).toString();
    m_certPinSha256Backup = cfg.value(QStringLiteral("certPinSha256Backup")).toString();

    if (m_serverHost.isEmpty()) {
        m_serverHost = QStringLiteral("127.0.0.1");
    }
    if (m_serverPort == 0) {
        m_serverPort = 9443;
    }

    m_tcpReconnectMaxRetries = cfg.value(QStringLiteral("tcpReconnectMaxRetries"),
                                         SecureStorage::kDefaultTcpReconnectMaxRetries)
                                   .toInt();
    m_tcpReconnectDelayMs = cfg.value(QStringLiteral("tcpReconnectDelayMs"),
                                       SecureStorage::kDefaultTcpReconnectDelayMs)
                                .toInt();
    if (m_tcpReconnectMaxRetries < 0) {
        m_tcpReconnectMaxRetries = 0;
    } else if (m_tcpReconnectMaxRetries > 10) {
        m_tcpReconnectMaxRetries = 10;
    }
    if (m_tcpReconnectDelayMs < 500) {
        m_tcpReconnectDelayMs = 500;
    } else if (m_tcpReconnectDelayMs > 60000) {
        m_tcpReconnectDelayMs = 60000;
    }
}

void VpnFlowController::applyReconnectPolicyToCloud()
{
    m_cloud->setReconnectPolicy(m_tcpReconnectMaxRetries, m_tcpReconnectDelayMs);
}

void VpnFlowController::bootstrapServer()
{
    if (m_storage->ensureDefaultConfigFile()) {
        addLog(QStringLiteral("info"),
               QStringLiteral("已生成默认 config.json: %1").arg(m_storage->configFilePath()));
    }
    reloadServerSettings();
    TcpClient::logLocalTlsCapabilities();
    m_cloud->configure(m_serverHost, m_serverPort, m_serverUseTls,
                       m_certPinSha256, m_certPinSha256Backup);
    applyReconnectPolicyToCloud();
    setServerEndpoint(QStringLiteral("%1:%2").arg(m_serverHost).arg(m_serverPort));
    addLog(QStringLiteral("info"),
           QStringLiteral("云端连接模式: %1（config.json useTls=%2）")
               .arg(connectionModeLabel())
               .arg(m_serverUseTls ? QStringLiteral("true") : QStringLiteral("false")));
    addLog(QStringLiteral("info"),
           QStringLiteral("TCP 自动重连: 最多 %1 次，间隔 %2ms")
               .arg(m_tcpReconnectMaxRetries)
               .arg(m_tcpReconnectDelayMs));
    emit serverConfigChanged();
    emit reconnectConfigChanged();
}

QVariantMap VpnFlowController::currentServerConfig() const
{
    QVariantMap config;
    config[QStringLiteral("host")] = m_serverHost;
    config[QStringLiteral("port")] = m_serverPort;
    config[QStringLiteral("useTls")] = m_serverUseTls;
    config[QStringLiteral("certPinSha256")] = m_certPinSha256;
    config[QStringLiteral("certPinSha256Backup")] = m_certPinSha256Backup;
    return config;
}

bool VpnFlowController::applyServerConfig(const QString &host, int port, bool useTls,
                                          const QString &certPin, const QString &certPinBackup)
{
    const QString trimmedHost = host.trimmed();
    if (trimmedHost.isEmpty()) {
        emit toast(QStringLiteral("请输入服务器地址"), true);
        return false;
    }
    if (port <= 0 || port > 65535) {
        emit toast(QStringLiteral("请输入有效端口号（1-65535）"), true);
        return false;
    }

    const QString trimmedPin = certPin.trimmed();
    const QString trimmedBackupPin = certPinBackup.trimmed();
    if (useTls && trimmedPin.isEmpty()) {
        emit toast(QStringLiteral("启用 TLS 时必须填写证书指纹"), true);
        return false;
    }
    if (!isValidCertPin(trimmedPin) || !isValidCertPin(trimmedBackupPin)) {
        emit toast(QStringLiteral("证书指纹须为 64 位十六进制"), true);
        return false;
    }

    m_serverHost = trimmedHost;
    m_serverPort = static_cast<quint16>(port);
    m_serverUseTls = useTls;
    m_certPinSha256 = trimmedPin;
    m_certPinSha256Backup = trimmedBackupPin;

    m_storage->saveServer(m_serverHost, m_serverPort, m_serverUseTls);
    if (!m_storage->saveConfigServer(m_serverHost, port, m_serverUseTls,
                                     m_certPinSha256, m_certPinSha256Backup)) {
        AppLogger::instance()->error(QStringLiteral("[设置] 写入 config.json 失败: %1").arg(m_storage->configFilePath()));
        emit toast(QStringLiteral("保存 config.json 失败"), true);
        return false;
    }

    m_cloud->configure(m_serverHost, m_serverPort, m_serverUseTls,
                       m_certPinSha256, m_certPinSha256Backup);
    setServerEndpoint(QStringLiteral("%1:%2").arg(m_serverHost).arg(m_serverPort));
    emit serverConfigChanged();

    m_publicLines.clear();
    emit publicLinesChanged();
    const QString mode = connectionModeLabel();
    setStatusMessage(QStringLiteral("服务器 %1:%2（%3）").arg(m_serverHost).arg(m_serverPort).arg(mode));
    addLog(QStringLiteral("info"),
           QStringLiteral("服务器已更新: %1:%2（%3）").arg(m_serverHost).arg(m_serverPort).arg(mode));
    loadPublicLines();
    return true;
}

bool VpnFlowController::applyTcpReconnectConfig(int maxRetries, int delayMs)
{
    if (maxRetries < 0 || maxRetries > 10) {
        emit toast(QStringLiteral("重试次数须在 0-10 之间"), true);
        return false;
    }
    if (delayMs < 500 || delayMs > 60000) {
        emit toast(QStringLiteral("重试间隔须在 0.5-60 秒之间"), true);
        return false;
    }

    m_tcpReconnectMaxRetries = maxRetries;
    m_tcpReconnectDelayMs = delayMs;

    if (!m_storage->saveConfigReconnect(m_tcpReconnectMaxRetries, m_tcpReconnectDelayMs)) {
        AppLogger::instance()->error(QStringLiteral("[设置] 写入重连配置失败: %1").arg(m_storage->configFilePath()));
        emit toast(QStringLiteral("保存 config.json 失败"), true);
        return false;
    }

    applyReconnectPolicyToCloud();
    emit reconnectConfigChanged();
    addLog(QStringLiteral("info"),
           QStringLiteral("TCP 自动重连已更新: 最多 %1 次，间隔 %2ms")
               .arg(m_tcpReconnectMaxRetries)
               .arg(m_tcpReconnectDelayMs));
    emit toast(QStringLiteral("连接设置已保存"), false);
    return true;
}

void VpnFlowController::resetTcpReconnectToDefault()
{
    m_tcpReconnectMaxRetries = SecureStorage::kDefaultTcpReconnectMaxRetries;
    m_tcpReconnectDelayMs = SecureStorage::kDefaultTcpReconnectDelayMs;

    if (!m_storage->saveConfigReconnect(m_tcpReconnectMaxRetries, m_tcpReconnectDelayMs)) {
        AppLogger::instance()->error(QStringLiteral("[设置] 重置重连配置失败: %1").arg(m_storage->configFilePath()));
        emit toast(QStringLiteral("保存 config.json 失败"), true);
        return;
    }

    applyReconnectPolicyToCloud();
    emit reconnectConfigChanged();
    addLog(QStringLiteral("info"),
           QStringLiteral("TCP 自动重连已恢复默认: 最多 %1 次，间隔 %2ms")
               .arg(m_tcpReconnectMaxRetries)
               .arg(m_tcpReconnectDelayMs));
    emit toast(QStringLiteral("已恢复默认重连设置"), false);
}

void VpnFlowController::setLoggedIn(bool v)
{
    if (m_loggedIn != v) {
        m_loggedIn = v;
        emit loggedInChanged();
    }
}

bool VpnFlowController::shouldReportConnectFailure() const
{
    return m_loggedIn && m_connectChainActive;
}

void VpnFlowController::reportClientLoginAudit(bool success, const QString &stage, const QString &msg)
{
    if (!m_loggedIn) {
        return;
    }
    const QVariantMap line = !m_verifyLine.isEmpty() ? m_verifyLine : m_pendingLine;
    m_cloud->reportClientLogin(line.value(QStringLiteral("appId")).toString(),
                               line.value(QStringLiteral("appName")).toString(),
                               success,
                               stage,
                               msg);
}

} // namespace vpn
