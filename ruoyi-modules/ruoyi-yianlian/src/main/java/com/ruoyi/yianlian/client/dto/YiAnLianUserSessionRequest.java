package com.ruoyi.yianlian.client.dto;

import com.ruoyi.yianlian.client.YiAnLianBase.YiAnLianRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class YiAnLianUserSessionRequest extends YiAnLianRequest
{
    private String id;
    private String username;
}
