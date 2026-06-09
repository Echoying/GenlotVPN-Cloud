#pragma once

#include <QObject>
#include <QSettings>
#include <QString>

namespace vpn {

/** 本地配置与记住密码（Windows 使用 QSettings + 简单存储） */
class SecureStorage : public QObject {
    Q_OBJECT
public:
    explicit SecureStorage(QObject *parent = nullptr);

    Q_INVOKABLE void saveServer(const QString &host, quint16 port, bool useTls);
    Q_INVOKABLE QVariantMap loadServer() const;

    Q_INVOKABLE void saveRememberedUser(const QString &username, const QString &password, bool remember);
    Q_INVOKABLE QVariantMap loadRememberedUser() const;

private:
    QSettings m_settings;
};

} // namespace vpn
