#pragma once

#include <QString>

namespace vpn {

/** 本机设备信息（IP / 操作系统 / MAC），用于登录审计上报 */
class ClientDeviceInfo {
public:
    static QString localIpv4();
    static QString osDescription();
    static QString macAddress();
    /** 平台标识：windows / macos / unknown（供云端版本策略选下载链接） */
    static QString platformId();
};

} // namespace vpn
