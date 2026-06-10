#pragma once

#include <QObject>
#include <QString>

namespace vpn {

/** 应用版本信息，供 QML 与启动日志使用 */
class AppInfo : public QObject {
    Q_OBJECT
    Q_PROPERTY(QString version READ version CONSTANT)
    Q_PROPERTY(QString buildId READ buildId CONSTANT)
    Q_PROPERTY(QString versionLabel READ versionLabel CONSTANT)
    Q_PROPERTY(QString windowTitle READ windowTitle CONSTANT)

public:
    explicit AppInfo(QObject *parent = nullptr);

    QString version() const;
    QString buildId() const;
    QString versionLabel() const;
    QString windowTitle() const;
};

} // namespace vpn
