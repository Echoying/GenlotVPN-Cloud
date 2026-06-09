#include "CertificatePinner.h"
#include <QCryptographicHash>
#include <QSslKey>

namespace vpn {

namespace {

QString normalizePin(QString pin)
{
    return pin.trimmed().toLower();
}

} // namespace

CertificatePinner::CertificatePinner(QString primarySpkiSha256, QString backupSpkiSha256)
    : m_primaryPin(normalizePin(std::move(primarySpkiSha256)))
    , m_backupPin(normalizePin(std::move(backupSpkiSha256)))
{
}

bool CertificatePinner::hasPin() const
{
    return !m_primaryPin.isEmpty();
}

bool CertificatePinner::matchesPin(const QByteArray &hashHex) const
{
    const QByteArray normalized = hashHex.toLower();
    if (!m_primaryPin.isEmpty() && normalized == m_primaryPin.toLatin1()) {
        return true;
    }
    if (!m_backupPin.isEmpty() && normalized == m_backupPin.toLatin1()) {
        return true;
    }
    return false;
}

bool CertificatePinner::verify(QSslSocket *socket) const
{
    if (!hasPin()) {
        return true;
    }
    const auto certs = socket->peerCertificateChain();
    if (certs.isEmpty()) {
        return false;
    }
    const QSslKey pubKey = certs.first().publicKey();
    const QByteArray spki = pubKey.toDer();
    const QByteArray hash = QCryptographicHash::hash(spki, QCryptographicHash::Sha256).toHex();
    return matchesPin(hash);
}

} // namespace vpn
