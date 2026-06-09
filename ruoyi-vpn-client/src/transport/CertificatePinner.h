#pragma once

#include <QByteArray>
#include <QSslSocket>
#include <QString>

namespace vpn {

/** TLS 证书 Pinning（SPKI SHA-256，支持主/备指纹） */
class CertificatePinner {
public:
    CertificatePinner(QString primarySpkiSha256, QString backupSpkiSha256 = QString());

    bool hasPin() const;
    bool verify(QSslSocket *socket) const;

private:
    bool matchesPin(const QByteArray &hashHex) const;

    QString m_primaryPin;
    QString m_backupPin;
};

} // namespace vpn
