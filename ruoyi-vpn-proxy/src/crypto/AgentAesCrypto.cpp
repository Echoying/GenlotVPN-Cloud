#include "AgentAesCrypto.h"

#include <QByteArray>

#ifdef Q_OS_WIN
#include <windows.h>
#include <bcrypt.h>
#endif

namespace vpnproxy {

namespace {

constexpr char kKey[] = "EnSwordAgent@123";
constexpr char kIv[] = "321@tnegAdrowSnE";

#ifdef Q_OS_WIN

QByteArray aesCbcCrypt(const QByteArray &input, bool encrypt)
{
    BCRYPT_ALG_HANDLE alg = nullptr;
    BCRYPT_KEY_HANDLE key = nullptr;

    NTSTATUS status = BCryptOpenAlgorithmProvider(&alg, BCRYPT_AES_ALGORITHM, nullptr, 0);
    if (status != 0) {
        return QByteArray();
    }

    status = BCryptSetProperty(alg, BCRYPT_CHAINING_MODE,
                               reinterpret_cast<PUCHAR>(const_cast<wchar_t *>(BCRYPT_CHAIN_MODE_CBC)),
                               sizeof(BCRYPT_CHAIN_MODE_CBC), 0);
    if (status != 0) {
        BCryptCloseAlgorithmProvider(alg, 0);
        return QByteArray();
    }

    status = BCryptGenerateSymmetricKey(alg, &key, nullptr, 0,
                                        reinterpret_cast<PUCHAR>(const_cast<char *>(kKey)),
                                        static_cast<ULONG>(sizeof(kKey) - 1), 0);
    if (status != 0) {
        BCryptCloseAlgorithmProvider(alg, 0);
        return QByteArray();
    }

    QByteArray ivBuf(kIv, static_cast<int>(sizeof(kIv) - 1));
    QByteArray output;
    ULONG resultSize = 0;

    if (encrypt) {
        status = BCryptEncrypt(key,
                               reinterpret_cast<PUCHAR>(const_cast<char *>(input.data())),
                               static_cast<ULONG>(input.size()),
                               nullptr,
                               reinterpret_cast<PUCHAR>(ivBuf.data()),
                               static_cast<ULONG>(ivBuf.size()),
                               nullptr,
                               0,
                               &resultSize,
                               BCRYPT_BLOCK_PADDING);
        if (status != 0) {
            BCryptDestroyKey(key);
            BCryptCloseAlgorithmProvider(alg, 0);
            return QByteArray();
        }
        output.resize(static_cast<int>(resultSize));
        ivBuf = QByteArray(kIv, static_cast<int>(sizeof(kIv) - 1));
        status = BCryptEncrypt(key,
                               reinterpret_cast<PUCHAR>(const_cast<char *>(input.data())),
                               static_cast<ULONG>(input.size()),
                               nullptr,
                               reinterpret_cast<PUCHAR>(ivBuf.data()),
                               static_cast<ULONG>(ivBuf.size()),
                               reinterpret_cast<PUCHAR>(output.data()),
                               resultSize,
                               &resultSize,
                               BCRYPT_BLOCK_PADDING);
        output.resize(static_cast<int>(resultSize));
    } else {
        status = BCryptDecrypt(key,
                               reinterpret_cast<PUCHAR>(const_cast<char *>(input.data())),
                               static_cast<ULONG>(input.size()),
                               nullptr,
                               reinterpret_cast<PUCHAR>(ivBuf.data()),
                               static_cast<ULONG>(ivBuf.size()),
                               nullptr,
                               0,
                               &resultSize,
                               BCRYPT_BLOCK_PADDING);
        if (status != 0) {
            BCryptDestroyKey(key);
            BCryptCloseAlgorithmProvider(alg, 0);
            return QByteArray();
        }
        output.resize(static_cast<int>(resultSize));
        ivBuf = QByteArray(kIv, static_cast<int>(sizeof(kIv) - 1));
        status = BCryptDecrypt(key,
                               reinterpret_cast<PUCHAR>(const_cast<char *>(input.data())),
                               static_cast<ULONG>(input.size()),
                               nullptr,
                               reinterpret_cast<PUCHAR>(ivBuf.data()),
                               static_cast<ULONG>(ivBuf.size()),
                               reinterpret_cast<PUCHAR>(output.data()),
                               resultSize,
                               &resultSize,
                               BCRYPT_BLOCK_PADDING);
        output.resize(static_cast<int>(resultSize));
    }

    BCryptDestroyKey(key);
    BCryptCloseAlgorithmProvider(alg, 0);
    if (status != 0) {
        return QByteArray();
    }
    return output;
}

#endif

} // namespace

QString AgentAesCrypto::encryptToBase64(const QByteArray &plainUtf8)
{
#ifdef Q_OS_WIN
    const QByteArray cipher = aesCbcCrypt(plainUtf8, true);
    if (cipher.isEmpty() && !plainUtf8.isEmpty()) {
        return QString();
    }
    return QString::fromLatin1(cipher.toBase64());
#else
    Q_UNUSED(plainUtf8);
    return QString();
#endif
}

QByteArray AgentAesCrypto::decryptFromBase64(const QByteArray &cipherBase64)
{
#ifdef Q_OS_WIN
    const QByteArray decoded = QByteArray::fromBase64(cipherBase64);
    if (decoded.isEmpty() && !cipherBase64.isEmpty()) {
        return QByteArray();
    }
    return aesCbcCrypt(decoded, false);
#else
    Q_UNUSED(cipherBase64);
    return QByteArray();
#endif
}

} // namespace vpnproxy
