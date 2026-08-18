package com.ruoyi.vpn.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.vpn.auth.tcp.TcpRpcDispatcher;
import com.ruoyi.vpn.auth.util.ClientVersionComparator;
import com.ruoyi.vpn.protocol.ClientVersionReject;
import com.ruoyi.yianlian.api.RemoteVpnClientVersionService;
import com.ruoyi.yianlian.api.domain.VpnClientVersionPolicyDTO;

/**
 * 云端 LOGIN 客户端最低版本门禁。
 */
@Service
public class VpnClientVersionGateService
{
    private static final Logger log = LoggerFactory.getLogger(VpnClientVersionGateService.class);

    @Autowired
    private RemoteVpnClientVersionService remoteVpnClientVersionService;

    /**
     * @return null 表示放行；非 null 为应返回的 RpcResult
     */
    public TcpRpcDispatcher.RpcResult check(String clientVersion, String clientPlatform)
    {
        R<VpnClientVersionPolicyDTO> r;
        try
        {
            r = remoteVpnClientVersionService.getPolicy(SecurityConstants.INNER);
        }
        catch (Exception e)
        {
            log.warn("获取客户端版本策略失败，放行登录: {}", e.getMessage());
            return null;
        }
        if (r == null || r.getCode() != R.SUCCESS || r.getData() == null)
        {
            String msg = r != null ? r.getMsg() : "null";
            log.warn("获取客户端版本策略失败，放行登录: {}", msg);
            return null;
        }
        VpnClientVersionPolicyDTO p = r.getData();
        if (!"1".equals(p.getEnabled()))
        {
            return null;
        }
        String min = p.getMinVersion() == null ? "" : p.getMinVersion().trim();
        String cv = clientVersion == null ? "" : clientVersion.trim();
        boolean reject = !ClientVersionComparator.isValid(cv)
                || !ClientVersionComparator.isValid(min)
                || ClientVersionComparator.isLower(cv, min);
        if (!reject)
        {
            return null;
        }
        String url = resolveUrl(p, clientPlatform);
        String msg = "客户端版本过低，请升级至 " + min + " 或以上";
        ClientVersionReject data = ClientVersionReject.newBuilder()
                .setMinVersion(min)
                .setDownloadUrl(url == null ? "" : url)
                .setClientVersion(cv)
                .build();
        return TcpRpcDispatcher.RpcResult.fail(426, msg, data);
    }

    private String resolveUrl(VpnClientVersionPolicyDTO p, String platform)
    {
        if ("windows".equalsIgnoreCase(platform))
        {
            return p.getDownloadUrlWindows();
        }
        if ("macos".equalsIgnoreCase(platform))
        {
            return p.getDownloadUrlMacos();
        }
        return "";
    }
}
