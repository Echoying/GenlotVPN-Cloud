package com.ruoyi.yianlian.service.vpn;

import com.ruoyi.yianlian.domain.VpnService;
import java.util.List;

public interface IVpnServiceService
{
    List<VpnService> selectServiceList(VpnService service);
    VpnService selectServiceById(Long id);
    int insertService(VpnService service);
    int updateService(VpnService service);
    int deleteServiceById(Long id);
    int deleteServiceByIds(Long[] ids);
}
