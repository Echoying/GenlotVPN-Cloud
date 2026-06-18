package com.ruoyi.yianlian.service.vpn;

import com.ruoyi.yianlian.domain.VpnLocalUser;

import java.util.List;
import java.util.Map;

/**
 * VPN本地用户 服务层
 */
public interface IVpnLocalUserService
{
    List<VpnLocalUser> selectLocalUserList(VpnLocalUser user);

    VpnLocalUser selectLocalUserById(Long localUserId);

    VpnLocalUser selectLocalUserByUserName(String userName);

    boolean checkUserNameUnique(VpnLocalUser user);

    int insertLocalUser(VpnLocalUser user, String plainPassword);

    int updateLocalUser(VpnLocalUser user);

    int resetPwd(VpnLocalUser user, String plainPassword);

    int updateLocalUserStatus(VpnLocalUser user);

    int deleteLocalUserByIds(Long[] localUserIds);

    void updateLocalUserLogin(VpnLocalUser user);

    void changeLocalPassword(VpnLocalUser user, String newPassword);

    List<Map<String, Object>> getAuthorizedLines(Long localUserId);

    boolean isAuthorizedForLine(Long localUserId, String appId);

    /**
     * 获取线路用户控制器登录凭证（明文密码，仅内部调用）
     */
    Map<String, String> getLineUserCredentials(Long localUserId, String appId);
}
