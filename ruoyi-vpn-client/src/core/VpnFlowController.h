#pragma once

#include <QObject>
#include <QTimer>
#include <QVariantList>
#include <QVariantMap>
#include <QDateTime>
#include "VpnCloudService.h"
#include "ControllerService.h"
#include "SessionManager.h"
#include "SecureStorage.h"
#include "OfflineLoginImporter.h"
#include "TrustedTimeProvider.h"

namespace vpn {

enum class GatewayPollTrigger {
    None,
    TurnOn,
    Switch,
};

/**
 * VPN 五 step 流程编排，供 QML 绑定
 */
class VpnFlowController : public QObject {
    Q_OBJECT
    Q_PROPERTY(QVariantList publicLines READ publicLines NOTIFY publicLinesChanged)
    Q_PROPERTY(QVariantList authorizedLines READ authorizedLines NOTIFY authorizedLinesChanged)
    Q_PROPERTY(QVariantList gateways READ gateways NOTIFY gatewaysChanged)
    Q_PROPERTY(QVariantList apps READ apps NOTIFY appsChanged)
    Q_PROPERTY(QVariantMap pendingLine READ pendingLine NOTIFY pendingLineChanged)
    Q_PROPERTY(QVariantMap selectedLine READ selectedLine NOTIFY selectedLineChanged)
    Q_PROPERTY(QString username READ username NOTIFY usernameChanged)
    Q_PROPERTY(bool loading READ loading NOTIFY loadingChanged)
    Q_PROPERTY(bool captchaEnabled READ captchaEnabled NOTIFY captchaChanged)
    Q_PROPERTY(QString captchaImage READ captchaImage NOTIFY captchaChanged)
    Q_PROPERTY(QString captchaUuid READ captchaUuid NOTIFY captchaChanged)
    Q_PROPERTY(bool verifyDialogVisible READ verifyDialogVisible WRITE setVerifyDialogVisible NOTIFY verifyDialogVisibleChanged)
    Q_PROPERTY(int sendCountdown READ sendCountdown NOTIFY sendCountdownChanged)
    Q_PROPERTY(QString statusMessage READ statusMessage NOTIFY statusMessageChanged)
    Q_PROPERTY(QString serverEndpoint READ serverEndpoint NOTIFY serverEndpointChanged)
    Q_PROPERTY(QString serverHost READ serverHost NOTIFY serverConfigChanged)
    Q_PROPERTY(int serverPort READ serverPort NOTIFY serverConfigChanged)
    Q_PROPERTY(bool serverUseTls READ serverUseTls NOTIFY serverConfigChanged)
    Q_PROPERTY(QString certPinSha256 READ certPinSha256 NOTIFY serverConfigChanged)
    Q_PROPERTY(QString certPinSha256Backup READ certPinSha256Backup NOTIFY serverConfigChanged)
    Q_PROPERTY(QString connectionModeLabel READ connectionModeLabel NOTIFY serverConfigChanged)
    Q_PROPERTY(int tcpReconnectMaxRetries READ tcpReconnectMaxRetries NOTIFY reconnectConfigChanged)
    Q_PROPERTY(int tcpReconnectDelayMs READ tcpReconnectDelayMs NOTIFY reconnectConfigChanged)
    Q_PROPERTY(bool loggedIn READ loggedIn NOTIFY loggedInChanged)
    Q_PROPERTY(bool offlineMode READ offlineMode NOTIFY offlineModeChanged)
    Q_PROPERTY(QVariantList offlineLoginLines READ offlineLoginLines NOTIFY offlineLoginLinesChanged)
    Q_PROPERTY(QString offlineLoginDir READ offlineLoginDir NOTIFY offlineLoginDirChanged)
    Q_PROPERTY(QString loginError READ loginError NOTIFY loginErrorChanged)
    Q_PROPERTY(int loginErrorKind READ loginErrorKind NOTIFY loginErrorChanged)
    Q_PROPERTY(QString verifyError READ verifyError NOTIFY verifyErrorChanged)
    Q_PROPERTY(QString selectedGatewayId READ selectedGatewayId NOTIFY selectedGatewayIdChanged)
    Q_PROPERTY(QString switchingGatewayId READ switchingGatewayId NOTIFY switchingGatewayIdChanged)

public:
    explicit VpnFlowController(VpnCloudService *cloud, ControllerService *controller,
                               SessionManager *session, SecureStorage *storage, QObject *parent = nullptr);

