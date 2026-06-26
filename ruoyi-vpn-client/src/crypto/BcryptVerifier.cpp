#include "BcryptVerifier.h"

#include "bcrypt_tables.h"
#include "bcrypt_index64.h"

#include <QByteArray>
#include <QString>

#include <array>
#include <cstdint>
#include <cstring>
#include <vector>

namespace vpn {
namespace {

constexpr int kBlowfishRounds = 16;
constexpr int kBcryptSaltLen = 16;
constexpr char kBase64Code[] =
    "./ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

int8_t char64(char c)
{
    const auto idx = static_cast<unsigned char>(c);
    if (idx >= bcrypt_tables::INDEX_64.size()) {
        return -1;
    }
    return bcrypt_tables::INDEX_64[idx];
}

class BCrypt {
public:
    std::array<uint32_t, 18> P{};
    std::array<uint32_t, 1024> S{};

    void initKey()
    {
        for (int i = 0; i < 18; ++i) {
            P[static_cast<size_t>(i)] = bcrypt_tables::P_ORIG[i];
        }
        for (int i = 0; i < 1024; ++i) {
            S[static_cast<size_t>(i)] = bcrypt_tables::S_ORIG[i];
        }
    }

    void encipher(uint32_t &l, uint32_t &r) const
    {
        l ^= P[0];
        int i = 0;
        while (i <= kBlowfishRounds - 2) {
            uint32_t n = S[(l >> 24) & 0xff];
            n += S[0x100 | ((l >> 16) & 0xff)];
            n ^= S[0x200 | ((l >> 8) & 0xff)];
            n += S[0x300 | (l & 0xff)];
            r ^= n ^ P[static_cast<size_t>(++i)];

            n = S[(r >> 24) & 0xff];
            n += S[0x100 | ((r >> 16) & 0xff)];
            n ^= S[0x200 | ((r >> 8) & 0xff)];
            n += S[0x300 | (r & 0xff)];
            l ^= n ^ P[static_cast<size_t>(++i)];
        }
        const uint32_t tmp = l;
        l = r ^ P[static_cast<size_t>(kBlowfishRounds + 1)];
        r = tmp;
    }

    static void streamToWords(const std::vector<uint8_t> &data, int &off, int &sign,
                              uint32_t &word0, uint32_t &word1)
    {
        word0 = 0;
        word1 = 0;
        const int len = static_cast<int>(data.size());
        for (int i = 0; i < 4; ++i) {
            const uint8_t b = data[static_cast<size_t>(off % len)];
            word0 = (word0 << 8) | b;
            word1 = (word1 << 8) | static_cast<int8_t>(b);
            if (i > 0) {
                sign |= static_cast<int>(word1 & 0x80);
            }
            off = (off + 1) % len;
        }
    }

    static uint32_t streamToWord(const std::vector<uint8_t> &data, int &off, bool signExtBug,
                               int &sign)
    {
        uint32_t w0 = 0;
        uint32_t w1 = 0;
        streamToWords(data, off, sign, w0, w1);
        return signExtBug ? w1 : w0;
    }

    static uint32_t streamToWordIsolated(const std::vector<uint8_t> &data, int &off,
                                         bool signExtBug)
    {
        int sign = 0;
        return streamToWord(data, off, signExtBug, sign);
    }

    void keySchedule(const std::vector<uint8_t> &key, bool signExtBug)
    {
        int off = 0;
        for (int i = 0; i < 18; ++i) {
            P[static_cast<size_t>(i)] ^=
                streamToWordIsolated(key, off, signExtBug);
        }
        uint32_t l = 0;
        uint32_t r = 0;
        for (int i = 0; i < 18; i += 2) {
            encipher(l, r);
            P[static_cast<size_t>(i)] = l;
            P[static_cast<size_t>(i + 1)] = r;
        }
        for (int i = 0; i < 1024; i += 2) {
            encipher(l, r);
            S[static_cast<size_t>(i)] = l;
            S[static_cast<size_t>(i + 1)] = r;
        }
    }

