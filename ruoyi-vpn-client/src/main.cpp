#include <QGuiApplication>
#include <QIcon>
#include <QQuickStyle>
#include <QQuickWindow>
#include <QSGRendererInterface>
#include <QQmlApplicationEngine>
#include <QQmlContext>
#include <QCoreApplication>
#include <QUrl>
#include <QWindow>
#ifdef Q_OS_WIN
#include <windows.h>
#endif
#include "core/AppLogger.h"
#include "core/VpnCloudService.h"
#include "core/ControllerService.h"
#include "core/SessionManager.h"
#include "core/SecureStorage.h"
#include "core/VpnFlowController.h"

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

} // namespace

int main(int argc, char *argv[])
{
    if (qEnvironmentVariableIsEmpty("GENLOT_USE_GPU")) {
        QQuickWindow::setGraphicsApi(QSGRendererInterface::Software);
    }
    QQuickStyle::setStyle(QStringLiteral("Fusion"));
    QCoreApplication::setOrganizationName(QStringLiteral("Genlot"));
    QCoreApplication::setApplicationName(QStringLiteral("GenlotVPN"));
    QGuiApplication app(argc, argv);
    qInstallMessageHandler(qtMessageHandler);

    vpn::AppLogger *appLogger = vpn::AppLogger::instance();
    appLogger->info(QStringLiteral("Genlot VPN 客户端启动"));
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
    // 与 Qt Creator 一致：从 exe 同目录加载 GenlotVPN/qmldir（打包脚本会复制该目录）
    engine.addImportPath(QCoreApplication::applicationDirPath());
    engine.rootContext()->setContextProperty(QStringLiteral("vpnFlow"), &flow);
    engine.rootContext()->setContextProperty(QStringLiteral("vpnStorage"), &secureStorage);
    engine.load(QUrl(QStringLiteral("qrc:/GenlotVPN/qml/main.qml")));
    if (engine.rootObjects().isEmpty()) {
        const QString err = QStringLiteral("界面加载失败，请确认安装目录下存在 GenlotVPN 文件夹。\n"
                                           "详细日志见 logs 目录。");
        appLogger->error(QStringLiteral("[启动] %1").arg(err));
        showStartupError(err);
        return -1;
    }
    if (auto *window = qobject_cast<QWindow *>(engine.rootObjects().value(0))) {
        window->setFlags(window->flags() & ~Qt::WindowMaximizeButtonHint);
        if (!appIcon.isNull()) {
            window->setIcon(appIcon);
        }
    }
    return app.exec();
}
