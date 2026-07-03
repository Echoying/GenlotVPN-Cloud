package com.ruoyi.yianlian.service.vpn.impl;

import com.ruoyi.common.core.constant.UserConstants;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.domain.VpnDept;
import com.ruoyi.yianlian.domain.VpnRole;
import com.ruoyi.yianlian.domain.VpnUser;
import com.ruoyi.yianlian.domain.VpnUserRole;
import com.ruoyi.yianlian.domain.VpnUserYianlianMapping;
import com.ruoyi.yianlian.mapper.VpnUserMapper;
import com.ruoyi.yianlian.mapper.VpnUserRoleMapper;
import com.ruoyi.yianlian.service.vpn.IVpnUserYianlianMappingService;
import com.ruoyi.yianlian.service.vpn.IVpnDeptService;
import com.ruoyi.yianlian.service.vpn.IVpnRoleService;
import com.ruoyi.yianlian.service.vpn.IVpnUserService;
import com.ruoyi.yianlian.service.vpn.VpnUserYiAnLianSyncService;
import com.ruoyi.yianlian.utils.AesUtils;
import com.ruoyi.common.security.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * VPN用户 业务层处理
 *
 * @author ruoyi
 */
@Service
public class VpnUserServiceImpl implements IVpnUserService
{
    private static final Logger log = LoggerFactory.getLogger(VpnUserServiceImpl.class);

    @Autowired
    private VpnUserMapper userMapper;

    @Autowired
    private VpnUserRoleMapper userRoleMapper;

    @Autowired
    private VpnUserYiAnLianSyncService userYiAnLianSyncService;

    @Autowired
    private IVpnUserYianlianMappingService userMappingService;

    @Autowired
    private IVpnDeptService deptService;

    @Autowired
    private IVpnRoleService roleService;

    @Autowired
    private AesUtils aesUtils;

    /**
     * 根据条件分页查询用户列表
     *
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    public List<VpnUser> selectUserList(VpnUser user)
    {
        return userMapper.selectUserList(user);
    }

    /**
     * 通过用户名查询用户
     *
     * @param userName 用户名
     * @return 用户对象信息
     */
    @Override
    public VpnUser selectUserByUserName(String userName)
    {
        return userMapper.selectUserByUserName(userName);
    }

    @Override
    public VpnUser selectUserByUserNameAndAppId(String userName, String appId)
    {
        return userMapper.selectUserByUserNameAndAppId(userName, appId);
    }

    /**
     * 通过用户ID查询用户
     *
     * @param userId 用户ID
     * @return 用户对象信息
     */
    @Override
    public VpnUser selectUserById(Long userId)
    {
        return userMapper.selectUserById(userId);
    }

    @Override
    public List<VpnUser> selectUsersByLocalUserId(Long localUserId)
    {
        return userMapper.selectUsersByLocalUserId(localUserId);
    }

    /**
     * 查询用户所属角色组
     *
     * @param userName 用户名
     * @return 结果
     */
    @Override
    public String selectUserRoleGroup(String userName)
    {
        List<VpnRole> list = selectUserById(selectUserByUserName(userName).getUserId()).getRoles();
        if (CollectionUtils.isEmpty(list))
        {
            return StringUtils.EMPTY;
        }
        return list.stream().map(VpnRole::getRoleName).collect(Collectors.joining(","));
    }

