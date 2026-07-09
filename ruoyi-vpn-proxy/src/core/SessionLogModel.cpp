#include "SessionLogModel.h"
#include "AppLogger.h"

namespace vpnproxy {

SessionLogModel::SessionLogModel(QObject *parent) : QAbstractListModel(parent) {}

int SessionLogModel::rowCount(const QModelIndex &parent) const
{
    return parent.isValid() ? 0 : m_entries.size();
}

QVariant SessionLogModel::data(const QModelIndex &index, int role) const
{
    if (!index.isValid() || index.row() < 0 || index.row() >= m_entries.size()) {
        return {};
    }
    const SessionLogEntry &e = m_entries.at(index.row());
    switch (role) {
    case IdRole: return e.id;
    case TimeRole: return e.time.toString(QStringLiteral("HH:mm:ss"));
    case TypeRole: return e.type;
    case MessageRole: return e.message;
    default: return {};
    }
}

QHash<int, QByteArray> SessionLogModel::roleNames() const
{
    return {{IdRole, "id"}, {TimeRole, "time"}, {TypeRole, "type"}, {MessageRole, "message"}};
}

void SessionLogModel::append(const QString &type, const QString &message)
{
    if (m_entries.size() >= kMaxEntries) {
        beginRemoveRows(QModelIndex(), m_entries.size() - 1, m_entries.size() - 1);
        m_entries.removeLast();
        endRemoveRows();
    }
    SessionLogEntry entry;
    entry.id = QString::number(++m_seq);
    entry.time = QDateTime::currentDateTime();
    entry.type = type;
    entry.message = message;
    beginInsertRows(QModelIndex(), 0, 0);
    m_entries.prepend(entry);
    endInsertRows();
    AppLogger::instance()->logSession(type, message);
    emit countChanged();
    emit entryAdded();
}

void SessionLogModel::clear()
{
    if (m_entries.isEmpty()) {
        return;
    }
    beginResetModel();
    m_entries.clear();
    endResetModel();
    emit countChanged();
}

} // namespace vpnproxy
