package com.ruoyi.yianlian.mapper;

import java.util.List;
import com.ruoyi.yianlian.domain.VpnUserYianlianMapping;
import org.apache.ibatis.annotations.Param;

/**
 * VPN用户与易安联用户映射Mapper接口
 *
 * @author ruoyi
 */
public interface VpnUserYianlianMappingMapper
{
    /**
     * 查询映射
     *
     * @param id 映射主键
     * @return 映射
     */
    public VpnUserYianlianMapping selectVpnUserYianlianMappingById(Long id);

    /**
     * 查询映射列表
     *
     * @param vpnUserYianlianMapping 映射
     * @return 映射集合
     */
    public List<VpnUserYianlianMapping> selectVpnUserYianlianMappingList(VpnUserYianlianMapping vpnUserYianlianMapping);

    /**
     * 新增映射
     *
     * @param vpnUserYianlianMapping 映射
     * @return 结果
     */
    public int insertVpnUserYianlianMapping(VpnUserYianlianMapping vpnUserYianlianMapping);

    /**
     * 修改映射
     *
     * @param vpnUserYianlianMapping 映射
     * @return 结果
     */
    public int updateVpnUserYianlianMapping(VpnUserYianlianMapping vpnUserYianlianMapping);

    /**
     * 删除映射
     *
     * @param id 映射主键
     * @return 结果
     */
    public int deleteVpnUserYianlianMappingById(Long id);

    /**
     * 批量删除映射
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteVpnUserYianlianMappingByIds(Long[] ids);

    /**
     * 根据VPN用户ID查询映射
     *
     * @param vpnUserId VPN用户ID
     * @return 映射
     */
    public VpnUserYianlianMapping selectByVpnUserId(Long vpnUserId);

    /**
     * 根据易安联用户ID查询映射
     *
     * @param yianlianUserId 易安联用户ID
     * @return 映射
     */
    public VpnUserYianlianMapping selectByYianlianUserId(String yianlianUserId);

    /**
     * 根据VPN用户ID删除映射（逻辑删除）
     *
     * @param vpnUserId VPN用户ID
     * @return 结果
     */
    public int deleteByVpnUserId(Long vpnUserId);

    /**
     * 根据用户ID查询映射列表
     *
     * @param userId 用户ID
     * @return 映射列表
     */
    public List<VpnUserYianlianMapping> selectByUserId(Long userId);

    /**
     * 根据用户ID和应用ID查询映射
     *
     * @param userId 用户ID
     * @param appId 应用ID
     * @return 映射
     */
    public VpnUserYianlianMapping selectByUserIdAndAppId(@Param("userId") Long userId, @Param("appId") String appId);

    /**
     * 新增映射
     *
     * @param mapping 映射
     * @return 结果
     */
    public int insert(VpnUserYianlianMapping mapping);

    /**
     * 根据用户ID删除映射
     *
     * @param userId 用户ID
     * @return 结果
     */
    public int deleteByUserId(Long userId);

    /**
     * 根据用户ID数组批量删除映射
     *
     * @param userIds 用户ID数组
     * @return 结果
     */
    public int deleteByUserIds(Long[] userIds);
}