    /**
     * 校验用户名称是否唯一
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean checkUserNameUnique(VpnUser user)
    {
        Long userId = StringUtils.isNull(user.getUserId()) ? -1L : user.getUserId();
        VpnUser info = userMapper.checkUserNameUnique(user.getUserName(), user.getAppId());
        if (StringUtils.isNotNull(info) && info.getUserId().longValue() != userId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验手机号码是否唯一
     *
     * @param user 用户信息
     * @return
     */
    @Override
    public boolean checkPhoneUnique(VpnUser user)
    {
        Long userId = StringUtils.isNull(user.getUserId()) ? -1L : user.getUserId();
        VpnUser info = userMapper.checkPhoneUnique(user.getPhonenumber());
        if (StringUtils.isNotNull(info) && info.getUserId().longValue() != userId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验email是否唯一
     *
     * @param user 用户信息
     * @return
     */
    @Override
    public boolean checkEmailUnique(VpnUser user)
    {
        Long userId = StringUtils.isNull(user.getUserId()) ? -1L : user.getUserId();
        VpnUser info = userMapper.checkEmailUnique(user.getEmail());
        if (StringUtils.isNotNull(info) && info.getUserId().longValue() != userId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 新增保存用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertUser(VpnUser user)
    {
        // 新增用户信息
        int rows = userMapper.insertUser(user);
        // 新增用户与角色管理
        insertUserRole(user);
        return rows;
    }

    /**
     * 注册用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean registerUser(VpnUser user)
    {
        return userMapper.insertUser(user) > 0;
    }

    /**
     * 修改保存用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateUser(VpnUser user)
    {
        refreshUserRoleRelations(user);
        return userMapper.updateUser(user);
    }

    /**
     * 仅记录登录信息（登录IP、登录时间），不触碰角色关联
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int updateUserLogin(VpnUser user)
    {
        return userMapper.updateUserLogin(user);
    }

    /**
     * 用户授权角色
     *
     * @param userId 用户ID
     * @param roleIds 角色组
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertUserAuth(Long userId, Long[] roleIds)
    {
        VpnUser user = userMapper.selectUserById(userId);
        if (user == null)
        {
            throw new ServiceException("用户不存在");
        }
        user.setRoleIds(roleIds);
        validateUserRoleAppId(user);
        userRoleMapper.deleteUserRoleByUserId(userId);
        insertUserRole(userId, roleIds);
        if (!userYiAnLianSyncService.syncUserRoles(user))
        {
            throw new ServiceException("同步易安联用户角色失败");
        }
    }

    /**
     * 修改用户状态
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int updateUserStatus(VpnUser user)
    {
        return userMapper.updateUser(user);
    }

    /**
     * 修改用户基本信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int updateUserProfile(VpnUser user)
    {
        return userMapper.updateUser(user);
    }

    /**
     * 修改用户头像
     *
     * @param userName 用户名
     * @param avatar 头像地址
     * @return 结果
     */
    @Override
    public boolean updateUserAvatar(String userName, String avatar)
    {
        return userMapper.updateUserAvatar(userName, avatar) > 0;
    }

    /**
     * 重置用户密码
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int resetPwd(VpnUser user)
    {
        return userMapper.updateUser(user);
    }

    /**
     * 重置用户密码
     *
     * @param userName 用户名
     * @param password 密码
     * @return 结果
     */
    @Override
    public int resetUserPwd(String userName, String password)
    {
        return userMapper.resetUserPwd(userName, password);
    }

    /**
     * 新增用户角色信息
     *
     * @param user 用户对象
     */
    public void insertUserRole(VpnUser user)
    {
        List<Long> roleIdList = user.getRoleIdList();
        if (roleIdList == null || roleIdList.isEmpty())
        {
            return;
        }
        validateUserRoleAppId(user);
        this.insertUserRole(user.getUserId(), roleIdList.toArray(new Long[0]));
    }

    /**
     * 刷新用户角色关联：请求携带 roleIds 时先删后插；未携带时保留原有关联
     */
    private void refreshUserRoleRelations(VpnUser user)
    {
        Long userId = user.getUserId();
        if (userId == null)
        {
            return;
        }
        List<Long> roleIdList = user.getRoleIdList();
        if (roleIdList == null)
        {
            return;
        }
        validateUserRoleAppId(user);
        userRoleMapper.deleteUserRoleByUserId(userId);
        if (!roleIdList.isEmpty())
        {
            insertUserRole(userId, roleIdList.toArray(new Long[0]));
        }
    }

    /**
     * 新增用户角色信息
     *
     * @param userId 用户ID
     * @param roleIds 角色组
     */
    public void insertUserRole(Long userId, Long[] roleIds)
    {
        if (StringUtils.isNotEmpty(roleIds))
        {
            // 新增用户与角色管理
            List<VpnUserRole> list = new ArrayList<VpnUserRole>(roleIds.length);
            for (Long roleId : roleIds)
            {
                VpnUserRole ur = new VpnUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                list.add(ur);
            }
            userRoleMapper.batchUserRole(list);
        }
    }

    /**
     * 通过用户ID删除用户
     *
     * @param userId 用户ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteUserById(Long userId)
    {
        // 删除用户与角色关联
        userRoleMapper.deleteUserRoleByUserId(userId);
        return userMapper.deleteUserById(userId);
    }

    /**
     * 批量删除用户信息
     *
     * @param userIds 需要删除的用户ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteUserByIds(Long[] userIds)
    {
        for (Long userId : userIds)
        {
            checkUserAllowed(new VpnUser(userId));
        }
        // 删除用户与角色关联
        userRoleMapper.deleteUserRole(userIds);
        return userMapper.deleteUserByIds(userIds);
    }

    /**
     * 导入用户数据
     *
     * @param userList 用户数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    @Override
    public String importUser(List<VpnUser> userList, Boolean isUpdateSupport, String operName)
    {
        if (StringUtils.isNull(userList) || userList.size() == 0)
        {
            throw new ServiceException("导入用户数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        String password = "123456";
        for (VpnUser user : userList)
        {
            try
            {
                // 验证是否存在这个用户
                VpnUser u = userMapper.selectUserByUserName(user.getUserName());
                if (StringUtils.isNull(u))
                {
                    user.setPassword(SecurityUtils.encryptPassword(password));
                    user.setCreateBy(operName);
                    this.insertUser(user);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、账号 " + user.getUserName() + " 导入成功");
                }
                else if (isUpdateSupport)
                {
                    user.setUpdateBy(operName);
                    this.updateUser(user);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、账号 " + user.getUserName() + " 更新成功");
                }
                else
                {
                    failureNum++;
                    failureMsg.append("<br/>" + failureNum + "、账号 " + user.getUserName() + " 已存在");
                }
            }
            catch (Exception e)
            {
                failureNum++;
                String msg = "<br/>" + failureNum + "、账号 " + user.getUserName() + " 导入失败：";
                failureMsg.append(msg + e.getMessage());
                log.error(msg, e);
            }
        }
        if (failureNum > 0)
        {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            throw new ServiceException(failureMsg.toString());
        }
        else
        {
            successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
        }
        return successMsg.toString();
    }

    /**
     * 校验用户是否允许操作（VPN 用户无内置超级管理员，不做限制）
     *
     * @param user 用户信息
     */
    @Override
    public void checkUserAllowed(VpnUser user)
    {
        // VPN 用户体系与系统用户不同，不存在 userId=1 的超级管理员保护
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertUserWithSync(VpnUser user, String plainPassword)
    {
        validateDeptAppId(user);
        validateUserRoleAppId(user);
        int ret = insertUser(user);
        if (ret > 0 && !userYiAnLianSyncService.syncOnAdd(user, plainPassword))
        {
            throw new ServiceException("同步易安联用户失败");
        }
        return ret;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateUserWithSync(VpnUser user)
    {
        validateDeptAppId(user);
        VpnUser dbUser = userMapper.selectUserById(user.getUserId());
        if (dbUser != null && StringUtils.isEmpty(user.getAppId()))
        {
            user.setAppId(dbUser.getAppId());
        }
        int ret = updateUser(user);
        if (ret > 0 && !userYiAnLianSyncService.syncOnEdit(user))
        {
            throw new ServiceException("同步易安联用户失败");
        }
        return ret;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteUserByIdsWithSync(Long[] userIds)
    {
        for (Long userId : userIds)
        {
            checkUserAllowed(new VpnUser(userId));
        }
        for (Long userId : userIds)
        {
            VpnUser oldUser = userMapper.selectUserById(userId);
            if (oldUser == null)
            {
                continue;
            }
            if (StringUtils.isNotEmpty(oldUser.getAppId()))
            {
                VpnUserYianlianMapping mapping = userMappingService.selectByUserIdAndAppId(userId, oldUser.getAppId());
                if (!userYiAnLianSyncService.syncOnDelete(mapping))
                {
                    throw new ServiceException("同步易安联用户失败");
                }
                userMappingService.deleteByUserId(userId);
            }
        }
        return deleteUserByIds(userIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int resetPwdWithSync(VpnUser user, String plainPassword)
    {
        VpnUser dbUser = userMapper.selectUserById(user.getUserId());
        if (dbUser == null)
        {
            return 0;
        }
        String oldEncryptedPwd = dbUser.getEncryptedPwd();
        if (StringUtils.isEmpty(user.getAppId()))
        {
            user.setAppId(dbUser.getAppId());
        }
        user.setUserName(dbUser.getUserName());
        int ret = resetPwd(user);
        if (ret > 0 && !userYiAnLianSyncService.syncOnResetPassword(dbUser, plainPassword, oldEncryptedPwd))
        {
            throw new ServiceException("同步易安联用户失败");
        }
        return ret;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateUserStatusWithSync(VpnUser user)
    {
        VpnUser dbUser = userMapper.selectUserById(user.getUserId());
        if (dbUser == null)
        {
            return 0;
        }
        if (StringUtils.isEmpty(user.getAppId()))
        {
            user.setAppId(dbUser.getAppId());
        }
        int ret = updateUserStatus(user);
        if (ret > 0 && !userYiAnLianSyncService.syncOnChangeStatus(dbUser, user.getStatus()))
        {
            throw new ServiceException("同步易安联用户失败");
        }
        return ret;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePasswordWithSync(VpnUser vpnUser, String oldPlainPassword, String newPlainPassword)
    {
        VpnUser updateUser = new VpnUser();
        updateUser.setUserId(vpnUser.getUserId());
        updateUser.setPassword(SecurityUtils.encryptPassword(newPlainPassword));
        updateUser.setEncryptedPwd(aesUtils.encrypt(newPlainPassword));
        String oldEncryptedPwd = vpnUser.getEncryptedPwd();
        resetPwd(updateUser);
        if (StringUtils.isNotEmpty(vpnUser.getAppId())
                && !userYiAnLianSyncService.syncOnResetPassword(vpnUser, newPlainPassword, oldEncryptedPwd))
        {
            throw new ServiceException("同步易安联用户失败");
        }
    }

    private void validateDeptAppId(VpnUser user)
    {
        if (user.getDeptId() == null || StringUtils.isEmpty(user.getAppId()))
        {
            return;
        }
        VpnDept dept = deptService.selectDeptById(user.getDeptId());
        if (dept == null)
        {
            throw new ServiceException("归属部门不存在");
        }
        if (StringUtils.isNotEmpty(dept.getAppId()) && !user.getAppId().equals(dept.getAppId()))
        {
            throw new ServiceException("归属部门与当前线路不一致");
        }
    }

    /**
     * 校验分配的角色必须属于用户所在线路
     */
    private void validateUserRoleAppId(VpnUser user)
    {
        List<Long> roleIdList = user != null ? user.getRoleIdList() : null;
        if (roleIdList == null || roleIdList.isEmpty())
        {
            return;
        }
        String appId = user.getAppId();
        if (StringUtils.isEmpty(appId) && user.getUserId() != null)
        {
            VpnUser dbUser = userMapper.selectUserById(user.getUserId());
            if (dbUser != null)
            {
                appId = dbUser.getAppId();
            }
        }
        if (StringUtils.isEmpty(appId))
        {
            return;
        }
        for (Long roleId : roleIdList)
        {
            VpnRole role = roleService.selectRoleById(roleId);
            if (role == null)
            {
                throw new ServiceException("角色不存在");
            }
            if (StringUtils.isNotEmpty(role.getAppId()) && !appId.equals(role.getAppId()))
            {
                throw new ServiceException("角色「" + role.getRoleName() + "」与用户所属线路不一致");
            }
        }
    }
}