    void eksKey(const std::vector<uint8_t> &salt, const std::vector<uint8_t> &key,
                bool signExtBug, int safety)
    {
        int koff = 0;
        int doff = 0;
        int sign = 0;
        uint32_t diff = 0;
        for (int i = 0; i < 18; ++i) {
            uint32_t w0 = 0;
            uint32_t w1 = 0;
            streamToWords(key, koff, sign, w0, w1);
            diff |= w0 ^ w1;
            P[static_cast<size_t>(i)] ^= signExtBug ? w1 : w0;
        }
        diff |= diff >> 16;
        diff &= 0xffff;
        diff += 0xffff;
        int flip = (sign << 9) & (~diff & safety);

        uint32_t l = 0;
        uint32_t r = 0;
        P[0] ^= static_cast<uint32_t>(flip);

        for (int i = 0; i < 18; i += 2) {
            l ^= streamToWordIsolated(salt, doff, false);
            r ^= streamToWordIsolated(salt, doff, false);
            encipher(l, r);
            P[static_cast<size_t>(i)] = l;
            P[static_cast<size_t>(i + 1)] = r;
        }
        for (int i = 0; i < 1024; i += 2) {
            l ^= streamToWordIsolated(salt, doff, false);
            r ^= streamToWordIsolated(salt, doff, false);
            encipher(l, r);
            S[static_cast<size_t>(i)] = l;
            S[static_cast<size_t>(i + 1)] = r;
        }
    }

