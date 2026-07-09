#include "ConfigLoader.h"

#include <QCoreApplication>
#include <QFile>
#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>

namespace vpnproxy {

namespace {

QString configPath()
{
    return QCoreApplication::applicationDirPath() + QStringLiteral("/config.json");
}

QString defaultConfigPath()
{
    return QCoreApplication::applicationDirPath() + QStringLiteral("/config.default.json");
}

AppConfig parseConfigObject(const QJsonObject &obj)
{
    AppConfig cfg;
    cfg.controllerBaseUrl = obj.value(QStringLiteral("controllerBaseUrl")).toString(cfg.controllerBaseUrl);
    cfg.proxyListenHost = obj.value(QStringLiteral("proxyListenHost")).toString(cfg.proxyListenHost);
    cfg.proxyListenPort = obj.value(QStringLiteral("proxyListenPort")).toInt(cfg.proxyListenPort);
    cfg.adminListenHost = obj.value(QStringLiteral("adminListenHost")).toString(cfg.adminListenHost);
    cfg.adminListenPort = obj.value(QStringLiteral("adminListenPort")).toInt(cfg.adminListenPort);
    const QJsonArray ips = obj.value(QStringLiteral("proxyAllowedSourceIps")).toArray();
    for (const QJsonValue &v : ips) {
        const QString ip = v.toString().trimmed();
        if (!ip.isEmpty()) {
            cfg.proxyAllowedSourceIps.append(ip);
        }
    }
    return cfg;
}

} // namespace

bool ConfigLoader::saveDefaultIfMissing()
{
    const QString path = configPath();
    if (QFile::exists(path)) {
        return true;
    }
    const QString def = defaultConfigPath();
    if (!QFile::exists(def)) {
        return false;
    }
    return QFile::copy(def, path);
}

AppConfig ConfigLoader::load()
{
    saveDefaultIfMissing();
    QFile file(configPath());
    if (!file.open(QIODevice::ReadOnly)) {
        return AppConfig{};
    }
    const QJsonDocument doc = QJsonDocument::fromJson(file.readAll());
    if (!doc.isObject()) {
        return AppConfig{};
    }
    return parseConfigObject(doc.object());
}

} // namespace vpnproxy