    QVariantList publicLines() const { return m_publicLines; }
    QVariantList authorizedLines() const { return m_authorizedLines; }
    QVariantList gateways() const { return m_gateways; }
    QVariantList apps() const { return m_apps; }
    QVariantMap pendingLine() const { return m_pendingLine; }
    QVariantMap selectedLine() const { return m_selectedLine; }
    QString username() const { return m_username; }
    bool loading() const { return m_loading; }
    bool captchaEnabled() const { return m_captchaEnabled; }
    QString captchaImage() const { return m_captchaImage; }
    QString captchaUuid() const { return m_captchaUuid; }
    bool verifyDialogVisible() const { return m_verifyDialogVisible; }
    int sendCountdown() const { return m_sendCountdown; }
    QString statusMessage() const { return m_statusMessage; }
    QString serverEndpoint() const { return m_serverEndpoint; }
    QString serverHost() const { return m_serverHost; }
    int serverPort() const { return m_serverPort; }
    bool serverUseTls() const { return m_serverUseTls; }
    QString certPinSha256() const { return m_certPinSha256; }
    QString certPinSha256Backup() const { return m_certPinSha256Backup; }
    QString connectionModeLabel() const;
    int tcpReconnectMaxRetries() const { return m_tcpReconnectMaxRetries; }
    int tcpReconnectDelayMs() const { return m_tcpReconnectDelayMs; }
    bool loggedIn() const { return m_loggedIn; }
    bool offlineMode() const { return m_offlineMode; }
    QVariantList offlineLoginLines() const { return m_offlineLoginLines; }
    QString offlineLoginDir() const { return m_offlineLoginDir; }
    QString loginError() const { return m_loginError; }
    int loginErrorKind() const { return m_loginErrorKind; }
    QString verifyError() const { return m_verifyError; }
    QString selectedGatewayId() const { return m_selectedGatewayId; }
    QString switchingGatewayId() const { return m_switchingGatewayId; }

    void setVerifyDialogVisible(bool visible);

    Q_INVOKABLE void loadPublicLines();
    Q_INVOKABLE void loadAuthorizedLines();
    Q_INVOKABLE void selectPublicLine(const QVariantMap &line);
    Q_INVOKABLE void selectAuthorizedLine(const QVariantMap &line);
    Q_INVOKABLE void prepareLogin();
    Q_INVOKABLE void clearLoginError();
    Q_INVOKABLE QString validateLoginPurpose(const QString &loginPurpose) const;
    Q_INVOKABLE void doLogin(const QString &username, const QString &password, const QString &code,
                             bool rememberMe);
    Q_INVOKABLE void goOfflineChooseLine();
    Q_INVOKABLE void loadOfflineLoginFiles();
    Q_INVOKABLE void connectOfflineLine(int index, const QString &localUsername,
                                        const QString &localPassword);
    Q_INVOKABLE void goBackToLogin();
    Q_INVOKABLE void startAutoConnect();
    Q_INVOKABLE void sendVerifyCode(const QString &loginPurpose);
    Q_INVOKABLE void confirmVerifyCode(const QString &code);
    Q_INVOKABLE void turnOnGateway(bool on);
    Q_INVOKABLE void switchGateway(const QString &gatewayId);
    Q_INVOKABLE void refreshAppList();
    Q_INVOKABLE void doLogout();
    Q_INVOKABLE void shutdownAndQuit();
    /** 进程退出前兜底登出（托盘不可用关窗等场景），阻塞至多约 3 秒 */
    void ensureLogoutBeforeProcessExit();
    Q_INVOKABLE void changePassword(const QString &username, const QString &oldPwd,
                                    const QString &newPwd, const QString &confirmPwd);
    Q_INVOKABLE void goChooseLine();
    Q_INVOKABLE void goToSettings();
    Q_INVOKABLE void copyToClipboard(const QString &text);
    /** 读取系统剪贴板文本（供验证码等字段粘贴） */
    Q_INVOKABLE QString clipboardText() const;
    Q_INVOKABLE bool isHttpUrl(const QString &url) const;
    Q_INVOKABLE QString gatewayIp(const QVariant &gateway) const;
    Q_INVOKABLE bool isGatewaySwitching(const QString &gatewayId) const;
    Q_INVOKABLE QVariantMap currentServerConfig() const;
    Q_INVOKABLE bool applyServerConfig(const QString &host, int port, bool useTls,
                                       const QString &certPin, const QString &certPinBackup = QString());
    Q_INVOKABLE bool applyTcpReconnectConfig(int maxRetries, int delayMs);
    Q_INVOKABLE void resetTcpReconnectToDefault();
    void bootstrapServer();
    void setServerEndpoint(const QString &endpoint);

signals:
    void publicLinesChanged();
    void authorizedLinesChanged();
    void gatewaysChanged();
    void appsChanged();
    void pendingLineChanged();
    void selectedLineChanged();
    void usernameChanged();
    void loadingChanged();
    void captchaChanged();
    void verifyDialogVisibleChanged();
    void sendCountdownChanged();
    void statusMessageChanged();
    void serverEndpointChanged();
    void serverConfigChanged();
    void reconnectConfigChanged();
    void loggedInChanged();
    void offlineModeChanged();
    void offlineLoginLinesChanged();
    void offlineLoginDirChanged();
    void loginErrorChanged();
    void verifyErrorChanged();
    void selectedGatewayIdChanged();
    void switchingGatewayIdChanged();
    void navigateTo(const QString &page);
    void toast(const QString &message, bool isError);
    void changePasswordFinished();

private slots:
    void onConnectChainAfterVerify();

private:
    void addLog(const QString &type, const QString &message);
    void setLoading(bool v);
    void startCountdown(int seconds);
    void clearSendCountdown();
    void setStatusMessage(const QString &msg);
    void setLoginError(const QString &msg);
    void setVerifyError(const QString &msg);
    void setLoggedIn(bool v);
    void proceedControllerConnect(const QVariantMap &line);
    void startOfflineConnect(const OfflineLoginPayload &payload);
    void scanOfflineLoginDir(bool showTimeFallbackToast);
    void connectOfflineLineInternal(int index, const QString &localUsername,
                                    const QString &localPassword);
    void reloadServerSettings();
    void applyReconnectPolicyToCloud();
    void performLogoutCleanup(bool clearUsername, bool quitApp);
    void finishLogout(bool clearUsername);
    bool maybeHandleSessionExpired(const QString &msg);
    void handleSessionExpired(const QString &serverMsg);
    bool hasActiveCloudSession() const;
    void applyGatewayListUpdate(const QVariantList &gws, bool turnOn, int tunCode);
    bool isGatewayDataReady(const QVariantList &gateways, int tunCode) const;
    void applyTunnelStatusToSelectedGateway();
    void startGatewayPolling(GatewayPollTrigger trigger = GatewayPollTrigger::TurnOn);
    void stopGatewayPolling();
    void clearGatewaySwitchingState();
    void pollGatewayOnce();
    void scheduleNextGatewayPoll(int delayMs);
    void finishGatewayPolling(bool success);
    void startTunnelStatusPolling();
    void stopTunnelStatusPolling();
    void startSessionPing();
    void runSessionPingOnce();
    void stopSessionPing();
    void refreshGatewayAndTunnelStatus();
    void reportClientLoginAudit(bool success, const QString &stage, const QString &msg);
    bool shouldReportConnectFailure() const;
    void startOfflineExpireWatch();
    void stopOfflineExpireWatch();
    void checkOfflineCredentialExpiry();