    std::array<uint8_t, 24> cryptRaw(const std::vector<uint8_t> &password,
                                     const std::array<uint8_t, 16> &salt, int logRounds,
                                     bool signExtBug, int safety)
    {
        std::array<uint32_t, 6> cdata{};
        for (int i = 0; i < 6; ++i) {
            cdata[static_cast<size_t>(i)] = bcrypt_tables::BF_CRYPT_CIPHERTEXT[i];
        }

        if (logRounds < 4 || logRounds > 31) {
            return {};
        }
        const uint64_t rounds = 1ULL << logRounds;

        initKey();
        eksKey(std::vector<uint8_t>(salt.begin(), salt.end()), password, signExtBug, safety);
        for (uint64_t i = 0; i < rounds; ++i) {
            keySchedule(password, signExtBug);
            keySchedule(std::vector<uint8_t>(salt.begin(), salt.end()), false);
        }

        for (int i = 0; i < 64; ++i) {
            for (int j = 0; j < 3; ++j) {
                uint32_t l = cdata[static_cast<size_t>(j * 2)];
                uint32_t r = cdata[static_cast<size_t>(j * 2 + 1)];
                encipher(l, r);
                cdata[static_cast<size_t>(j * 2)] = l;
                cdata[static_cast<size_t>(j * 2 + 1)] = r;
            }
        }

        std::array<uint8_t, 24> ret{};
        int idx = 0;
        for (int i = 0; i < 6; ++i) {
            ret[static_cast<size_t>(idx++)] = static_cast<uint8_t>((cdata[static_cast<size_t>(i)] >> 24) & 0xff);
            ret[static_cast<size_t>(idx++)] = static_cast<uint8_t>((cdata[static_cast<size_t>(i)] >> 16) & 0xff);
            ret[static_cast<size_t>(idx++)] = static_cast<uint8_t>((cdata[static_cast<size_t>(i)] >> 8) & 0xff);
            ret[static_cast<size_t>(idx++)] = static_cast<uint8_t>(cdata[static_cast<size_t>(i)] & 0xff);
        }
        return ret;
    }
};

bool decodeBase64(const QByteArray &s, int maxOlen, std::vector<uint8_t> &out)
{
    out.clear();
    int off = 0;
    const int slen = s.size();
    while (off < slen - 1 && static_cast<int>(out.size()) < maxOlen) {
        const int8_t c1 = char64(s[off++]);
        const int8_t c2 = char64(s[off++]);
        if (c1 < 0 || c2 < 0) {
            break;
        }
        uint8_t o = static_cast<uint8_t>(c1 << 2);
        o |= static_cast<uint8_t>((c2 & 0x30) >> 4);
        out.push_back(o);
        if (static_cast<int>(out.size()) >= maxOlen || off >= slen) {
            break;
        }
        const int8_t c3 = char64(s[off++]);
        if (c3 < 0) {
            break;
        }
        o = static_cast<uint8_t>((c2 & 0x0f) << 4);
        o |= static_cast<uint8_t>((c3 & 0x3c) >> 2);
        out.push_back(o);
        if (static_cast<int>(out.size()) >= maxOlen || off >= slen) {
            break;
        }
        const int8_t c4 = char64(s[off++]);
        if (c4 < 0) {
            break;
        }
        o = static_cast<uint8_t>((c3 & 0x03) << 6);
        o |= static_cast<uint8_t>(c4);
        out.push_back(o);
    }
    return !out.empty();
}

void encodeBase64(const uint8_t *data, int len, QByteArray &out)
{
    int off = 0;
    while (off < len) {
        int c1 = data[off++] & 0xff;
        out.append(kBase64Code[(c1 >> 2) & 0x3f]);
        c1 = (c1 & 0x03) << 4;
        if (off >= len) {
            out.append(kBase64Code[c1 & 0x3f]);
            break;
        }
        int c2 = data[off++] & 0xff;
        c1 |= (c2 >> 4) & 0x0f;
        out.append(kBase64Code[c1 & 0x3f]);
        c1 = (c2 & 0x0f) << 2;
        if (off >= len) {
            out.append(kBase64Code[c1 & 0x3f]);
            break;
        }
        c2 = data[off++] & 0xff;
        c1 |= (c2 >> 6) & 0x03;
        out.append(kBase64Code[c1 & 0x3f]);
        out.append(kBase64Code[c2 & 0x3f]);
    }
}

QString hashPw(const QByteArray &passwordBytes, const QString &salt)
{
    if (salt.size() < 28 || salt[0] != QLatin1Char('$') || salt[1] != QLatin1Char('2')) {
        return {};
    }

    int off = 3;
    QChar minor = QLatin1Char('\0');
    if (salt[2] != QLatin1Char('$')) {
        minor = salt[2];
        if ((minor != QLatin1Char('a') && minor != QLatin1Char('x') && minor != QLatin1Char('y')
             && minor != QLatin1Char('b'))
            || salt[3] != QLatin1Char('$')) {
            return {};
        }
        off = 4;
    }
    if (salt.size() < off + 25) {
        return {};
    }

    const int rounds = salt.mid(off, 2).toInt();
    const QString realSalt = salt.mid(off + 3, 22);
    std::vector<uint8_t> saltb;
    if (!decodeBase64(realSalt.toLatin1(), kBcryptSaltLen, saltb)
        || static_cast<int>(saltb.size()) != kBcryptSaltLen) {
        return {};
    }

    std::vector<uint8_t> password(reinterpret_cast<const uint8_t *>(passwordBytes.constData()),
                                  reinterpret_cast<const uint8_t *>(passwordBytes.constData())
                                      + passwordBytes.size());
    if (minor >= QLatin1Char('a')) {
        password.push_back(0);
    }

    const bool signExtBug = (minor == QLatin1Char('x'));
    const int safety = (minor == QLatin1Char('a')) ? 0x10000 : 0;

    std::array<uint8_t, 16> saltArr{};
    std::memcpy(saltArr.data(), saltb.data(), kBcryptSaltLen);

    BCrypt bcrypt;
    const std::array<uint8_t, 24> hashed =
        bcrypt.cryptRaw(password, saltArr, rounds, signExtBug, safety);

    QByteArray result("$2", 2);
    if (minor != QLatin1Char('\0')) {
        result.append(minor.toLatin1());
    }
    result.append('$');
    if (rounds < 10) {
        result.append('0');
    }
    result.append(QByteArray::number(rounds));
    result.append('$');
    encodeBase64(saltArr.data(), kBcryptSaltLen, result);
    encodeBase64(hashed.data(), 23, result);
    return QString::fromLatin1(result);
}

bool slowEquals(const QByteArray &a, const QByteArray &b)
{
    if (a.size() != b.size()) {
        return false;
    }
    char diff = 0;
    for (int i = 0; i < a.size(); ++i) {
        diff |= static_cast<char>(a[i] ^ b[i]);
    }
    return diff == 0;
}

} // namespace

bool BcryptVerifier::matches(const QString &plainPassword, const QString &bcryptHash)
{
    const QByteArray hashBytes = bcryptHash.toUtf8();
    if (hashBytes.size() != 60) {
        return false;
    }
    const QString computed = hashPw(plainPassword.toUtf8(), QString::fromUtf8(hashBytes));
    if (computed.isEmpty()) {
        return false;
    }
    return slowEquals(hashBytes, computed.toUtf8());
}

} // namespace vpn
