#include <QGuiApplication>
#include <QIcon>
#include <QQuickStyle>
#include <QQuickWindow>
#include <QSGRendererInterface>
#include <QQmlApplicationEngine>
#include <QQmlContext>
#include <QCoreApplication>
#include <QUrl>
#include <QFile>
#include <QJsonDocument>
#include <QJsonObject>
#include <QWindow>
#include "core/VpnCloudService.h"
#include "core/ControllerService.h"
#include "core/SessionManager.h"
#include "core/SecureStorage.h"
#include "core/VpnFlowController.h"

int main(int argc, char *argv[])
{
    // Intel ?? + Qt Debug ?? D3D/OpenGL ???????? CPU ????
    // ?????????????? GENLOT_USE_GPU=1 ???
    if (qEnvironmentVariableIsEmpty("GENLOT_USE_GPU")) {
        QQuickWindow::setGraphicsApi(QSGRendererInterface::Software);
    }
    QQuickStyle::setStyle(QStringLiteral("Fusion"));
    QGuiApplication app(argc, argv);
    const QIcon appIcon(QStringLiteral(":/GenlotVPN/assets/images/genlot-app-icon-official.png"));
    if (!appIcon.isNull()) {
        app.setWindowIcon(appIcon);
    }

    vpn::VpnCloudService cloudService;
    vpn::ControllerService controllerService;
    vpn::SessionManager sessionManager;
    vpn::SecureStorage secureStorage;

    const QVariantMap saved = secureStorage.loadServer();
    QString host = saved.value(QStringLiteral("host")).toString();
    quint16 port = static_cast<quint16>(saved.value(QStringLiteral("port")).toUInt());
    bool useTls = saved.value(QStringLiteral("useTls")).toBool();

    const QString configPath = QCoreApplication::applicationDirPath() + QStringLiteral("/config.json");
    if (QFile::exists(configPath)) {
        QFile f(configPath);
        if (f.open(QIODevice::ReadOnly)) {
            const QJsonObject cfg = QJsonDocument::fromJson(f.readAll()).object();
            if (cfg.contains(QStringLiteral("serverHost"))) {
                host = cfg.value(QStringLiteral("serverHost")).toString();
            }
            if (cfg.contains(QStringLiteral("serverPort"))) {
                port = static_cast<quint16>(cfg.value(QStringLiteral("serverPort")).toInt());
            }
            if (cfg.contains(QStringLiteral("useTls"))) {
                useTls = cfg.value(QStringLiteral("useTls")).toBool();
            }
        }
    }
    if (host.isEmpty()) {
        host = QStringLiteral("127.0.0.1");
    }
    if (port == 0) {
        port = 9443;
    }

    cloudService.configure(host, port, useTls, {});

    vpn::VpnFlowController flow(&cloudService, &controllerService, &sessionManager, &secureStorage);
    flow.setServerEndpoint(QStringLiteral("%1:%2").arg(host).arg(port));

    QQmlApplicationEngine engine;
    engine.rootContext()->setContextProperty(QStringLiteral("vpnFlow"), &flow);
    engine.rootContext()->setContextProperty(QStringLiteral("vpnStorage"), &secureStorage);
    // main.qml ????????????? GenlotVPN ??????? qrc ??
    engine.load(QUrl(QStringLiteral("qrc:/GenlotVPN/qml/main.qml")));
    if (engine.rootObjects().isEmpty()) {
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
