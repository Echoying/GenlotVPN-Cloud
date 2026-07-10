#include "SessionLogModel.h"
#include "AppLogger.h"
#include "util/HttpUtil.h"

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
    case RequestLogRole: return e.requestLog;
    case ResponseLogRole: return e.responseLog;
    case ElapsedMsRole: return e.elapsedMs;
    default: return {};
    }
}

QHash<int, QByteArray> SessionLogModel::roleNames() const
{
    return {{IdRole, "id"},
            {TimeRole, "time"},
            {TypeRole, "type"},
            {MessageRole, "message"},
            {RequestLogRole, "requestLog"},
            {ResponseLogRole, "responseLog"},
            {ElapsedMsRole, "elapsedMs"}};
}

void SessionLogModel::append(const SessionLogPayload &payload)
{
    if (m_entries.size() >= kMaxEntries) {
        beginRemoveRows(QModelIndex(), m_entries.size() - 1, m_entries.size() - 1);
        m_entries.removeLast();
        endRemoveRows();
    }
    SessionLogEntry entry;
    entry.id = QString::number(++m_seq);
    entry.time = QDateTime::currentDateTime();
    entry.type = payload.type;
    entry.message = HttpUtil::sanitizeForLog(payload.message.trimmed());
    entry.requestLog = HttpUtil::sanitizeForLog(payload.requestLog.trimmed());
    entry.responseLog = HttpUtil::sanitizeForLog(payload.responseLog.trimmed());
    entry.elapsedMs = payload.elapsedMs;
    beginInsertRows(QModelIndex(), 0, 0);
    m_entries.prepend(entry);
    endInsertRows();
    AppLogger::instance()->logSession(entry.type, entry.message, entry.requestLog, entry.responseLog,
                                     entry.elapsedMs);
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
