#pragma once

#include <QString>
#include <QStringList>

namespace vpnproxy {

struct AppConfig {
    QString controllerBaseUrl{QStringLiteral("http://127.0.0.1:30303")};
    QString proxyListenHost{QStringLiteral("0.0.0.0")};
    int proxyListenPort = 18001;
    QStringList proxyAllowedSourceIps;
    QString adminListenHost{QStringLiteral("0.0.0.0")};
    int adminListenPort = 18080;
};

class ConfigLoader {
public:
    static AppConfig load();
    static bool saveDefaultIfMissing();
};

} // namespace vpnproxy
