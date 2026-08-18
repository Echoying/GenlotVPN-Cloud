#include "ClientDeviceInfo.h"
#include <QtGlobal>
#include <QAbstractSocket>
#include <QHostAddress>
#include <QNetworkInterface>
#include <QSysInfo>

namespace vpn {

QString ClientDeviceInfo::localIpv4()
{
    const QList<QNetworkInterface> interfaces = QNetworkInterface::allInterfaces();
    for (const QNetworkInterface &iface : interfaces) {
        const QNetworkInterface::InterfaceFlags flags = iface.flags();
        if (!(flags & QNetworkInterface::IsUp) || !(flags & QNetworkInterface::IsRunning)
            || (flags & QNetworkInterface::IsLoopBack)) {
            continue;
        }
        for (const QNetworkAddressEntry &entry : iface.addressEntries()) {
            const QHostAddress addr = entry.ip();
            if (addr.protocol() == QAbstractSocket::IPv4Protocol && !addr.isLoopback()) {
                return addr.toString();
            }
        }
    }
    return QString();
}

QString ClientDeviceInfo::osDescription()
{
    const QString pretty = QSysInfo::prettyProductName().trimmed();
    if (!pretty.isEmpty()) {
        return pretty;
    }
    return QStringLiteral("%1 %2").arg(QSysInfo::productType(), QSysInfo::productVersion());
}

QString ClientDeviceInfo::macAddress()
{
    const QList<QNetworkInterface> interfaces = QNetworkInterface::allInterfaces();
    for (const QNetworkInterface &iface : interfaces) {
        const QNetworkInterface::InterfaceFlags flags = iface.flags();
        if (!(flags & QNetworkInterface::IsUp) || (flags & QNetworkInterface::IsLoopBack)) {
            continue;
        }
        const QString mac = iface.hardwareAddress().trimmed();
        if (mac.isEmpty() || mac == QStringLiteral("00:00:00:00:00:00")) {
            continue;
        }
        return mac.toUpper();
    }
    return QString();
}

QString ClientDeviceInfo::platformId()
{
#if defined(Q_OS_WIN)
    return QStringLiteral("windows");
#elif defined(Q_OS_MACOS)
    return QStringLiteral("macos");
#else
    return QStringLiteral("unknown");
#endif
}

} // namespace vpn
