package com.ruoyi.yianlian.service.vpn.impl;

import com.ruoyi.common.core.constant.UserConstants;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.domain.VpnRole;
import com.ruoyi.yianlian.domain.VpnRoleYianlianMapping;
import com.ruoyi.yianlian.mapper.VpnRoleMapper;
import com.ruoyi.yianlian.mapper.VpnUserRoleMapper;
import com.ruoyi.yianlian.service.IVpnRoleYianlianMappingService;
import com.ruoyi.yianlian.service.vpn.IVpnRoleService;
import com.ruoyi.yianlian.service.vpn.VpnRoleYiAnLianSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * VPN角色 业务层处理
 *
 * @author ruoyi
 */
@Service
public class VpnRoleServiceImpl implements IVpnRoleService
{
    @Autowired
    private VpnRoleMapper roleMapper;

    @Autowired
    private VpnUserRoleMapper userRoleMapper;

    @Autowired
    private VpnRoleYiAnLianSyncService roleYiAnLianSyncService;

    @Autowired
    private IVpnRoleYianlianMappingService mappingService;

    /**
     * 根据条件分页查询角色数据
     *
     * @param role 角色信息
     * @return 角色数据集合信息
     */
    @Override
    public List<VpnRole> selectRoleList(VpnRole role)
    {
        return roleMapper.selectRoleList(role);
    }

    /**
     * 根据用户ID查询角色
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    @Override
    public List<VpnRole> selectRolesByUserId(Long userId)
    {
        List<VpnRole> userRoles = roleMapper.selectRolePermissionByUserId(userId);
        List<VpnRole> roles = selectRoleAll();
        for (VpnRole role : roles)
        {
            for (VpnRole userRole : userRoles)
            {
                if (role.getRoleId().longValue() == userRole.getRoleId().longValue())
                {
                    role.setFlag(true);
                    break;
                }
            }
        }
        return roles;
    }

    @Override
    public List<VpnRole> selectUserRolesByUserId(Long userId)
    {
        return roleMapper.selectRolePermissionByUserId(userId);
    }

    /**
     * 根据用户ID查询权限
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    @Override
    public Set<String> selectRolePermissionByUserId(Long userId)
    {
        List<VpnRole> perms = roleMapper.selectRolePermissionByUserId(userId);
        Set<String> permsSet = new HashSet<>();
        for (VpnRole perm : perms)
        {
            if (StringUtils.isNotNull(perm))
            {
                if (StringUtils.isNotEmpty(perm.getRoleKey()))
                {
                    permsSet.add(perm.getRoleKey());
                }
                else
                {
                    permsSet.add(perm.getRoleName());
                }
            }
        }
        return permsSet;
    }

    /**
     * 查询所有角色
     *
     * @return 角色列表
     */
    @Override
    public List<VpnRole> selectRoleAll()
    {
        return selectRoleList(new VpnRole());
    }

    /**
     * 根据用户ID获取角色选择框列表
     *
     * @param userId 用户ID
     * @return 选中角色ID列表
     */
    @Override
    public List<Long> selectRoleListByUserId(Long userId)
    {
        return roleMapper.selectRoleListByUserId(userId);
    }

    /**
     * 通过角色ID查询角色
     *
     * @param roleId 角色ID
     * @return 角色对象信息
     */
    @Override
    public VpnRole selectRoleById(Long roleId)
    {
        return roleMapper.selectRoleById(roleId);
    }

    /**
     * 校验角色名称是否唯一
     *
     * @param role 角色信息
     * @return 结果
     */
    @Override
    public boolean checkRoleNameUnique(VpnRole role)
    {
        Long roleId = StringUtils.isNull(role.getRoleId()) ? -1L : role.getRoleId();
        VpnRole info = roleMapper.checkRoleNameUnique(role.getRoleName(), role.getAppId());
        if (StringUtils.isNotNull(info) && info.getRoleId().longValue() != roleId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public boolean checkRoleKeyUnique(VpnRole role)
    {
        if (StringUtils.isEmpty(role.getRoleKey()))
        {
            return UserConstants.UNIQUE;
        }
        Long roleId = StringUtils.isNull(role.getRoleId()) ? -1L : role.getRoleId();
        VpnRole info = roleMapper.checkRoleKeyUnique(role.getRoleKey(), role.getAppId());
        if (StringUtils.isNotNull(info) && info.getRoleId().longValue() != roleId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 通过角色ID查询角色使用数量
     *
     * @param roleId 角色ID
     * @return 结果
     */
    @Override
    public int countUserRoleByRoleId(Long roleId)
    {
        return userRoleMapper.countUserRoleByRoleId(roleId);
    }

    /**
     * 新增保存角色信息
     *
     * @param role 角色信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertRole(VpnRole role)
    {
        return roleMapper.insertRole(role);
    }

    /**
     * 修改保存角色信息
     *
     * @param role 角色信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateRole(VpnRole role)
    {
        return roleMapper.updateRole(role);
    }

    /**
     * 修改角色状态
     *
     * @param role 角色信息
     * @return 结果
     */
    @Override
    public int updateRoleStatus(VpnRole role)
    {
        return roleMapper.updateRole(role);
    }

    /**
     * 通过角色ID删除角色
     *
     * @param roleId 角色ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRoleById(Long roleId)
    {
        return roleMapper.deleteRoleById(roleId);
    }

    /**
     * 批量删除角色信息
     *
     * @param roleIds 需要删除的角色ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRoleByIds(Long[] roleIds)
    {
        for (Long roleId : roleIds)
        {
            VpnRole role = selectRoleById(roleId);
            if (countUserRoleByRoleId(roleId) > 0)
            {
                throw new ServiceException(String.format("%1$s已分配,不能删除", role.getRoleName()));
            }
        }
        return roleMapper.deleteRoleByIds(roleIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertRoleWithSync(VpnRole role)
    {
        int ret = insertRole(role);
        if (ret > 0 && !roleYiAnLianSyncService.syncOnAdd(role))
        {
            throw new ServiceException("同步易安联角色失败");
        }
        return ret;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateRoleWithSync(VpnRole role)
    {
        int ret = updateRole(role);
        if (ret > 0 && !roleYiAnLianSyncService.syncOnEdit(role))
        {
            throw new ServiceException("同步易安联角色失败");
        }
        return ret;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRoleByIdsWithSync(Long[] roleIds)
    {
        for (Long roleId : roleIds)
        {
            VpnRole role = selectRoleById(roleId);
            if (countUserRoleByRoleId(roleId) > 0)
            {
                throw new ServiceException(String.format("%1$s已分配,不能删除", role.getRoleName()));
            }
        }
        for (Long roleId : roleIds)
        {
            VpnRole oldRole = roleMapper.selectRoleById(roleId);
            if (oldRole == null)
            {
                continue;
            }
            if (StringUtils.isNotEmpty(oldRole.getAppId()))
            {
                VpnRoleYianlianMapping mapping = mappingService.selectByRoleIdAndAppId(roleId, oldRole.getAppId());
                if (!roleYiAnLianSyncService.syncOnDelete(mapping))
                {
                    throw new ServiceException("同步易安联角色失败");
                }
                mappingService.deleteByRoleId(roleId);
            }
        }
        return roleMapper.deleteRoleByIds(roleIds);
    }
}
