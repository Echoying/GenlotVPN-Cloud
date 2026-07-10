#pragma once

#include <QAbstractListModel>
#include <QDateTime>
#include <QVector>

namespace vpnproxy {

struct SessionLogPayload {
    QString type;
    QString message;
    QString requestLog;
    QString responseLog;
    qint64 elapsedMs = -1;
};

struct SessionLogEntry {
    QString id;
    QDateTime time;
    QString type;
    QString message;
    QString requestLog;
    QString responseLog;
    qint64 elapsedMs = -1;
};

class SessionLogModel : public QAbstractListModel {
    Q_OBJECT
    Q_PROPERTY(int count READ count NOTIFY countChanged)

public:
    enum Roles {
        IdRole = Qt::UserRole + 1,
        TimeRole,
        TypeRole,
        MessageRole,
        RequestLogRole,
        ResponseLogRole,
        ElapsedMsRole
    };

    explicit SessionLogModel(QObject *parent = nullptr);

    int rowCount(const QModelIndex &parent = QModelIndex()) const override;
    QVariant data(const QModelIndex &index, int role = Qt::DisplayRole) const override;
    QHash<int, QByteArray> roleNames() const override;
    int count() const { return m_entries.size(); }

    void append(const SessionLogPayload &payload);
    Q_INVOKABLE void clear();

signals:
    void countChanged();
    void entryAdded();

private:
    static constexpr int kMaxEntries = 500;
    QVector<SessionLogEntry> m_entries;
    qint64 m_seq = 0;
};

} // namespace vpnproxy
