#include "TcpHmacUtils.h"
#include <QCryptographicHash>
#include <QtEndian>

namespace vpn {

static QByteArray int32Be(int value)
{
    char buf[4];
    qToBigEndian(static_cast<qint32>(value), reinterpret_cast<uchar *>(buf));
    return QByteArray(buf, 4);
}

static QByteArray int64Be(qint64 value)
{
    char buf[8];
    qToBigEndian(value, reinterpret_cast<uchar *>(buf));
    return QByteArray(buf, 8);
}

QByteArray TcpHmacUtils::sign(const QByteArray &sessionKey, int typeValue, qint64 timestampMs,
                              const QByteArray &nonce, const QByteArray &payload)
{
    QCryptographicHash hash(QCryptographicHash::Sha256);
    // Qt 无 HMAC 直接 API 时用 keyed hash 近似 — 使用 QMac
    // Qt 6.5+ 有 qHmac — 为兼容性用手动 HMAC-SHA256
    const int blockSize = 64;
    QByteArray key = sessionKey;
    if (key.size() > blockSize) {
        key = QCryptographicHash::hash(key, QCryptographicHash::Sha256);
    }
    key.resize(blockSize, '\0');

    QByteArray oKeyPad(blockSize, 0x5c);
    QByteArray iKeyPad(blockSize, 0x36);
    for (int i = 0; i < key.size(); ++i) {
        oKeyPad[i] = oKeyPad[i] ^ key[i];
        iKeyPad[i] = iKeyPad[i] ^ key[i];
    }

    QCryptographicHash inner(QCryptographicHash::Sha256);
    inner.addData(iKeyPad);
    inner.addData(int32Be(typeValue));
    inner.addData(int64Be(timestampMs));
    if (!nonce.isEmpty()) {
        inner.addData(nonce);
    }
    if (!payload.isEmpty()) {
        inner.addData(payload);
    }

    QCryptographicHash outer(QCryptographicHash::Sha256);
    outer.addData(oKeyPad);
    outer.addData(inner.result());
    return outer.result();
}

bool TcpHmacUtils::needsMac(int messageType)
{
    return messageType >= 5; // LOGOUT 及之后
}

} // namespace vpn
