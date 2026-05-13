package com.ruoyi.yianlian.mapper;

import com.ruoyi.yianlian.domain.VpnService;
import java.util.List;

/**
 * VPN应用 数据层
 */
public interface VpnServiceMapper
{
    public List<VpnService> selectServiceList(VpnService service);
    public VpnService selectServiceById(Long id);
    public int insertService(VpnService service);
    public int updateService(VpnService service);
    public int deleteServiceById(Long id);
    public int deleteServiceByIds(Long[] ids);
}
