package com.ruoyi.yianlian.domain.vo;

import lombok.Data;

@Data
public class VpnLocalUserSyncLineResult
{
    private String appId;
    private String appName;
    private boolean success;
    private boolean updated;
    private String message;
}
