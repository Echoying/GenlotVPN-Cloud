package com.ruoyi.yianlian.service;

import com.ruoyi.yianlian.domain.YalDeptAuth;
import com.ruoyi.yianlian.domain.vo.ServiceTreeSelect;

import java.util.List;

/**
 * 部门授权 服务层
 *
 * @author ruoyi
 */
public interface IYalDeptAuthService
{
    /**
     * 查询部门授权信息
     *
     * @param id 主键ID
     * @return 部门授权信息
     */
    public YalDeptAuth selectYalDeptAuthById(Long id);

    /**
     * 查询部门授权列表
     *
     * @param yalDeptAuth 部门授权信息
     * @return 部门授权集合
     */
    public List<YalDeptAuth> selectYalDeptAuthList(YalDeptAuth yalDeptAuth);

    /**
     * 根据部门ID查询授权列表
     *
     * @param deptId 部门ID
     * @return 部门授权集合
     */
    public List<YalDeptAuth> selectByDeptId(Long deptId);

    /**
     * 根据部门ID与线路查询授权列表
     */
    public List<YalDeptAuth> selectByDeptIdAndLineId(Long deptId, String lineId);

    /**
     * 新增部门授权
     *
     * @param yalDeptAuth 部门授权信息
     * @return 结果
     */
    public int insertYalDeptAuth(YalDeptAuth yalDeptAuth);

    /**
     * 修改部门授权
     *
     * @param yalDeptAuth 部门授权信息
     * @return 结果
     */
    public int updateYalDeptAuth(YalDeptAuth yalDeptAuth);

    /**
     * 批量删除部门授权
     *
     * @param ids 需要删除的主键ID
     * @return 结果
     */
    public int deleteYalDeptAuthByIds(Long[] ids);

    /**
     * 删除部门授权
     *
     * @param id 主键ID
     * @return 结果
     */
    public int deleteYalDeptAuthById(Long id);

    /**
     * 批量保存部门授权（先删后插）
     *
     * @param deptId 部门ID
     * @param authList 授权列表
     * @return 结果
     */
    public int batchSaveDeptAuth(Long deptId, String lineId, List<YalDeptAuth> authList);

    /**
     * 构建应用服务树（应用组为父节点，应用为子节点）
     *
     * @param appId 线路appId，为空则返回所有
     * @return 树结构
     */
    public List<ServiceTreeSelect> buildServiceTree(String appId);
}
