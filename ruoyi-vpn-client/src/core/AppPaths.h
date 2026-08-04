#pragma once

#include <QString>

namespace vpn {

/** 跨平台资源/可写目录：Windows 绿色包用 exe 旁；macOS Bundle 可写目录进 Application Support */
class AppPaths {
public:
    /** 可写根目录（配置、日志、离线登录） */
    static QString writableRoot();
    /** 只读资源根（默认配置模板、i18n、旁路 QML 模块） */
    static QString resourceRoot();

    static QString configFilePath();
    static QString configDefaultTemplatePath();
    static QString logDir();
    static QString offlineLoginDir();
    static QString i18nDir();
    /** QQmlEngine::addImportPath 用 */
    static QString qmlImportPath();

    static bool ensureWritableRoot();
};

} // namespace vpn
