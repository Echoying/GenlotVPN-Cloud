package com.ruoyi.yianlian.mapper;

import java.util.List;
import com.ruoyi.yianlian.domain.VpnLogininfor;

/**
 * VPN访问日志情况信息 数据层
 *
 * @author ruoyi
 */
public interface VpnLogininforMapper
{
    /**
     * 新增VPN登录日志
     *
     * @param logininfor 访问日志对象
     */
    public int insertLogininfor(VpnLogininfor logininfor);

    /**
     * 查询VPN登录日志集合
     *
     * @param logininfor 访问日志对象
     * @return 登录记录集合
     */
    public List<VpnLogininfor> selectLogininforList(VpnLogininfor logininfor);

    /**
     * 批量删除VPN登录日志
     *
     * @param infoIds 需要删除的登录日志ID
     * @return 结果
     */
    public int deleteLogininforByIds(Long[] infoIds);

    /**
     * 清空VPN登录日志
     *
     * @return 结果
     */
    public int cleanLogininfor();
}
