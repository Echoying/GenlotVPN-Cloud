#include <QApplication>
#include <QIcon>
#include <QQuickStyle>
#include <QQuickWindow>
#include <QSGRendererInterface>
#include <QQmlApplicationEngine>
#include <QQmlContext>
#include <QCoreApplication>
#include <QUrl>
#include <QWindow>
#include <QtQml/qqml.h>
#ifdef Q_OS_WIN
#include <windows.h>
#endif
#include "core/AppLogger.h"
#include "core/VpnCloudService.h"
#include "core/ControllerService.h"
#include "core/SessionManager.h"
#include "core/SecureStorage.h"
#include "core/VpnFlowController.h"
#include "core/AppInfo.h"
#include "core/ProxyLogModel.h"
#include "platform/SingleInstance.h"
#include "platform/TrayIcon.h"

namespace {

void qtMessageHandler(QtMsgType type, const QMessageLogContext &context, const QString &msg)
{
    Q_UNUSED(context)
    auto *logger = vpn::AppLogger::instance();
    switch (type) {
    case QtDebugMsg:
        logger->debug(msg);
        break;
    case QtWarningMsg:
        logger->warn(msg);
        break;
    case QtCriticalMsg:
    case QtFatalMsg:
        logger->error(msg);
        break;
    default:
        logger->info(msg);
        break;
    }
}

void showStartupError(const QString &message)
{
#ifdef Q_OS_WIN
    MessageBoxW(nullptr,
                reinterpret_cast<LPCWSTR>(message.utf16()),
                L"Genlot VPN",
                MB_OK | MB_ICONERROR);
#else
    Q_UNUSED(message)
#endif
}

void showAlreadyRunningNotice(const QString &message)
{
#ifdef Q_OS_WIN
    MessageBoxW(nullptr,
                reinterpret_cast<LPCWSTR>(message.utf16()),
                L"Genlot VPN",
                MB_OK | MB_ICONINFORMATION);
#else
    Q_UNUSED(message)
#endif
}

void raiseApplicationWindow(QWindow *window)
{
    if (!window) {
        return;
    }
    if (window->visibility() == QWindow::Minimized) {
        window->showNormal();
    } else {
        window->show();
    }
    window->raise();
    window->requestActivate();
#ifdef Q_OS_WIN
    const HWND hwnd = reinterpret_cast<HWND>(window->winId());
    if (hwnd) {
        if (IsIconic(hwnd)) {
            ShowWindow(hwnd, SW_RESTORE);
        }
        SetForegroundWindow(hwnd);
    }
#endif
}

} // namespace

int main(int argc, char *argv[])
{
    if (qEnvironmentVariableIsEmpty("GENLOT_USE_GPU")) {
        QQuickWindow::setGraphicsApi(QSGRendererInterface::Software);
    }
    QQuickStyle::setStyle(QStringLiteral("Fusion"));
    QCoreApplication::setOrganizationName(QStringLiteral("Genlot"));
    QCoreApplication::setApplicationName(QStringLiteral("GenlotVPN"));
    vpn::AppInfo appInfo;
    QCoreApplication::setApplicationVersion(appInfo.version());
    // 托盘右键菜单使用 QMenu（QWidget），须 QApplication 而非 QGuiApplication
    QApplication app(argc, argv);
    qInstallMessageHandler(qtMessageHandler);

    vpn::AppLogger *appLogger = vpn::AppLogger::instance();
    appLogger->info(QStringLiteral("Genlot VPN 客户端启动 %1").arg(appInfo.versionLabel()));

    vpn::SingleInstance singleInstance(QStringLiteral("GenlotVPN_SingleInstance"));
    if (!singleInstance.tryRun()) {
        showAlreadyRunningNotice(QStringLiteral("Genlot VPN 已在运行中。"));
        return 0;
    }
    const QIcon appIcon(QStringLiteral(":/GenlotVPN/assets/images/genlot-app-icon-official.png"));
    if (!appIcon.isNull()) {
        app.setWindowIcon(appIcon);
    }

    vpn::VpnCloudService cloudService;
    vpn::ControllerService controllerService;
    vpn::SessionManager sessionManager;
    vpn::SecureStorage secureStorage;

    vpn::VpnFlowController flow(&cloudService, &controllerService, &sessionManager, &secureStorage);
    appLogger->info(QStringLiteral("云端地址: %1").arg(flow.serverEndpoint()));

    QQmlApplicationEngine engine;
    qmlRegisterUncreatableType<vpn::ProxyLogModel>(
        "GenlotVPN", 1, 0, "ProxyLogModel",
        QStringLiteral("通过 vpnFlow.proxyLogs 访问"));
    vpn::TrayIcon trayIcon(appIcon);
    // 与 Qt Creator 一致：从 exe 同目录加载 GenlotVPN/qmldir（打包脚本会复制该目录）
    engine.addImportPath(QCoreApplication::applicationDirPath());
    engine.rootContext()->setContextProperty(QStringLiteral("vpnFlow"), &flow);
    engine.rootContext()->setContextProperty(QStringLiteral("vpnStorage"), &secureStorage);
    engine.rootContext()->setContextProperty(QStringLiteral("vpnTray"), &trayIcon);
    engine.rootContext()->setContextProperty(QStringLiteral("vpnApp"), &appInfo);
    engine.load(QUrl(QStringLiteral("qrc:/GenlotVPN/qml/main.qml")));
    if (engine.rootObjects().isEmpty()) {
        const QString err = QStringLiteral("界面加载失败，请确认安装目录下存在 GenlotVPN 文件夹。\n"
                                           "详细日志见 logs 目录。");
        appLogger->error(QStringLiteral("[启动] %1").arg(err));
        showStartupError(err);
        return -1;
    }
    QWindow *mainWindow = qobject_cast<QWindow *>(engine.rootObjects().value(0));
    if (mainWindow) {
        mainWindow->setFlags(mainWindow->flags() & ~Qt::WindowMaximizeButtonHint);
        if (!appIcon.isNull()) {
            mainWindow->setIcon(appIcon);
        }
    }
    trayIcon.attachWindow(mainWindow);
    QObject::connect(&singleInstance, &vpn::SingleInstance::activateRequested, [&]() {
        appLogger->info(QStringLiteral("[单实例] 收到置前请求"));
        trayIcon.showMainWindow();
    });
    QObject::connect(&trayIcon, &vpn::TrayIcon::quitRequested, &flow, &vpn::VpnFlowController::shutdownAndQuit);
    return app.exec();
}
