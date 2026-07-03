package com.ruoyi.vpn.auth.domain;

import java.io.Serializable;

/**
 * VPN 单会话索引（Redis 存储对象）
 */
public class VpnSessionIndex implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String tokenId;

    public String getTokenId()
    {
        return tokenId;
    }

    public void setTokenId(String tokenId)
    {
        this.tokenId = tokenId;
    }
}
