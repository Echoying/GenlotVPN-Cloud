#include "FrameCodec.h"

namespace vpn {

QByteArray FrameCodec::encode(const QByteArray &protobufBody)
{
    QByteArray frame;
    frame.append(kFrameMagic, 4);
    frame.append(static_cast<char>(kFrameVersion));
    const int len = protobufBody.size();
    frame.append(static_cast<char>((len >> 24) & 0xFF));
    frame.append(static_cast<char>((len >> 16) & 0xFF));
    frame.append(static_cast<char>((len >> 8) & 0xFF));
    frame.append(static_cast<char>(len & 0xFF));
    frame.append(protobufBody);
    return frame;
}

bool FrameCodec::tryDecode(QByteArray &buffer, QByteArray &outBody)
{
    if (buffer.size() < kFrameHeaderSize) {
        return false;
    }
    if (buffer.left(4) != QByteArray(kFrameMagic, 4)) {
        buffer.remove(0, 1);
        return false;
    }
    if (static_cast<uint8_t>(buffer.at(4)) != kFrameVersion) {
        return false;
    }
    const int len = (static_cast<uint8_t>(buffer.at(5)) << 24)
                  | (static_cast<uint8_t>(buffer.at(6)) << 16)
                  | (static_cast<uint8_t>(buffer.at(7)) << 8)
                  | static_cast<uint8_t>(buffer.at(8));
    if (len <= 0 || len > 1048576) {
        buffer.clear();
        return false;
    }
    if (buffer.size() < kFrameHeaderSize + len) {
        return false;
    }
    outBody = buffer.mid(kFrameHeaderSize, len);
    buffer.remove(0, kFrameHeaderSize + len);
    return true;
}

} // namespace vpn
