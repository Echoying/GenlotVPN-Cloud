package com.ruoyi.yianlian.service.vpn;

import com.ruoyi.yianlian.domain.vo.VpnOfflineLoginExportRequest;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 离线登录导出服务
 */
public interface IVpnOfflineLoginExportService
{
    List<Map<String, Object>> listSelectableLines(Long localUserId);

    void exportZip(HttpServletResponse response, VpnOfflineLoginExportRequest request);
}
