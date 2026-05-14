package com.ruoyi.yianlian.client.dto;

import lombok.Data;

@Data
public class YiAnLianUserSessionResponse
{
    private String id;
    private String username;
    private String name;
    private String loginIp;
    private String loginTime;
    private Integer onlineStatus;
}
