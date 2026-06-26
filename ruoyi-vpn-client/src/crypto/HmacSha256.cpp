#include "HmacSha256.h"

#include <QCryptographicHash>

namespace vpn {

QByteArray HmacSha256::sign(const QByteArray &key, const QByteArray &message)
{
    const int blockSize = 64;
    QByteArray normalizedKey = key;
    if (normalizedKey.size() > blockSize) {
        normalizedKey = QCryptographicHash::hash(normalizedKey, QCryptographicHash::Sha256);
    }
    normalizedKey.resize(blockSize, '\0');

    QByteArray oKeyPad(blockSize, static_cast<char>(0x5c));
    QByteArray iKeyPad(blockSize, static_cast<char>(0x36));
    for (int i = 0; i < normalizedKey.size(); ++i) {
        oKeyPad[i] = static_cast<char>(oKeyPad[i] ^ normalizedKey[i]);
        iKeyPad[i] = static_cast<char>(iKeyPad[i] ^ normalizedKey[i]);
    }

    QCryptographicHash inner(QCryptographicHash::Sha256);
    inner.addData(iKeyPad);
    inner.addData(message);

    QCryptographicHash outer(QCryptographicHash::Sha256);
    outer.addData(oKeyPad);
    outer.addData(inner.result());
    return outer.result();
}

} // namespace vpn
