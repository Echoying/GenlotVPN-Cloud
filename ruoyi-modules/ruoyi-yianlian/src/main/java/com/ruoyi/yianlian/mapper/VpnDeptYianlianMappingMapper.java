package com.ruoyi.yianlian.mapper;

import java.util.List;
import com.ruoyi.yianlian.domain.VpnDeptYianlianMapping;
import org.apache.ibatis.annotations.Param;

/**
 * VPN部门与易安联部门映射Mapper接口
 *
 * @author ruoyi
 */
public interface VpnDeptYianlianMappingMapper
{
    /**
     * 查询映射
     *
     * @param id 映射主键
     * @return 映射
     */
    public VpnDeptYianlianMapping selectVpnDeptYianlianMappingById(Long id);

    /**
     * 查询映射列表
     *
     * @param vpnDeptYianlianMapping 映射
     * @return 映射集合
     */
    public List<VpnDeptYianlianMapping> selectVpnDeptYianlianMappingList(VpnDeptYianlianMapping vpnDeptYianlianMapping);

    /**
     * 新增映射
     *
     * @param vpnDeptYianlianMapping 映射
     * @return 结果
     */
    public int insertVpnDeptYianlianMapping(VpnDeptYianlianMapping vpnDeptYianlianMapping);

    /**
     * 修改映射
     *
     * @param vpnDeptYianlianMapping 映射
     * @return 结果
     */
    public int updateVpnDeptYianlianMapping(VpnDeptYianlianMapping vpnDeptYianlianMapping);

    /**
     * 删除映射
     *
     * @param id 映射主键
     * @return 结果
     */
    public int deleteVpnDeptYianlianMappingById(Long id);

    /**
     * 批量删除映射
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteVpnDeptYianlianMappingByIds(Long[] ids);

    /**
     * 根据VPN部门ID查询映射
     *
     * @param vpnDeptId VPN部门ID
     * @return 映射
     */
    public VpnDeptYianlianMapping selectByVpnDeptId(Long vpnDeptId);

    /**
     * 根据易安联部门ID查询映射
     *
     * @param yianlianDeptId 易安联部门ID
     * @return 映射
     */
    public VpnDeptYianlianMapping selectByYianlianDeptId(String yianlianDeptId);

    /**
     * 根据VPN部门ID删除映射（逻辑删除）
     *
     * @param vpnDeptId VPN部门ID
     * @return 结果
     */
    public int deleteByVpnDeptId(Long vpnDeptId);

    /**
     * 根据部门ID查询映射列表
     *
     * @param deptId 部门ID
     * @return 映射列表
     */
    public List<VpnDeptYianlianMapping> selectByDeptId(Long deptId);

    /**
     * 根据部门ID和应用ID查询映射
     *
     * @param deptId 部门ID
     * @param appId 应用ID
     * @return 映射
     */
    public VpnDeptYianlianMapping selectByDeptIdAndAppId(@Param("deptId") Long deptId, @Param("appId") String appId);

    /**
     * 新增映射
     *
     * @param mapping 映射
     * @return 结果
     */
    public int insert(VpnDeptYianlianMapping mapping);

    /**
     * 根据部门ID删除映射
     *
     * @param deptId 部门ID
     * @return 结果
     */
    public int deleteByDeptId(Long deptId);

    /**
     * 根据部门ID数组批量删除映射
     *
     * @param deptIds 部门ID数组
     * @return 结果
     */
    public int deleteByDeptIds(Long[] deptIds);
}
