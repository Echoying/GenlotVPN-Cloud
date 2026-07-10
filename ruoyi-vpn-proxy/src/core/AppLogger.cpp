#include "AppLogger.h"
#include "util/HttpUtil.h"

#include <QCoreApplication>
#include <QDateTime>
#include <QDir>
#include <QFile>
#include <QTextStream>
#if QT_VERSION >= QT_VERSION_CHECK(6, 0, 0)
#include <QStringConverter>
#endif

namespace vpnproxy {

AppLogger *AppLogger::instance()
{
    static AppLogger logger;
    return &logger;
}

AppLogger::AppLogger()
{
    m_logDirPath = QCoreApplication::applicationDirPath() + QStringLiteral("/logs");
    QDir().mkpath(m_logDirPath);
    ensureLogFiles();
}

void AppLogger::ensureLogFiles()
{
    const QString date = QDateTime::currentDateTime().toString(QStringLiteral("yyyy-MM-dd"));
    m_logFilePath = m_logDirPath + QStringLiteral("/genlot-vpn-proxy-") + date + QStringLiteral(".log");
    m_errorLogFilePath = m_logDirPath + QStringLiteral("/genlot-vpn-proxy-error-") + date + QStringLiteral(".log");
    m_sessionLogFilePath = m_logDirPath + QStringLiteral("/genlot-vpn-proxy-session-") + date + QStringLiteral(".log");
    m_accessLogFilePath = m_logDirPath + QStringLiteral("/genlot-vpn-proxy-access-") + date + QStringLiteral(".log");
}

QString AppLogger::levelLabel(const QString &level)
{
    const QString normalized = level.trimmed().toLower();
    if (normalized == QStringLiteral("error")) {
        return QStringLiteral("ERROR");
    }
    if (normalized == QStringLiteral("warn") || normalized == QStringLiteral("warning")) {
        return QStringLiteral("WARN");
    }
    return QStringLiteral("INFO");
}

bool AppLogger::isErrorLevel(const QString &level)
{
    return level.trimmed().toLower() == QStringLiteral("error");
}

void AppLogger::appendToFile(const QString &path, const QString &line, bool flushImmediately)
{
    if (path.isEmpty()) {
        return;
    }
    QFile file(path);
    if (!file.open(QIODevice::WriteOnly | QIODevice::Append | QIODevice::Text)) {
        return;
    }
    QTextStream out(&file);
#if QT_VERSION >= QT_VERSION_CHECK(6, 0, 0)
    out.setEncoding(QStringConverter::Utf8);
#endif
    out << line << '\n';
    if (flushImmediately) {
        out.flush();
        file.flush();
    }
}

void AppLogger::log(const QString &level, const QString &message)
{
    const QString text = HttpUtil::sanitizeForLog(message.trimmed());
    if (text.isEmpty()) {
        return;
    }
    ensureLogFiles();
    const QString line = QStringLiteral("[%1] [%2] %3")
                             .arg(QDateTime::currentDateTime().toString(QStringLiteral("yyyy-MM-dd HH:mm:ss")),
                                  levelLabel(level), text);
    appendToFile(m_logFilePath, line, isErrorLevel(level));
    if (isErrorLevel(level)) {
        appendToFile(m_errorLogFilePath, line, true);
    }
}

void AppLogger::logSession(const QString &level, const QString &message)
{
    logSession(level, message, QString(), QString(), -1);
}

void AppLogger::logSession(const QString &level, const QString &message, const QString &requestLog,
                           const QString &responseLog, qint64 elapsedMs)
{
    const QString text = HttpUtil::sanitizeForLog(message.trimmed());
    if (text.isEmpty() && requestLog.trimmed().isEmpty() && responseLog.trimmed().isEmpty()) {
        return;
    }
    ensureLogFiles();
    const QString time = QDateTime::currentDateTime().toString(QStringLiteral("yyyy-MM-dd HH:mm:ss"));
    const QString elapsedText = elapsedMs >= 0 ? QStringLiteral("%1ms").arg(elapsedMs) : QStringLiteral("-");
    appendToFile(m_sessionLogFilePath,
                 QStringLiteral("[%1] [%2] %3 | 耗时: %4").arg(time, levelLabel(level), text, elapsedText),
                 isErrorLevel(level));
    if (!requestLog.trimmed().isEmpty()) {
        appendToFile(m_sessionLogFilePath, QStringLiteral("--- request ---"), false);
        appendToFile(m_sessionLogFilePath, HttpUtil::sanitizeForLog(requestLog), false);
    }
    if (!responseLog.trimmed().isEmpty()) {
        appendToFile(m_sessionLogFilePath, QStringLiteral("--- response ---"), false);
        appendToFile(m_sessionLogFilePath, HttpUtil::sanitizeForLog(responseLog), false);
    }
    log(level, QStringLiteral("[会话] %1").arg(text));
}

void AppLogger::logProxyAccess(const QString &summary, const QString &requestLog,
                               const QString &responseLog)
{
    ensureLogFiles();
    const QString time = QDateTime::currentDateTime().toString(QStringLiteral("yyyy-MM-dd HH:mm:ss"));
    appendToFile(m_accessLogFilePath,
                 QStringLiteral("[%1] [ACCESS] %2").arg(time, HttpUtil::sanitizeForLog(summary)),
                 false);
    if (!requestLog.isEmpty()) {
        appendToFile(m_accessLogFilePath, QStringLiteral("--- request ---"), false);
        appendToFile(m_accessLogFilePath, HttpUtil::sanitizeForLog(requestLog), false);
    }
    if (!responseLog.isEmpty()) {
        appendToFile(m_accessLogFilePath, QStringLiteral("--- response ---"), false);
        appendToFile(m_accessLogFilePath, HttpUtil::sanitizeForLog(responseLog), false);
    }
}

} // namespace vpnproxy
