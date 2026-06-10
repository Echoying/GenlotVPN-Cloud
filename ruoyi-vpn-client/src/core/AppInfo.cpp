#include "AppInfo.h"

namespace vpn {

namespace {

#ifndef GENLOT_APP_VERSION
#define GENLOT_APP_VERSION "1.0.0"
#endif
#ifndef GENLOT_APP_BUILD_ID
#define GENLOT_APP_BUILD_ID "unknown"
#endif

QString macroString(const char *value)
{
    return QString::fromUtf8(value);
}

} // namespace

AppInfo::AppInfo(QObject *parent)
    : QObject(parent)
{
}

QString AppInfo::version() const
{
    return macroString(GENLOT_APP_VERSION);
}

QString AppInfo::buildId() const
{
    return macroString(GENLOT_APP_BUILD_ID);
}

QString AppInfo::versionLabel() const
{
    return QStringLiteral("%1 (%2)").arg(version(), buildId());
}

QString AppInfo::windowTitle() const
{
    return QStringLiteral("Genlot VPN %1").arg(version());
}

} // namespace vpn
