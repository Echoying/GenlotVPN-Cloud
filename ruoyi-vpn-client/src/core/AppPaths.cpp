#include "AppPaths.h"

#include <QCoreApplication>
#include <QDir>
#include <QFileInfo>
#include <QStandardPaths>

namespace vpn {

namespace {

QString joinPath(const QString &base, const QString &leaf)
{
    if (base.isEmpty()) {
        return leaf;
    }
    return QDir(base).filePath(leaf);
}

QString macosResourcesDir()
{
#ifdef Q_OS_MACOS
    // applicationDirPath = .../GenlotVPN.app/Contents/MacOS
    const QDir macOsDir(QCoreApplication::applicationDirPath());
    const QString resources = macOsDir.absoluteFilePath(QStringLiteral("../Resources"));
    if (QFileInfo::exists(resources)) {
        return QFileInfo(resources).absoluteFilePath();
    }
#endif
    return QString();
}

} // namespace

QString AppPaths::writableRoot()
{
#ifdef Q_OS_MACOS
    // AppDataLocation → ~/Library/Application Support/...（勿用 AppConfigLocation/Preferences）
    const QString path = QStandardPaths::writableLocation(QStandardPaths::AppDataLocation);
    if (!path.isEmpty()) {
        return path;
    }
#endif
    return QCoreApplication::applicationDirPath();
}

QString AppPaths::resourceRoot()
{
#ifdef Q_OS_MACOS
    const QString resources = macosResourcesDir();
    if (!resources.isEmpty()) {
        return resources;
    }
#endif
    return QCoreApplication::applicationDirPath();
}

bool AppPaths::ensureWritableRoot()
{
    return QDir().mkpath(writableRoot());
}

QString AppPaths::configFilePath()
{
    ensureWritableRoot();
    return joinPath(writableRoot(), QStringLiteral("config.json"));
}

QString AppPaths::configDefaultTemplatePath()
{
    const QString inResources = joinPath(resourceRoot(), QStringLiteral("config.default.json"));
    if (QFileInfo::exists(inResources)) {
        return inResources;
    }
    return joinPath(QCoreApplication::applicationDirPath(), QStringLiteral("config.default.json"));
}

QString AppPaths::logDir()
{
    ensureWritableRoot();
    return joinPath(writableRoot(), QStringLiteral("logs"));
}

QString AppPaths::offlineLoginDir()
{
    ensureWritableRoot();
    return joinPath(writableRoot(), QStringLiteral("offline-login"));
}

QString AppPaths::i18nDir()
{
    const QString inResources = joinPath(resourceRoot(), QStringLiteral("i18n"));
    // 避免空的 Resources/i18n 目录挡住 MacOS 旁已部署的 .qm
    if (QDir(inResources).exists()
        && !QDir(inResources).entryList(QStringList() << QStringLiteral("*.qm"), QDir::Files).isEmpty()) {
        return inResources;
    }
    return joinPath(QCoreApplication::applicationDirPath(), QStringLiteral("i18n"));
}

QString AppPaths::qmlImportPath()
{
    // 打包后 GenlotVPN/qmldir 与可执行文件同目录（MacOS）或 Resources
    const QString besideExe = QCoreApplication::applicationDirPath();
    if (QDir(joinPath(besideExe, QStringLiteral("GenlotVPN"))).exists()) {
        return besideExe;
    }
    const QString inResources = resourceRoot();
    if (QDir(joinPath(inResources, QStringLiteral("GenlotVPN"))).exists()) {
        return inResources;
    }
    return besideExe;
}

} // namespace vpn
