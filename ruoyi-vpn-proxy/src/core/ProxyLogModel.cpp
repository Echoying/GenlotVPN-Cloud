#include "ProxyLogModel.h"
#include "AppLogger.h"

namespace vpnproxy {

ProxyLogModel::ProxyLogModel(QObject *parent) : QAbstractListModel(parent) {}

int ProxyLogModel::rowCount(const QModelIndex &parent) const
{
    return parent.isValid() ? 0 : m_entries.size();
}

QVariant ProxyLogModel::data(const QModelIndex &index, int role) const
{
    if (!index.isValid() || index.row() < 0 || index.row() >= m_entries.size()) {
        return {};
    }
    const ProxyLogEntry &entry = m_entries.at(index.row());
    switch (role) {
    case IdRole: return entry.id;
    case TimeRole: return entry.time.toString(QStringLiteral("yyyy-MM-dd HH:mm:ss"));
    case MethodRole: return entry.method;
    case PathRole: return entry.path;
    case StatusRole: return entry.status;
    case PeerIpRole: return entry.peerIp;
    case RequestLogRole: return entry.requestLog;
    case ResponseLogRole: return entry.responseLog;
    case SuccessRole: return entry.success;
    default: return entry.path;
    }
}

QHash<int, QByteArray> ProxyLogModel::roleNames() const
{
    return {{IdRole, "id"}, {TimeRole, "time"}, {MethodRole, "method"}, {PathRole, "path"},
            {StatusRole, "status"}, {PeerIpRole, "peerIp"}, {RequestLogRole, "requestLog"},
            {ResponseLogRole, "responseLog"}, {SuccessRole, "success"}};
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
    const QString summary = QStringLiteral("%1 %2 peer=%3 url=%4")
                                .arg(entry.method)
                                .arg(entry.status > 0 ? QString::number(entry.status) : QStringLiteral("-"))
                                .arg(entry.peerIp, entry.path);
    AppLogger::instance()->logProxyAccess(summary, entry.requestLog, entry.responseLog);
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
    const ProxyLogEntry &e = m_entries.at(row);
    return {{QStringLiteral("id"), e.id}, {QStringLiteral("time"), e.time.toString(QStringLiteral("yyyy-MM-dd HH:mm:ss"))},
            {QStringLiteral("method"), e.method}, {QStringLiteral("path"), e.path},
            {QStringLiteral("status"), e.status}, {QStringLiteral("peerIp"), e.peerIp},
            {QStringLiteral("requestLog"), e.requestLog}, {QStringLiteral("responseLog"), e.responseLog},
            {QStringLiteral("success"), e.success}};
}

} // namespace vpnproxy
