#include "TrayIcon.h"
#include "AppLogger.h"

#include <QAction>
#include <QApplication>
#include <QCoreApplication>
#include <QMenu>
#include <QSystemTrayIcon>
#include <QWindow>
#ifdef Q_OS_WIN
#include <windows.h>
#endif

namespace vpnproxy {

namespace {

QIcon resolveTrayIcon(const QIcon &icon)
{
    if (!icon.isNull()) {
        return icon;
    }
    const QString base = QCoreApplication::applicationDirPath();
    const QStringList candidates = {
        base + QStringLiteral("/GenlotVPNProxy/assets/images/genlot-app.ico"),
        base + QStringLiteral("/GenlotVPNProxy/assets/images/genlot-app-icon-official.png"),
        QStringLiteral("qrc:/GenlotVPNProxy/assets/images/genlot-app-icon-official.png"),
    };
    for (const QString &path : candidates) {
        const QIcon candidate(path);
        if (!candidate.isNull()) {
            return candidate;
        }
    }
    return QIcon();
}

} // namespace

TrayIcon::TrayIcon(const QIcon &icon, QObject *parent)
    : QObject(parent)
{
    if (!QSystemTrayIcon::isSystemTrayAvailable()) {
        AppLogger::instance()->warn(QStringLiteral("[托盘] 系统托盘不可用，关闭窗口将直接退出"));
        return;
    }

    QApplication::setQuitOnLastWindowClosed(false);
    setupTray(icon);
}

bool TrayIcon::isAvailable() const
{
    return m_tray != nullptr;
}

void TrayIcon::attachWindow(QWindow *window)
{
    m_window = window;
}

void TrayIcon::setupTray(const QIcon &icon)
{
    m_tray = new QSystemTrayIcon(this);
    const QIcon trayIcon = resolveTrayIcon(icon);
    m_tray->setIcon(trayIcon.isNull() ? QIcon::fromTheme(QStringLiteral("application")) : trayIcon);
    m_tray->setToolTip(QStringLiteral("GenlotVPN Proxy"));

    m_menu = new QMenu();
    QAction *showAction = m_menu->addAction(QStringLiteral("打开主窗口"));
    QAction *quitAction = m_menu->addAction(QStringLiteral("退出"));
    connect(showAction, &QAction::triggered, this, &TrayIcon::showMainWindow);
    connect(quitAction, &QAction::triggered, this, &TrayIcon::quitFromTray);
    m_tray->setContextMenu(m_menu);

    connect(m_tray, &QSystemTrayIcon::activated, this, [this](QSystemTrayIcon::ActivationReason reason) {
        if (reason == QSystemTrayIcon::Trigger || reason == QSystemTrayIcon::DoubleClick) {
            showMainWindow();
        }
    });

    m_tray->show();
    AppLogger::instance()->info(QStringLiteral("[托盘] 已启用"));
}

void TrayIcon::raiseWindow()
{
    if (!m_window) {
        return;
    }
    if (m_window->visibility() == QWindow::Minimized) {
        m_window->showMaximized();
    } else {
        m_window->show();
        if (m_window->visibility() != QWindow::Maximized) {
            m_window->showMaximized();
        }
    }
    m_window->raise();
    m_window->requestActivate();
#ifdef Q_OS_WIN
    const HWND hwnd = reinterpret_cast<HWND>(m_window->winId());
    if (hwnd) {
        if (IsIconic(hwnd)) {
            ShowWindow(hwnd, SW_RESTORE);
        }
        SetForegroundWindow(hwnd);
    }
#endif
}

void TrayIcon::showMainWindow()
{
    raiseWindow();
}

void TrayIcon::hideToTray()
{
    if (!m_tray) {
        return;
    }
    if (m_window) {
        m_window->hide();
    }
    if (!m_tray->isVisible()) {
        m_tray->show();
    }
    AppLogger::instance()->info(QStringLiteral("[托盘] 窗口已隐藏到托盘"));
}

void TrayIcon::quitFromTray()
{
    AppLogger::instance()->info(QStringLiteral("[托盘] 用户选择退出"));
    emit quitRequested();
}

} // namespace vpnproxy
