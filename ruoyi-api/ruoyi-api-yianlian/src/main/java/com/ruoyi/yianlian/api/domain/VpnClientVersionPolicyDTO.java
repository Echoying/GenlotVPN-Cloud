package com.ruoyi.yianlian.api.domain;

import java.io.Serializable;

/**
 * VPN 客户端版本策略 DTO（Feign 传输）
 *
 * @author ruoyi
 */
public class VpnClientVersionPolicyDTO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 是否启用（0关 1开） */
    private String enabled;

    /** 最低客户端版本 x.y.z */
    private String minVersion;

    /** Windows 下载链接 */
    private String downloadUrlWindows;

    /** macOS 下载链接 */
    private String downloadUrlMacos;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getEnabled()
    {
        return enabled;
    }

    public void setEnabled(String enabled)
    {
        this.enabled = enabled;
    }

    public String getMinVersion()
    {
        return minVersion;
    }

    public void setMinVersion(String minVersion)
    {
        this.minVersion = minVersion;
    }

    public String getDownloadUrlWindows()
    {
        return downloadUrlWindows;
    }

    public void setDownloadUrlWindows(String downloadUrlWindows)
    {
        this.downloadUrlWindows = downloadUrlWindows;
    }

    public String getDownloadUrlMacos()
    {
        return downloadUrlMacos;
    }

    public void setDownloadUrlMacos(String downloadUrlMacos)
    {
        this.downloadUrlMacos = downloadUrlMacos;
    }
}
