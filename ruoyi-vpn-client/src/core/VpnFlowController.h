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

namespace vpn {

/**
 * VPN 五 step 流程编排，供 QML 绑定
 */
class VpnFlowController : public QObject {
    Q_OBJECT
    Q_PROPERTY(QVariantList publicLines READ publicLines NOTIFY publicLinesChanged)
    Q_PROPERTY(QVariantList logs READ logs NOTIFY logsChanged)
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
    Q_PROPERTY(bool loggedIn READ loggedIn NOTIFY loggedInChanged)
    Q_PROPERTY(QString loginError READ loginError NOTIFY loginErrorChanged)
    Q_PROPERTY(QString verifyError READ verifyError NOTIFY verifyErrorChanged)
    Q_PROPERTY(QString selectedGatewayId READ selectedGatewayId NOTIFY selectedGatewayIdChanged)
    Q_PROPERTY(QString switchingGatewayId READ switchingGatewayId NOTIFY switchingGatewayIdChanged)

public:
    explicit VpnFlowController(VpnCloudService *cloud, ControllerService *controller,
                               SessionManager *session, SecureStorage *storage, QObject *parent = nullptr);

    QVariantList publicLines() const { return m_publicLines; }
    QVariantList logs() const { return m_logs; }
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
    bool loggedIn() const { return m_loggedIn; }
    QString loginError() const { return m_loginError; }
    QString verifyError() const { return m_verifyError; }
    QString selectedGatewayId() const { return m_selectedGatewayId; }
    QString switchingGatewayId() const { return m_switchingGatewayId; }

    void setVerifyDialogVisible(bool visible);

    Q_INVOKABLE void loadPublicLines();
    Q_INVOKABLE void selectPublicLine(const QVariantMap &line);
    Q_INVOKABLE void prepareLogin();
    Q_INVOKABLE void clearLoginError();
    Q_INVOKABLE void doLogin(const QString &username, const QString &password, const QString &code, bool rememberMe);
    Q_INVOKABLE void startAutoConnect();
    Q_INVOKABLE void sendVerifyCode();
    Q_INVOKABLE void confirmVerifyCode(const QString &code);
    Q_INVOKABLE void turnOnGateway(bool on);
    Q_INVOKABLE void switchGateway(const QString &gatewayId);
    Q_INVOKABLE void refreshAppList();
    Q_INVOKABLE void doLogout();
    Q_INVOKABLE void changePassword(const QString &username, const QString &oldPwd,
                                    const QString &newPwd, const QString &confirmPwd);
    Q_INVOKABLE void goChooseLine();
    Q_INVOKABLE void copyToClipboard(const QString &text);
    Q_INVOKABLE bool isHttpUrl(const QString &url) const;
    Q_INVOKABLE QString gatewayIp(const QVariant &gateway) const;
    void setServerEndpoint(const QString &endpoint);

signals:
    void publicLinesChanged();
    void logsChanged();
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
    void loggedInChanged();
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
    void setStatusMessage(const QString &msg);
    void setLoginError(const QString &msg);
    void setVerifyError(const QString &msg);
    void setLoggedIn(bool v);
    void proceedControllerConnect(const QVariantMap &line);
    void finishLogout(bool clearUsername);

    VpnCloudService *m_cloud;
    ControllerService *m_controller;
    SessionManager *m_session;
    SecureStorage *m_storage;

    QVariantList m_publicLines;
    QVariantList m_logs;
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
    bool m_loggedIn = false;
    bool m_loginPending = false;
    bool m_lineVerifyPending = false;
    bool m_sendLineVerifyPending = false;
    QString m_loginError;
    QString m_verifyError;
    QTimer *m_countdownTimer = nullptr;
    bool m_autoConnectPending = false;
    bool m_autoConnectStarted = false;
    bool m_changePasswordPending = false;
    QString m_selectedGatewayId;
    QString m_switchingGatewayId;
    bool m_autoGatewayInitPending = false;
    bool m_autoTurnOnGatewayPending = false;
};

} // namespace vpn
