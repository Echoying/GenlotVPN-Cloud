#pragma once

#include <QObject>
#include <QString>

class QLocalServer;

namespace vpnproxy {

class SingleInstance : public QObject {
    Q_OBJECT
public:
    explicit SingleInstance(const QString &serverName, QObject *parent = nullptr);
    bool tryRun();

signals:
    void activateRequested();

private:
    void onNewConnection();

    QString m_serverName;
    QLocalServer *m_server = nullptr;
};

} // namespace vpnproxy
