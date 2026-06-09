#include "AppLogger.h"
#include <QCoreApplication>
#include <QDateTime>
#include <QDir>
#include <QFile>
#include <QTextStream>
#if QT_VERSION >= QT_VERSION_CHECK(6, 0, 0)
#include <QStringConverter>
#endif

namespace vpn {

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
    m_logFilePath = m_logDirPath + QStringLiteral("/genlot-vpn-") + date + QStringLiteral(".log");
    m_errorLogFilePath = m_logDirPath + QStringLiteral("/genlot-vpn-error-") + date + QStringLiteral(".log");
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
    if (normalized == QStringLiteral("debug")) {
        return QStringLiteral("DEBUG");
    }
    return QStringLiteral("INFO");
}

bool AppLogger::isErrorLevel(const QString &level)
{
    return level.trimmed().toLower() == QStringLiteral("error");
}

void AppLogger::log(const QString &level, const QString &message)
{
    const QString text = message.trimmed();
    if (text.isEmpty()) {
        return;
    }

    ensureLogFiles();

    const QString timeFull = QDateTime::currentDateTime().toString(QStringLiteral("yyyy-MM-dd HH:mm:ss"));
    const QString label = levelLabel(level);
    const QString line = QStringLiteral("[%1] [%2] %3").arg(timeFull, label, text);

    appendToFile(m_logFilePath, line, isErrorLevel(level));
    if (isErrorLevel(level)) {
        appendToFile(m_errorLogFilePath, line, true);
    }
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

} // namespace vpn
