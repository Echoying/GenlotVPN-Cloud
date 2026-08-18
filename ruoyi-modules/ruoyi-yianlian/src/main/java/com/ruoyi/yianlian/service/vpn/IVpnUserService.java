package com.ruoyi.yianlian.service.vpn;

import com.ruoyi.yianlian.domain.VpnUser;

import java.util.List;

/**
 * VPN用户 业务层
 *
 * @author ruoyi
 */
public interface IVpnUserService
{
    /**
     * 根据条件分页查询用户列表
     *
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    public List<VpnUser> selectUserList(VpnUser user);

    /**
     * 通过用户名查询用户
     *
     * @param userName 用户名
     * @return 用户对象信息
     */
    public VpnUser selectUserByUserName(String userName);

    /**
     * 通过用户名查询用户（同名多线路会返回多条）
     */
    public List<VpnUser> selectUserListByUserName(String userName);

    /**
     * 通过用户名和线路查询用户
     */
    public VpnUser selectUserByUserNameAndAppId(String userName, String appId);

    /**
     * 通过用户ID查询用户
     *
     * @param userId 用户ID
     * @return 用户对象信息
     */
    public VpnUser selectUserById(Long userId);

    /**
     * 查询本地用户已同步的线路用户
     */
    public List<VpnUser> selectUsersByLocalUserId(Long localUserId);

    /**
     * 根据用户ID查询用户所属角色组
     *
     * @param userName 用户名
     * @return 结果
     */
    public String selectUserRoleGroup(String userName);

    /**
     * 校验用户名称是否唯一
     *
     * @param user 用户信息
     * @return 结果
     */
    public boolean checkUserNameUnique(VpnUser user);

    /**
     * 校验手机号码是否唯一
     *
     * @param user 用户信息
     * @return 结果
     */
    public boolean checkPhoneUnique(VpnUser user);

    /**
     * 校验email是否唯一
     *
     * @param user 用户信息
     * @return 结果
     */
    public boolean checkEmailUnique(VpnUser user);

    /**
     * 新增用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    public int insertUser(VpnUser user);

    /**
     * 注册用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    public boolean registerUser(VpnUser user);

    /**
     * 修改用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    public int updateUser(VpnUser user);

    /**
     * 仅记录登录信息（登录IP、登录时间），不触碰角色
     *
     * @param user 用户信息（userId、loginIp、loginDate）
     * @return 结果
     */
    public int updateUserLogin(VpnUser user);

    /**
     * 用户授权角色
     *
     * @param userId 用户ID
     * @param roleIds 角色组
     */
    public void insertUserAuth(Long userId, Long[] roleIds);

    /**
     * 修改用户状态
     *
     * @param user 用户信息
     * @return 结果
     */
    public int updateUserStatus(VpnUser user);

    /**
     * 修改用户基本信息
     *
     * @param user 用户信息
     * @return 结果
     */
    public int updateUserProfile(VpnUser user);

    /**
     * 修改用户头像
     *
     * @param userName 用户名
     * @param avatar 头像地址
     * @return 结果
     */
    public boolean updateUserAvatar(String userName, String avatar);

    /**
     * 重置用户密码
     *
     * @param user 用户信息
     * @return 结果
     */
    public int resetPwd(VpnUser user);

    /**
     * 重置用户密码
     *
     * @param userName 用户名
     * @param password 密码
     * @return 结果
     */
    public int resetUserPwd(String userName, String password);

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
     * 导入用户数据
     *
     * @param userList 用户数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    public String importUser(List<VpnUser> userList, Boolean isUpdateSupport, String operName);

    /**
     * 校验用户是否允许操作
     *
     * @param user 用户信息
     */
    public void checkUserAllowed(VpnUser user);

    /**
     * 新增用户并同步易安联
     */
    public int insertUserWithSync(VpnUser user, String plainPassword);

    /**
     * 修改用户并同步易安联
     */
    public int updateUserWithSync(VpnUser user);

    /**
     * 批量删除用户并同步易安联
     */
    public int deleteUserByIdsWithSync(Long[] userIds);

    /**
     * 重置密码并同步易安联
     */
    public int resetPwdWithSync(VpnUser user, String plainPassword);

    /**
     * 修改状态并同步易安联
     */
    public int updateUserStatusWithSync(VpnUser user);

    /**
     * 修改密码并同步易安联（VPN 用户自助）
     */
    public void updatePasswordWithSync(VpnUser vpnUser, String oldPlainPassword, String newPlainPassword);
}
