package com.ruoyi.yianlian.service;

import com.ruoyi.yianlian.domain.YalRoleAuth;
import com.ruoyi.yianlian.domain.vo.ServiceTreeSelect;

import java.util.List;

/**
 * 角色授权 服务层接口
 *
 * @author ruoyi
 */
public interface IYalRoleAuthService
{
    /**
     * 查询角色授权
     */
    public YalRoleAuth selectYalRoleAuthById(Long id);

    /**
     * 查询角色授权列表
     */
    public List<YalRoleAuth> selectYalRoleAuthList(YalRoleAuth yalRoleAuth);

    /**
     * 根据角色ID查询授权列表
     */
    public List<YalRoleAuth> selectByRoleId(Long roleId);

    /**
     * 新增角色授权
     */
    public int insertYalRoleAuth(YalRoleAuth yalRoleAuth);

    /**
     * 修改角色授权
     */
    public int updateYalRoleAuth(YalRoleAuth yalRoleAuth);

    /**
     * 删除角色授权信息
     */
    public int deleteYalRoleAuthByIds(Long[] ids);

    /**
     * 删除角色授权信息
     */
    public int deleteYalRoleAuthById(Long id);

    /**
     * 批量保存角色授权（先删后插），并同步给易安联
     */
    public int batchSaveRoleAuth(Long roleId, List<YalRoleAuth> authList);

    /**
     * 构建应用服务树
     */
    public List<ServiceTreeSelect> buildServiceTree(String appId);
}
