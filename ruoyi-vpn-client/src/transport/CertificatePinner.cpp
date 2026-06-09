#include "CertificatePinner.h"
#include <QCryptographicHash>
#include <QSslKey>

namespace vpn {

CertificatePinner::CertificatePinner(QString expectedSpkiSha256)
    : m_pin(std::move(expectedSpkiSha256))
{
}

bool CertificatePinner::verify(QSslSocket *socket) const
{
    if (m_pin.isEmpty()) {
        return true;
    }
    const auto certs = socket->peerCertificateChain();
    if (certs.isEmpty()) {
        return false;
    }
    const QSslKey pubKey = certs.first().publicKey();
    const QByteArray spki = pubKey.toDer();
    const QByteArray hash = QCryptographicHash::hash(spki, QCryptographicHash::Sha256).toHex();
    return hash.compare(m_pin.toLatin1(), Qt::CaseInsensitive) == 0;
}

} // namespace vpn
