#pragma once

#include <QByteArray>
#include <cstdint>

namespace vpn {

constexpr char kFrameMagic[] = "GVPN";
constexpr uint8_t kFrameVersion = 0x01;
constexpr int kFrameHeaderSize = 9;

/** GVPN 帧编解码 */
class FrameCodec {
public:
    static QByteArray encode(const QByteArray &protobufBody);
    static bool tryDecode(QByteArray &buffer, QByteArray &outBody);
};

} // namespace vpn
