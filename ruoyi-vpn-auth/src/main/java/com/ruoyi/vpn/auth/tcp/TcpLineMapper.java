package com.ruoyi.vpn.auth.tcp;

import java.util.List;
import java.util.Map;
import com.ruoyi.vpn.protocol.VpnLine;

/**
 * 线路 Map 与 Protobuf 转换
 */
public final class TcpLineMapper
{
    private TcpLineMapper()
    {
    }

    public static VpnLine toProto(Map<String, Object> map)
    {
        VpnLine.Builder builder = VpnLine.newBuilder();
        if (map.get("appId") != null)
        {
            builder.setAppId(String.valueOf(map.get("appId")));
        }
        if (map.get("appName") != null)
        {
            builder.setAppName(String.valueOf(map.get("appName")));
        }
        if (map.get("host") != null)
        {
            builder.setHost(String.valueOf(map.get("host")));
        }
        if (map.get("srvPort") != null)
        {
            builder.setSrvPort(String.valueOf(map.get("srvPort")));
        }
        if (map.get("spaPort") != null)
        {
            builder.setSpaPort(String.valueOf(map.get("spaPort")));
        }
        if (map.get("spaKey") != null)
        {
            builder.setSpaKey(String.valueOf(map.get("spaKey")));
        }
        return builder.build();
    }

    public static void addAllLines(com.ruoyi.vpn.protocol.ListPublicLinesResponse.Builder target,
            List<Map<String, Object>> lines)
    {
        for (Map<String, Object> line : lines)
        {
            target.addLines(toProto(line));
        }
    }

    public static void addAllLines(com.ruoyi.vpn.protocol.GetAuthorizedLinesResponse.Builder target,
            List<Map<String, Object>> lines)
    {
        for (Map<String, Object> line : lines)
        {
            target.addLines(toProto(line));
        }
    }
}
