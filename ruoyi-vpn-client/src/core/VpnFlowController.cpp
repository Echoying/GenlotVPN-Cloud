#include "VpnFlowController.h"
#include <QGuiApplication>
#include <QClipboard>
#include <QTimer>
#include <QRegularExpression>

namespace vpn {

namespace {

bool isSessionExpiredMessage(const QString &msg)
{
    if (msg.contains(QStringLiteral("验证码"))) {
        return false;
    }
    return msg.contains(QStringLiteral("登录状态已过期"))
           || msg.contains(QStringLiteral("登录已过期"))
           || msg.contains(QStringLiteral("凭证已过期"))
           || msg.contains(QStringLiteral("未登录或会话已失效"))
           || msg.contains(QStringLiteral("未登录"))
           || msg.contains(QStringLiteral("会话已失效"))
           || msg.contains(QStringLiteral("会话已过期"));
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
    m_countdownTimer = new QTimer(this);
    m_countdownTimer->setInterval(1000);
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
        } else {
            setStatusMessage(QStringLiteral("共 %1 条线路").arg(lines.size()));
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
        emit navigateTo(QStringLiteral("connect"));
        if (m_autoConnectPending) {
            m_autoConnectPending = false;
            startAutoConnect();
        }
    });
    connect(m_cloud, &VpnCloudService::authorizedLinesReady, this, [this](const QVariantList &lines) {
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
    connect(m_cloud, &VpnCloudService::requestFailed, this, [this](const QString &msg) {
        setLoading(false);
        m_autoConnectStarted = false;
        if (m_changePasswordPending) {
            m_changePasswordPending = false;
            emit toast(msg, true);
            return;
        }
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
        if (m_lineVerifyPending || m_sendLineVerifyPending) {
            m_lineVerifyPending = false;
            m_sendLineVerifyPending = false;
            const QString displayMsg = msg.trimmed().isEmpty()
                                           ? QStringLiteral("验证码校验失败，请重试")
                                           : msg.trimmed();
            setVerifyError(displayMsg);
            addLog(QStringLiteral("error"), displayMsg);
            return;
        }
        if (m_verifyDialogVisible && !isSessionExpiredMessage(msg)) {
            const QString displayMsg = msg.trimmed().isEmpty()
                                           ? QStringLiteral("验证失败，请重试")
                                           : msg.trimmed();
            setVerifyError(displayMsg);
            addLog(QStringLiteral("error"), displayMsg);
            return;
        }
        if (isSessionExpiredMessage(msg)) {
            if (m_loggedIn) {
                finishLogout(false);
                emit toast(QStringLiteral("登录已过期，请重新登录"), true);
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
        addLog(QStringLiteral("error"), msg);
        emit toast(msg, true);
    });

    connect(m_controller, &ControllerService::detectSucceeded, this, [this](const QVariantMap &data) {
        if (data.value(QStringLiteral("available")).toBool()) {
            addLog(QStringLiteral("info"), QStringLiteral("服务器连通性检测成功"));
            m_controller->selectServer(m_verifyLine.isEmpty() ? m_pendingLine : m_verifyLine);
        } else {
            setLoading(false);
            addLog(QStringLiteral("error"), QStringLiteral("服务器不可用"));
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
        m_selectedLine = m_verifyLine.isEmpty() ? m_pendingLine : m_verifyLine;
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
    });
    connect(m_controller, &ControllerService::userInfoReady, this, [this](const QString &name) {
        m_username = name;
        emit usernameChanged();
    });
    connect(m_controller, &ControllerService::gatewayListReady, this, [this](const QVariantList &gws, bool, int) {
        m_gateways = normalizeGateways(gws);
        if (!m_switchingGatewayId.isEmpty()) {
            m_selectedGatewayId = m_switchingGatewayId;
            m_switchingGatewayId.clear();
            emit selectedGatewayIdChanged();
            emit switchingGatewayIdChanged();
        } else if (m_selectedGatewayId.isEmpty() && !gws.isEmpty()) {
            m_selectedGatewayId = gws.first().toMap().value(QStringLiteral("id")).toString();
            emit selectedGatewayIdChanged();
        } else if (!m_selectedGatewayId.isEmpty()) {
            bool found = false;
            for (const QVariant &v : gws) {
                if (v.toMap().value(QStringLiteral("id")).toString() == m_selectedGatewayId) {
                    found = true;
                    break;
                }
            }
            if (!found && !gws.isEmpty()) {
                m_selectedGatewayId = gws.first().toMap().value(QStringLiteral("id")).toString();
                emit selectedGatewayIdChanged();
            }
        }
        if (m_autoGatewayInitPending) {
            m_autoGatewayInitPending = false;
            if (!m_gateways.isEmpty()) {
                const QString firstId = m_gateways.first().toMap().value(QStringLiteral("id")).toString();
                m_selectedGatewayId = firstId;
                emit selectedGatewayIdChanged();
                addLog(QStringLiteral("info"), QStringLiteral("正在自动开启网关..."));
                m_autoTurnOnGatewayPending = true;
                m_controller->switchGateway(firstId);
            }
        } else if (m_autoTurnOnGatewayPending) {
            m_autoTurnOnGatewayPending = false;
            turnOnGateway(true);
        }
        emit gatewaysChanged();
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
        emit appsChanged();
    });
    connect(m_controller, &ControllerService::operationFailed, this, [this](const QString &msg) {
        setLoading(false);
        if (!m_switchingGatewayId.isEmpty()) {
            m_switchingGatewayId.clear();
            emit switchingGatewayIdChanged();
        }
        if (m_autoTurnOnGatewayPending) {
            m_autoTurnOnGatewayPending = false;
        }
        addLog(QStringLiteral("error"), msg);
        emit toast(msg, true);
    });
}

void VpnFlowController::setVerifyDialogVisible(bool visible)
{
    if (visible && !m_verifyDialogVisible) {
        setVerifyError(QString());
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
    setLoading(true);
    setStatusMessage(QStringLiteral("正在连接 %1 ...").arg(m_serverEndpoint));
    m_cloud->fetchPublicLines();
}

void VpnFlowController::selectPublicLine(const QVariantMap &line)
{
    setLoginError(QString());
    m_pendingLine = line;
    m_session->setPendingLine(line);
    emit pendingLineChanged();
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

void VpnFlowController::doLogin(const QString &username, const QString &password, const QString &code, bool rememberMe)
{
    const QString appId = m_pendingLine.value(QStringLiteral("appId")).toString();
    if (appId.isEmpty()) {
        emit toast(QStringLiteral("请先选择线路"), true);
        return;
    }
    setLoginError(QString());
    m_storage->saveRememberedUser(username, password, rememberMe);
    m_username = username;
    emit usernameChanged();
    setLoading(true);
    m_loginPending = true;
    m_autoConnectPending = true;
    m_autoConnectStarted = false;
    m_cloud->login(username, password, appId, code, m_captchaUuid);
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
    addLog(QStringLiteral("info"), QStringLiteral("开始检测线路: %1").arg(line.value(QStringLiteral("appName")).toString()));
    m_controller->detectServer(line);
}

void VpnFlowController::turnOnGateway(bool on)
{
    m_controller->turnOnGateway(on);
}

void VpnFlowController::switchGateway(const QString &gatewayId)
{
    const QString id = gatewayId.trimmed();
    if (id.isEmpty() || id == m_selectedGatewayId || !m_switchingGatewayId.isEmpty()) {
        return;
    }
    m_switchingGatewayId = id;
    emit switchingGatewayIdChanged();
    m_controller->switchGateway(id);
}

void VpnFlowController::refreshAppList()
{
    m_controller->fetchAppList();
    m_controller->fetchGatewayList();
}

void VpnFlowController::doLogout()
{
    finishLogout(true);
    m_controller->controllerLogout();
    m_cloud->logout();
}

void VpnFlowController::finishLogout(bool clearUsername)
{
    setVerifyDialogVisible(false);
    setLoggedIn(false);
    m_loginPending = false;
    m_lineVerifyPending = false;
    m_sendLineVerifyPending = false;
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
    m_logs.clear();
    m_selectedGatewayId.clear();
    m_switchingGatewayId.clear();
    m_autoGatewayInitPending = false;
    m_autoTurnOnGatewayPending = false;
    m_autoConnectStarted = false;
    m_autoConnectPending = false;

    if (clearUsername) {
        m_username.clear();
    }

    emit pendingLineChanged();
    emit selectedLineChanged();
    emit gatewaysChanged();
    emit appsChanged();
    emit logsChanged();
    emit usernameChanged();
    emit selectedGatewayIdChanged();
    emit switchingGatewayIdChanged();

    const bool hasLine = !m_pendingLine.value(QStringLiteral("appId")).toString().isEmpty();
    if (hasLine) {
        emit navigateTo(QStringLiteral("login"));
        m_cloud->fetchCaptcha();
    } else {
        emit navigateTo(QStringLiteral("choose"));
    }
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
    const QString appId = m_pendingLine.value(QStringLiteral("appId")).toString();
    if (appId.isEmpty()) {
        emit toast(QStringLiteral("请先选择线路"), true);
        return;
    }
    m_changePasswordPending = true;
    setLoading(true);
    m_cloud->changePassword(user, oldPwd, newPwd, appId);
}

void VpnFlowController::goChooseLine()
{
    emit navigateTo(QStringLiteral("choose"));
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
    QVariantMap entry;
    entry[QStringLiteral("type")] = type;
    entry[QStringLiteral("time")] = QDateTime::currentDateTime().toString(QStringLiteral("HH:mm:ss"));
    entry[QStringLiteral("message")] = message;
    m_logs.append(entry);
    emit logsChanged();
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
    m_sendCountdown = seconds;
    emit sendCountdownChanged();
    m_countdownTimer->start();
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

void VpnFlowController::setLoggedIn(bool v)
{
    if (m_loggedIn != v) {
        m_loggedIn = v;
        emit loggedInChanged();
    }
}

} // namespace vpn
