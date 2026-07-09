#pragma once

#include <QAbstractListModel>
#include <QDateTime>
#include <QVariantMap>
#include <QVector>

namespace vpnproxy {

struct ProxyLogEntry {
    QString id;
    QDateTime time;
    QString method;
    QString path;
    int status = 0;
    QString peerIp;
    QString requestLog;
    QString responseLog;
    bool success = false;
};

class ProxyLogModel : public QAbstractListModel {
    Q_OBJECT
    Q_PROPERTY(int count READ count NOTIFY countChanged)

public:
    enum Roles {
        IdRole = Qt::UserRole + 1,
        TimeRole,
        MethodRole,
        PathRole,
        StatusRole,
        PeerIpRole,
        RequestLogRole,
        ResponseLogRole,
        SuccessRole
    };

    explicit ProxyLogModel(QObject *parent = nullptr);

    int rowCount(const QModelIndex &parent = QModelIndex()) const override;
    QVariant data(const QModelIndex &index, int role = Qt::DisplayRole) const override;
    QHash<int, QByteArray> roleNames() const override;
    int count() const { return m_entries.size(); }

    void appendEntry(const ProxyLogEntry &entry);
    Q_INVOKABLE void clear();
    Q_INVOKABLE QVariantMap entryAt(int row) const;

signals:
    void countChanged();
    void entryAdded();

private:
    static constexpr int kMaxEntries = 200;
    QVector<ProxyLogEntry> m_entries;
};

} // namespace vpnproxy
