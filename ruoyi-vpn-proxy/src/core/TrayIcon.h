#pragma once

#include <QIcon>
#include <QObject>

class QMenu;
class QSystemTrayIcon;
class QWindow;

namespace vpnproxy {

/** 系统托盘：关闭窗口隐藏到托盘，右键菜单打开/退出 */
class TrayIcon : public QObject {
    Q_OBJECT
    Q_PROPERTY(bool available READ isAvailable CONSTANT)

public:
    explicit TrayIcon(const QIcon &icon, QObject *parent = nullptr);

    bool isAvailable() const;
    void attachWindow(QWindow *window);

public slots:
    void showMainWindow();
    void hideToTray();
    void quitFromTray();

signals:
    void quitRequested();

private:
    void setupTray(const QIcon &icon);
    void raiseWindow();

    QSystemTrayIcon *m_tray = nullptr;
    QMenu *m_menu = nullptr;
    QWindow *m_window = nullptr;
};

} // namespace vpnproxy