    VpnCloudService *m_cloud;
    ControllerService *m_controller;
    SessionManager *m_session;
    SecureStorage *m_storage;
    TrustedTimeProvider *m_timeProvider = nullptr;

    QVariantList m_publicLines;
    QVariantList m_authorizedLines;
    QVariantList m_gateways;
    QVariantList m_apps;
    QVariantMap m_pendingLine;
    QVariantMap m_selectedLine;
    QVariantMap m_verifyLine;
    QString m_username;
    bool m_loading = false;
    bool m_captchaEnabled = true;
    QString m_captchaImage;
    QString m_captchaUuid;
    bool m_verifyDialogVisible = false;
    int m_sendCountdown = 0;
    QString m_statusMessage;
    QString m_serverEndpoint;
    QString m_serverHost;
    quint16 m_serverPort = 9443;
    bool m_serverUseTls = false;
    QString m_certPinSha256;
    QString m_certPinSha256Backup;
    int m_tcpReconnectMaxRetries = SecureStorage::kDefaultTcpReconnectMaxRetries;
    int m_tcpReconnectDelayMs = SecureStorage::kDefaultTcpReconnectDelayMs;
    bool m_loggedIn = false;
    bool m_offlineMode = false;
    QString m_offlineEncryptedPassword;
    QString m_offlineExpireAtText;
    QString m_offlineHmacKey;
    QVariantList m_offlineLoginLines;
    QString m_offlineLoginDir;
    bool m_loginPending = false;
    bool m_lineVerifyPending = false;
    bool m_sendLineVerifyPending = false;
    QString m_loginError;
    int m_loginErrorKind = 0;
    QString m_loginPurpose;
    bool m_awaitingPublicLineLogin = false;
    QString m_verifyError;
    QTimer *m_countdownTimer = nullptr;
    bool m_autoConnectPending = false;
    bool m_chooseLineLoading = false;
    bool m_autoConnectStarted = false;
    bool m_changePasswordPending = false;
    QString m_selectedGatewayId;
    QString m_switchingGatewayId;
    bool m_autoGatewayInitPending = false;
    bool m_handlingSessionExpiry = false;
    bool m_handlingOfflineExpiry = false;
    bool m_connectChainActive = false;
    bool m_logoutCleanupInProgress = false;

    QTimer *m_offlineExpireTimer = nullptr;
    QTimer *m_tunnelStatusTimer = nullptr;
    QTimer *m_sessionPingTimer = nullptr;
    bool m_sessionPingInFlight = false;
    bool m_gatewayPolling = false;
    GatewayPollTrigger m_gatewayPollTrigger = GatewayPollTrigger::None;
    int m_gatewayPollAttempts = 0;
    int m_tunnelStatus = -1;
    bool m_tunnelReConnect = false;

    static constexpr int GatewayPollIntervalMs = 3000;
    static constexpr int GatewayPollMaxAttempts = 20;
    static constexpr int TunnelStatusPollIntervalMs = 10000;
    static constexpr int SessionPingIntervalMs = 30000;
    static constexpr int kOfflineExpireCheckIntervalMs = 5 * 60 * 1000;
};

} // namespace vpn

