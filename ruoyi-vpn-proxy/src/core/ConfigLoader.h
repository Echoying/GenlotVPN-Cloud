#pragma once

#include <QString>
#include <QStringList>

namespace vpnproxy {

struct AppConfig {
    QString controllerBaseUrl{QStringLiteral("http://127.0.0.1:30303")};
    /** 访问本机 Agent 30303 是否全包 AES 传输（与 VPN 客户端一致，默认 true） */
    bool controllerAesEnabled = true;
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
