#pragma once

#include <QObject>
#include <QString>

class QLocalServer;

namespace vpn {

/**
 * 单实例锁：首个进程监听本地套接字，后续启动则通知已有实例并退出。
 */
class SingleInstance : public QObject {
    Q_OBJECT
public:
    explicit SingleInstance(const QString &serverName, QObject *parent = nullptr);

    /** @return true 表示当前为唯一实例，可继续启动；false 表示已有实例在运行 */
    bool tryRun();

signals:
    void activateRequested();

private:
    void onNewConnection();

    QString m_serverName;
    QLocalServer *m_server = nullptr;
};

} // namespace vpn
