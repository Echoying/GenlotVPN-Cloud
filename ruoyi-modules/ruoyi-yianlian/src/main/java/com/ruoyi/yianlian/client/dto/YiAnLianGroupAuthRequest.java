package com.ruoyi.yianlian.client.dto;

import com.ruoyi.yianlian.client.YiAnLianBase.YiAnLianRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 易安联授予组织权限请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("授予组织权限请求")
public class YiAnLianGroupAuthRequest extends YiAnLianRequest
{
    @ApiModelProperty(value = "组织id", required = true, example = "xxx")
    private String groupId;

    @ApiModelProperty(value = "角色id", example = "xxx")
    private String roleId;
    @ApiModelProperty(value = "应用id", example = "xxx")
    private String serviceId;

    @ApiModelProperty(value = "应用组id，不能为0", example = "xxx")
    private String serviceGroupId;
}
