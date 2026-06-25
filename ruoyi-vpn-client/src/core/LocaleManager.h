#pragma once

#include <QObject>
#include <QTranslator>
#include <QVariantList>

class QApplication;
class QQmlApplicationEngine;

namespace vpn {

class SecureStorage;

/** 应用语言：加载 .qm、持久化 config.json、热切换 QML 文案 */
class LocaleManager : public QObject {
    Q_OBJECT
    Q_PROPERTY(QString currentLocale READ currentLocale NOTIFY localeChanged)
    Q_PROPERTY(QVariantList availableLocales READ availableLocales CONSTANT)

public:
    explicit LocaleManager(SecureStorage *storage, QObject *parent = nullptr);

    QString currentLocale() const { return m_currentLocale; }
    QVariantList availableLocales() const;

    bool installTranslator(QApplication *app, QQmlApplicationEngine *engine);

    Q_INVOKABLE void setLocale(const QString &locale);
    Q_INVOKABLE QString displayName(const QString &locale) const;
    Q_INVOKABLE int localeIndex(const QString &locale) const;

signals:
    void localeChanged();

private:
    bool applyLocale(const QString &locale);
    QString normalizeLocale(const QString &locale) const;
    QString translationsDir() const;

    SecureStorage *m_storage = nullptr;
    QApplication *m_app = nullptr;
    QQmlApplicationEngine *m_engine = nullptr;
    QTranslator m_translator;
    bool m_translatorInstalled = false;
    QString m_currentLocale = QStringLiteral("zh_CN");
};

} // namespace vpn
