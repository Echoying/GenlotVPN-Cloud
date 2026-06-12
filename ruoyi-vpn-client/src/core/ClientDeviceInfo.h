#pragma once

#include <QString>

namespace vpn {

/** 本机设备信息（IP / 操作系统 / MAC），用于登录审计上报 */
class ClientDeviceInfo {
public:
    static QString localIpv4();
    static QString osDescription();
    static QString macAddress();
};

} // namespace vpn
