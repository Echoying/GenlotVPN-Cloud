#include "ProxyLogModel.h"

namespace vpn {

ProxyLogModel::ProxyLogModel(QObject *parent) : QAbstractListModel(parent) {}

int ProxyLogModel::rowCount(const QModelIndex &parent) const
{
    if (parent.isValid()) {
        return 0;
    }
    return m_entries.size();
}

QVariant ProxyLogModel::data(const QModelIndex &index, int role) const
{
    if (!index.isValid() || index.row() < 0 || index.row() >= m_entries.size()) {
        return {};
    }
    const ProxyLogEntry &entry = m_entries.at(index.row());
    switch (role) {
    case IdRole:
        return entry.id;
    case TimeRole:
        return entry.time.toString(QStringLiteral("yyyy-MM-dd HH:mm:ss.zzz"));
    case MethodRole:
        return entry.method;
    case PathRole:
        return entry.path;
    case StatusRole:
        return entry.status;
    case PeerIpRole:
        return entry.peerIp;
    case RequestLogRole:
        return entry.requestLog;
    case ResponseLogRole:
        return entry.responseLog;
    case SuccessRole:
        return entry.success;
    case Qt::DisplayRole:
        return entry.path;
    default:
        return {};
    }
}

QHash<int, QByteArray> ProxyLogModel::roleNames() const
{
    return {
        {IdRole, "id"},
        {TimeRole, "time"},
        {MethodRole, "method"},
        {PathRole, "path"},
        {StatusRole, "status"},
        {PeerIpRole, "peerIp"},
        {RequestLogRole, "requestLog"},
        {ResponseLogRole, "responseLog"},
        {SuccessRole, "success"},
    };
}

void ProxyLogModel::appendEntry(const ProxyLogEntry &entry)
{
    if (m_entries.size() >= kMaxEntries) {
        beginRemoveRows(QModelIndex(), m_entries.size() - 1, m_entries.size() - 1);
        m_entries.removeLast();
        endRemoveRows();
    }
    beginInsertRows(QModelIndex(), 0, 0);
    m_entries.prepend(entry);
    endInsertRows();
    emit countChanged();
    emit entryAdded();
}

void ProxyLogModel::clear()
{
    if (m_entries.isEmpty()) {
        return;
    }
    beginResetModel();
    m_entries.clear();
    endResetModel();
    emit countChanged();
}

QVariantMap ProxyLogModel::entryAt(int row) const
{
    if (row < 0 || row >= m_entries.size()) {
        return {};
    }
    const ProxyLogEntry &entry = m_entries.at(row);
    return {
        {QStringLiteral("id"), entry.id},
        {QStringLiteral("time"), entry.time.toString(QStringLiteral("yyyy-MM-dd HH:mm:ss.zzz"))},
        {QStringLiteral("method"), entry.method},
        {QStringLiteral("path"), entry.path},
        {QStringLiteral("status"), entry.status},
        {QStringLiteral("peerIp"), entry.peerIp},
        {QStringLiteral("requestLog"), entry.requestLog},
        {QStringLiteral("responseLog"), entry.responseLog},
        {QStringLiteral("success"), entry.success},
    };
}

} // namespace vpn
