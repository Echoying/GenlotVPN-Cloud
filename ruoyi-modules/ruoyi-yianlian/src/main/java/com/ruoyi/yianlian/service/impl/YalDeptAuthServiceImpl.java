package com.ruoyi.yianlian.service.impl;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.yianlian.domain.YalDeptAuth;
import com.ruoyi.yianlian.domain.vo.ServiceTreeSelect;
import com.ruoyi.yianlian.mapper.YalDeptAuthMapper;
import com.ruoyi.yianlian.service.IYalDeptAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门授权 服务层实现
 *
 * @author ruoyi
 */
@Service
public class YalDeptAuthServiceImpl implements IYalDeptAuthService
{
    @Autowired
    private YalDeptAuthMapper yalDeptAuthMapper;

    /**
     * 查询部门授权信息
     *
     * @param id 主键ID
     * @return 部门授权信息
     */
    @Override
    public YalDeptAuth selectYalDeptAuthById(Long id)
    {
        return yalDeptAuthMapper.selectYalDeptAuthById(id);
    }

    /**
     * 查询部门授权列表
     *
     * @param yalDeptAuth 部门授权信息
     * @return 部门授权集合
     */
    @Override
    public List<YalDeptAuth> selectYalDeptAuthList(YalDeptAuth yalDeptAuth)
    {
        return yalDeptAuthMapper.selectYalDeptAuthList(yalDeptAuth);
    }

    /**
     * 根据部门ID查询授权列表
     *
     * @param deptId 部门ID
     * @return 部门授权集合
     */
    @Override
    public List<YalDeptAuth> selectByDeptId(Long deptId)
    {
        return yalDeptAuthMapper.selectYalDeptAuthByDeptId(deptId);
    }

    /**
     * 新增部门授权
     *
     * @param yalDeptAuth 部门授权信息
     * @return 结果
     */
    @Override
    public int insertYalDeptAuth(YalDeptAuth yalDeptAuth)
    {
        yalDeptAuth.setCreateTime(DateUtils.getNowDate());
        return yalDeptAuthMapper.insertYalDeptAuth(yalDeptAuth);
    }

    /**
     * 修改部门授权
     *
     * @param yalDeptAuth 部门授权信息
     * @return 结果
     */
    @Override
    public int updateYalDeptAuth(YalDeptAuth yalDeptAuth)
    {
        yalDeptAuth.setUpdateTime(DateUtils.getNowDate());
        return yalDeptAuthMapper.updateYalDeptAuth(yalDeptAuth);
    }

    /**
     * 批量删除部门授权
     *
     * @param ids 需要删除的主键ID
     * @return 结果
     */
    @Override
    public int deleteYalDeptAuthByIds(Long[] ids)
    {
        return yalDeptAuthMapper.deleteYalDeptAuthByIds(ids);
    }

    /**
     * 删除部门授权
     *
     * @param id 主键ID
     * @return 结果
     */
    @Override
    public int deleteYalDeptAuthById(Long id)
    {
        return yalDeptAuthMapper.deleteYalDeptAuthById(id);
    }

    /**
     * 批量保存部门授权（先删后插）
     *
     * @param deptId 部门ID
     * @param authList 授权列表
     * @return 结果
     */
    @Override
    @Transactional
    public int batchSaveDeptAuth(Long deptId, List<YalDeptAuth> authList)
    {
        // 先删除该部门的所有授权
        yalDeptAuthMapper.deleteYalDeptAuthByDeptId(deptId);

        // 批量插入新的授权
        if (authList != null && !authList.isEmpty())
        {
            for (YalDeptAuth auth : authList)
            {
                auth.setDeptId(deptId);
                auth.setCreateTime(DateUtils.getNowDate());
            }
            return yalDeptAuthMapper.batchInsertYalDeptAuth(authList);
        }
        return 0;
    }

    /**
     * 构建应用服务树（应用组为父节点，应用为子节点）
     *
     * @param appId 线路appId，为空则返回所有
     * @return 树结构
     */
    @Override
    public List<ServiceTreeSelect> buildServiceTree(String appId)
    {
        List<ServiceTreeSelect> treeList = new ArrayList<>();

        try
        {
            // TODO: 实现调用易安联API获取应用组和应用列表
            // 需要确定具体的API路径后实现
            // 示例：
            // List<AppGroup> groupList = openApiClient.postForList(appId, "/api/appgroups", params, AppGroup.class);
            // List<App> appList = openApiClient.postForList(appId, "/api/apps", params, App.class);
        }
        catch (Exception e)
        {
            // 记录日志或处理异常
            e.printStackTrace();
        }

        return treeList;
    }
}
