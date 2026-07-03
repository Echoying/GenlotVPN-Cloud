package com.ruoyi.yianlian.mapper;

import com.ruoyi.yianlian.domain.VpnUser;
import com.ruoyi.yianlian.domain.vo.VpnLineSyncedUserVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * VPN用户表 数据层
 *
 * @author ruoyi
 */
public interface VpnUserMapper
{
    /**
     * 根据条件分页查询用户列表
     *
     * @param vpnUser 用户信息
     * @return 用户信息集合信息
     */
    public List<VpnUser> selectUserList(VpnUser vpnUser);

    /**
     * 通过用户名查询用户
     *
     * @param userName 用户名
     * @return 用户对象信息
     */
    public VpnUser selectUserByUserName(String userName);

    /**
     * 通过用户名和线路查询用户
     */
    public VpnUser selectUserByUserNameAndAppId(@Param("userName") String userName, @Param("appId") String appId);

    /**
     * 通过用户ID查询用户
     *
     * @param userId 用户ID
     * @return 用户对象信息
     */
    public VpnUser selectUserById(Long userId);

    /**
     * 通过本地用户ID和线路查询用户
     */
    public VpnUser selectUserByLocalUserIdAndAppId(@Param("localUserId") Long localUserId, @Param("appId") String appId);

    /**
     * 查询本地用户已同步的线路用户列表
     */
    public List<VpnUser> selectUsersByLocalUserId(Long localUserId);

    /**
     * 查询本地用户已授权线路 appId 列表
     */
    public List<String> selectDistinctAppIdsByLocalUserId(Long localUserId);

    /**
     * 新增用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    public int insertUser(VpnUser user);

    /**
     * 修改用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    public int updateUser(VpnUser user);

    /**
     * 仅更新用户登录信息（登录IP、登录时间），不触碰角色关联
     *
     * @param user 用户信息（userId、loginIp、loginDate）
     * @return 结果
     */
    public int updateUserLogin(VpnUser user);

    /**
     * 修改用户头像
     *
     * @param userName 用户名
     * @param avatar 头像地址
     * @return 结果
     */
    public int updateUserAvatar(@Param("userName") String userName, @Param("avatar") String avatar);

    /**
     * 重置用户密码
     *
     * @param userName 用户名
     * @param password 密码
     * @return 结果
     */
    public int resetUserPwd(@Param("userName") String userName, @Param("password") String password);

    /**
     * 通过用户ID删除用户
     *
     * @param userId 用户ID
     * @return 结果
     */
    public int deleteUserById(Long userId);

    /**
     * 批量删除用户信息
     *
     * @param userIds 需要删除的用户ID
     * @return 结果
     */
    public int deleteUserByIds(Long[] userIds);

    /**
     * 校验用户名称是否唯一
     *
     * @param userName 用户名称
     * @return 结果
     */
    public VpnUser checkUserNameUnique(@Param("userName") String userName, @Param("appId") String appId);

    /**
     * 校验手机号码是否唯一
     *
     * @param phonenumber 手机号码
     * @return 结果
     */
    public VpnUser checkPhoneUnique(String phonenumber);

    /**
     * 校验email是否唯一
     *
     * @param email 用户邮箱
     * @return 结果
     */
    public VpnUser checkEmailUnique(String email);

    /**
     * 查询线路下已关联本地用户的同步记录（按部门展示）
     */
    List<VpnLineSyncedUserVO> selectSyncedLocalUsersByAppId(@Param("appId") String appId);
}
