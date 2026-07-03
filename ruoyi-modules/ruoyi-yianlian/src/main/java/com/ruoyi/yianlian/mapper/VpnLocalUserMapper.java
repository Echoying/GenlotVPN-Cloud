package com.ruoyi.yianlian.mapper;

import com.ruoyi.yianlian.domain.VpnLocalUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * VPN本地用户 数据层
 */
public interface VpnLocalUserMapper
{
    List<VpnLocalUser> selectLocalUserList(VpnLocalUser user);

    VpnLocalUser selectLocalUserById(Long localUserId);

    VpnLocalUser selectLocalUserByUserName(String userName);

    VpnLocalUser checkUserNameUnique(String userName);

    int insertLocalUser(VpnLocalUser user);

    int updateLocalUser(VpnLocalUser user);

    int updateLocalUserLogin(VpnLocalUser user);

    int resetLocalUserPwd(VpnLocalUser user);

    int deleteLocalUserByIds(Long[] localUserIds);

    List<VpnLocalUser> selectUnsyncedByAppId(@Param("appId") String appId);
}
