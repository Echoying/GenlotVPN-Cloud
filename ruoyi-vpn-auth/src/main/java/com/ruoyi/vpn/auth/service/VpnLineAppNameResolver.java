package com.ruoyi.vpn.auth.service;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.api.RemoteVpnLineService;

/**
 * 按 app_id 解析线路名称（客户端未上报 app_name 时兜底）
 */
@Component
public class VpnLineAppNameResolver
{
    @Autowired
    private RemoteVpnLineService remoteVpnLineService;

    public String resolve(String appId, String reportedAppName)
    {
        if (StringUtils.isNotEmpty(reportedAppName))
        {
            return StringUtils.trim(reportedAppName);
        }
        if (StringUtils.isEmpty(appId))
        {
            return "";
        }
        R<List<Map<String, Object>>> result = remoteVpnLineService.listPublicLines(SecurityConstants.INNER);
        if (R.FAIL == result.getCode() || result.getData() == null)
        {
            return "";
        }
        for (Map<String, Object> line : result.getData())
        {
            if (appId.equals(line.get("appId")))
            {
                Object appName = line.get("appName");
                return appName == null ? "" : String.valueOf(appName);
            }
        }
        return "";
    }
}
