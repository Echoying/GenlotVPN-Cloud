#pragma once

#include <QJsonObject>
#include <QObject>
#include <QSettings>
#include <QString>

namespace vpn {

/** 本地配置；记住的密码经 Windows DPAPI 加密后存入 QSettings */
class SecureStorage : public QObject {
    Q_OBJECT
public:
    static constexpr int kDefaultTcpReconnectMaxRetries = 3;
    static constexpr int kDefaultTcpReconnectDelayMs = 1500;

    explicit SecureStorage(QObject *parent = nullptr);

    Q_INVOKABLE void saveServer(const QString &host, quint16 port, bool useTls);
    Q_INVOKABLE QVariantMap loadServer() const;

    Q_INVOKABLE QString configFilePath() const;
    /** 若缺少或无效的 config.json，从 config.default.json 生成；返回 true 表示已写入 */
    Q_INVOKABLE bool ensureDefaultConfigFile();
    Q_INVOKABLE QVariantMap loadConfigFile() const;
    Q_INVOKABLE bool saveConfigServer(const QString &host, int port, bool useTls,
                                      const QString &certPinSha256,
                                      const QString &certPinSha256Backup = QString());
    Q_INVOKABLE bool saveConfigReconnect(int maxRetries, int delayMs);
    Q_INVOKABLE bool saveConfigLocale(const QString &locale);

    Q_INVOKABLE void saveRememberedUser(const QString &username, const QString &password, bool remember);
    Q_INVOKABLE QVariantMap loadRememberedUser() const;

private:
    bool readConfigObject(QJsonObject *out) const;
    bool writeConfigObject(const QJsonObject &cfg);

    void clearRememberedUser();
    QString loadRememberedPassword() const;

    QSettings m_settings;
};

} // namespace vpn
