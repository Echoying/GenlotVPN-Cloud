package com.ruoyi.yianlian.mapper;

import java.util.List;
import com.ruoyi.yianlian.domain.YalDeptAuth;
import org.apache.ibatis.annotations.Param;

/**
 * 部门授权Mapper接口
 *
 * @author ruoyi
 */
public interface YalDeptAuthMapper
{
    /**
     * 查询部门授权
     *
     * @param id 部门授权主键
     * @return 部门授权
     */
    public YalDeptAuth selectYalDeptAuthById(Long id);

    /**
     * 查询部门授权列表
     *
     * @param yalDeptAuth 部门授权
     * @return 部门授权集合
     */
    public List<YalDeptAuth> selectYalDeptAuthList(YalDeptAuth yalDeptAuth);

    /**
     * 新增部门授权
     *
     * @param yalDeptAuth 部门授权
     * @return 结果
     */
    public int insertYalDeptAuth(YalDeptAuth yalDeptAuth);

    /**
     * 修改部门授权
     *
     * @param yalDeptAuth 部门授权
     * @return 结果
     */
    public int updateYalDeptAuth(YalDeptAuth yalDeptAuth);

    /**
     * 删除部门授权
     *
     * @param id 部门授权主键
     * @return 结果
     */
    public int deleteYalDeptAuthById(Long id);

    /**
     * 批量删除部门授权
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteYalDeptAuthByIds(Long[] ids);

    /**
     * 根据部门ID查询授权列表
     *
     * @param deptId 部门ID
     * @return 授权列表
     */
    public List<YalDeptAuth> selectYalDeptAuthByDeptId(Long deptId);

    /**
     * 根据部门ID删除授权
     *
     * @param deptId 部门ID
     * @return 结果
     */
    public int deleteYalDeptAuthByDeptId(Long deptId);

    /**
     * 批量新增部门授权
     *
     * @param list 部门授权列表
     * @return 结果
     */
    public int batchInsertYalDeptAuth(@Param("list") List<YalDeptAuth> list);
}
