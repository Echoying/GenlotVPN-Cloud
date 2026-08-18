#include "LocaleManager.h"
#include "AppPaths.h"
#include "SecureStorage.h"
#include <QApplication>
#include <QCoreApplication>
#include <QDir>
#include <QQmlApplicationEngine>
#include <QVariantMap>

namespace vpn {

namespace {

struct LocaleEntry {
    const char *id;
    const char *displayName;
};

const LocaleEntry kLocales[] = {
    {"zh_CN", "简体中文"},
    {"en", "English"},
};

} // namespace

LocaleManager::LocaleManager(SecureStorage *storage, QObject *parent)
    : QObject(parent)
    , m_storage(storage)
{
}

QVariantList LocaleManager::availableLocales() const
{
    QVariantList list;
    for (const LocaleEntry &entry : kLocales) {
        QVariantMap item;
        item.insert(QStringLiteral("id"), QString::fromUtf8(entry.id));
        item.insert(QStringLiteral("name"), QString::fromUtf8(entry.displayName));
        list.append(item);
    }
    return list;
}

QString LocaleManager::displayName(const QString &locale) const
{
    const QString normalized = normalizeLocale(locale);
    for (const LocaleEntry &entry : kLocales) {
        if (normalized == QLatin1String(entry.id)) {
            return QString::fromUtf8(entry.displayName);
        }
    }
    return normalized;
}

int LocaleManager::localeIndex(const QString &locale) const
{
    const QString normalized = normalizeLocale(locale);
    for (int i = 0; i < static_cast<int>(sizeof(kLocales) / sizeof(kLocales[0])); ++i) {
        if (normalized == QLatin1String(kLocales[i].id)) {
            return i;
        }
    }
    return 0;
}

bool LocaleManager::installTranslator(QApplication *app, QQmlApplicationEngine *engine)
{
    m_app = app;
    m_engine = engine;

    QString locale = QStringLiteral("zh_CN");
    if (m_storage) {
        const QVariantMap cfg = m_storage->loadConfigFile();
        locale = cfg.value(QStringLiteral("locale")).toString();
    }
    return applyLocale(locale);
}

void LocaleManager::setLocale(const QString &locale)
{
    const QString normalized = normalizeLocale(locale);
    if (normalized == m_currentLocale) {
        return;
    }
    if (!applyLocale(normalized)) {
        return;
    }
    if (m_storage) {
        m_storage->saveConfigLocale(normalized);
    }
    emit localeChanged();
}

QString LocaleManager::normalizeLocale(const QString &locale) const
{
    const QString trimmed = locale.trimmed();
    if (trimmed.isEmpty() || trimmed == QLatin1String("zh") || trimmed == QLatin1String("zh-CN")) {
        return QStringLiteral("zh_CN");
    }
    if (trimmed == QLatin1String("en") || trimmed.startsWith(QLatin1String("en_"))
        || trimmed.startsWith(QLatin1String("en-"))) {
        return QStringLiteral("en");
    }
    return trimmed;
}

QString LocaleManager::translationsDir() const
{
    return AppPaths::i18nDir();
}

bool LocaleManager::applyLocale(const QString &locale)
{
    const QString normalized = normalizeLocale(locale);
    if (!m_app) {
        m_currentLocale = normalized;
        return true;
    }

    if (m_translatorInstalled) {
        m_app->removeTranslator(&m_translator);
        m_translatorInstalled = false;
    }

    const QString qmPath = translationsDir() + QLatin1Char('/')
                           + QStringLiteral("genlotvpn_") + normalized + QStringLiteral(".qm");
    bool loaded = m_translator.load(qmPath);
    if (!loaded && normalized != QLatin1String("zh_CN")) {
        const QString fallback = translationsDir() + QStringLiteral("/genlotvpn_zh_CN.qm");
        loaded = m_translator.load(fallback);
    }

    if (loaded) {
        m_app->installTranslator(&m_translator);
        m_translatorInstalled = true;
    }

    m_currentLocale = normalized;
    if (m_engine) {
        m_engine->setUiLanguage(normalized);
        m_engine->retranslate();
    }
    return true;
}

} // namespace vpn
