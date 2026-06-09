#pragma once

#include <QObject>
#include <QVariantMap>

namespace vpn {

class SessionManager : public QObject {
    Q_OBJECT
    Q_PROPERTY(QVariantMap pendingLine READ pendingLine WRITE setPendingLine NOTIFY pendingLineChanged)
    Q_PROPERTY(QVariantMap selectedLine READ selectedLine WRITE setSelectedLine NOTIFY selectedLineChanged)
    Q_PROPERTY(QString accessToken READ accessToken WRITE setAccessToken NOTIFY accessTokenChanged)
public:
    explicit SessionManager(QObject *parent = nullptr);

    QVariantMap pendingLine() const { return m_pendingLine; }
    void setPendingLine(const QVariantMap &line);

    QVariantMap selectedLine() const { return m_selectedLine; }
    void setSelectedLine(const QVariantMap &line);

    QString accessToken() const { return m_accessToken; }
    void setAccessToken(const QString &token);

    Q_INVOKABLE void clear();

signals:
    void pendingLineChanged();
    void selectedLineChanged();
    void accessTokenChanged();

private:
    QVariantMap m_pendingLine;
    QVariantMap m_selectedLine;
    QString m_accessToken;
};

} // namespace vpn
