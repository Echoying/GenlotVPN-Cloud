#include <QApplication>
#include <QIcon>
#include <QQuickStyle>
#include <QQuickWindow>
#include <QSGRendererInterface>
#include <QQmlApplicationEngine>
#include <QQmlContext>
#include <QQmlError>
#include <QtQml/qqml.h>
#include <QCoreApplication>
#include <QFile>
#include <QUrl>
#include <QWindow>
#ifdef Q_OS_WIN
#include <windows.h>
#endif

#include "core/AdminHttpServer.h"
#include "core/AppLogger.h"
#include "core/ConfigLoader.h"
#include "core/ProxyLogModel.h"
#include "core/ProxySessionController.h"
#include "core/SingleInstance.h"
#include "core/TrayIcon.h"

namespace {

QIcon loadAppIcon()
{
    const QString base = QCoreApplication::applicationDirPath();
    const QStringList candidates = {
        base + QStringLiteral("/GenlotVPNProxy/assets/images/genlot-app.ico"),
        base + QStringLiteral("/GenlotVPNProxy/assets/images/genlot-app-icon-official.png"),
        QStringLiteral("qrc:/GenlotVPNProxy/assets/images/genlot-app-icon-official.png"),
        QStringLiteral("qrc:/GenlotVPNProxy/assets/images/genlot-app.ico"),
    };
    for (const QString &path : candidates) {
        const QIcon icon(path);
        if (!icon.isNull()) {
            return icon;
        }
    }
    return QIcon();
}

void qtMessageHandler(QtMsgType type, const QMessageLogContext &context, const QString &msg)
{
    Q_UNUSED(context)
    auto *logger = vpnproxy::AppLogger::instance();
    switch (type) {
    case QtDebugMsg:
        logger->info(msg);
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
                reinterpret_cast<LPCWSTR>(QStringLiteral("GenlotVPN Proxy").utf16()),
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
                reinterpret_cast<LPCWSTR>(QStringLiteral("GenlotVPN Proxy").utf16()),
                MB_OK | MB_ICONINFORMATION);
#else
    Q_UNUSED(message)
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
    QCoreApplication::setApplicationName(QStringLiteral("GenlotVPN-Proxy"));
    QCoreApplication::setApplicationVersion(GENLOT_APP_VERSION);
    QApplication app(argc, argv);
    qInstallMessageHandler(qtMessageHandler);

    vpnproxy::AppLogger::instance()->info(
        QStringLiteral("GenlotVPN-Proxy 启动 %1 (%2)").arg(GENLOT_APP_VERSION, GENLOT_BUILD_ID));

    vpnproxy::SingleInstance singleInstance(QStringLiteral("GenlotVPN_Proxy_SingleInstance"));
    if (!singleInstance.tryRun()) {
        showAlreadyRunningNotice(QStringLiteral("GenlotVPN Proxy 已在运行中。"));
        return 0;
    }

    QIcon appIcon = loadAppIcon();
    if (!appIcon.isNull()) {
        app.setWindowIcon(appIcon);
    }

    vpnproxy::TrayIcon trayIcon(appIcon);

    const vpnproxy::AppConfig config = vpnproxy::ConfigLoader::load();
    vpnproxy::ProxySessionController sessionController(config);
    vpnproxy::AdminHttpServer adminServer(&sessionController);

    if (!adminServer.start(config.adminListenHost, config.adminListenPort)) {
        showStartupError(QStringLiteral("管理 API 监听失败 %1:%2")
                             .arg(config.adminListenHost)
                             .arg(config.adminListenPort));
        return 1;
    }

    QQmlApplicationEngine engine;
    qmlRegisterUncreatableType<vpnproxy::ProxyLogModel>(
        "GenlotVPNProxy", 1, 0, "ProxyLogModel",
        QStringLiteral("通过 proxySession.proxyLogs 访问"));
    engine.addImportPath(QCoreApplication::applicationDirPath());
    engine.rootContext()->setContextProperty(QStringLiteral("proxySession"), &sessionController);
    engine.rootContext()->setContextProperty(QStringLiteral("vpnTray"), &trayIcon);
    QObject::connect(&engine, &QQmlApplicationEngine::warnings,
                     [](const QList<QQmlError> &warnings) {
                         for (const QQmlError &warning : warnings) {
                             vpnproxy::AppLogger::instance()->warn(
                                 QStringLiteral("[QML] %1").arg(warning.toString()));
                         }
                     });
    const QString localMainQml = QCoreApplication::applicationDirPath()
                                 + QStringLiteral("/GenlotVPNProxy/qml/main.qml");
    if (QFile::exists(localMainQml)) {
        engine.load(QUrl::fromLocalFile(localMainQml));
    } else {
        engine.load(QUrl(QStringLiteral("qrc:/GenlotVPNProxy/qml/main.qml")));
    }

    if (engine.rootObjects().isEmpty()) {
        showStartupError(QStringLiteral("QML 加载失败"));
        return 1;
    }

    QWindow *mainWindow = qobject_cast<QWindow *>(engine.rootObjects().value(0));
    if (mainWindow) {
        trayIcon.attachWindow(mainWindow);
        if (!appIcon.isNull()) {
            mainWindow->setIcon(appIcon);
        }
    }
    QObject::connect(&trayIcon, &vpnproxy::TrayIcon::quitRequested, &app, &QCoreApplication::quit);

    QObject::connect(&singleInstance, &vpnproxy::SingleInstance::activateRequested, &app, [&trayIcon]() {
        trayIcon.showMainWindow();
    });

    sessionController.bootstrap();

    return app.exec();
}
