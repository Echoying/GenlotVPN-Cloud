#pragma once

#include <QSslCertificate>
#include <QSslSocket>
#include <QString>

namespace vpn {

/** TLS 证书 Pinning */
class CertificatePinner {
public:
    explicit CertificatePinner(QString expectedSpkiSha256);

    bool verify(QSslSocket *socket) const;

private:
    QString m_pin;
};

} // namespace vpn
