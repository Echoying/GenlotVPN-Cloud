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
    appLogger->info(QStringLiteral("Genlot VPN ?????"));
    const QIcon appIcon(QStringLiteral(":/GenlotVPN/assets/images/genlot-app-icon-official.png"));
    if (!appIcon.isNull()) {
        app.setWindowIcon(appIcon);
    }

    vpn::VpnCloudService cloudService;
    vpn::ControllerService controllerService;
    vpn::SessionManager sessionManager;
    vpn::SecureStorage secureStorage;

    vpn::VpnFlowController flow(&cloudService, &controllerService, &sessionManager, &secureStorage);
    appLogger->info(QStringLiteral("?????: %1").arg(flow.serverEndpoint()));

    QQmlApplicationEngine engine;
    engine.rootContext()->setContextProperty(QStringLiteral("vpnFlow"), &flow);
    engine.rootContext()->setContextProperty(QStringLiteral("vpnStorage"), &secureStorage);
    engine.load(QUrl(QStringLiteral("qrc:/GenlotVPN/qml/main.qml")));
    if (engine.rootObjects().isEmpty()) {
        appLogger->error(QStringLiteral("[??] QML ??????"));
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
