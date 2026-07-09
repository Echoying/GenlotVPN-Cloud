#pragma once

#include <QString>

namespace vpnproxy {

/** 应用日志：写入程序目录 logs/；ERROR 双写 error 专用文件 */
class AppLogger {
public:
    static AppLogger *instance();

    void log(const QString &level, const QString &message);
    void info(const QString &message) { log(QStringLiteral("info"), message); }
    void warn(const QString &message) { log(QStringLiteral("warn"), message); }
    void error(const QString &message) { log(QStringLiteral("error"), message); }

    void logSession(const QString &level, const QString &message);
    void logProxyAccess(const QString &summary, const QString &requestLog,
                        const QString &responseLog);

    QString logDirPath() const { return m_logDirPath; }

private:
    AppLogger();

    void ensureLogFiles();
    void appendToFile(const QString &path, const QString &line, bool flushImmediately);
    static QString levelLabel(const QString &level);
    static bool isErrorLevel(const QString &level);

    QString m_logDirPath;
    QString m_logFilePath;
    QString m_errorLogFilePath;
    QString m_sessionLogFilePath;
    QString m_accessLogFilePath;
};

} // namespace vpnproxy
