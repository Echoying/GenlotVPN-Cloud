#include "SessionManager.h"

namespace vpn {

SessionManager::SessionManager(QObject *parent) : QObject(parent) {}

void SessionManager::setPendingLine(const QVariantMap &line)
{
    if (m_pendingLine != line) {
        m_pendingLine = line;
        emit pendingLineChanged();
    }
}

void SessionManager::setSelectedLine(const QVariantMap &line)
{
    if (m_selectedLine != line) {
        m_selectedLine = line;
        emit selectedLineChanged();
    }
}

void SessionManager::setAccessToken(const QString &token)
{
    if (m_accessToken != token) {
        m_accessToken = token;
        emit accessTokenChanged();
    }
}

void SessionManager::clear()
{
    m_pendingLine.clear();
    m_selectedLine.clear();
    m_accessToken.clear();
    emit pendingLineChanged();
    emit selectedLineChanged();
    emit accessTokenChanged();
}

} // namespace vpn
