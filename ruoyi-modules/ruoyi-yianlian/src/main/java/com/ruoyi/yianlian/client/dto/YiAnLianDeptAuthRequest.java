package com.ruoyi.yianlian.client.dto;

import com.ruoyi.yianlian.client.YiAnLianBase.YiAnLianRequest;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class YiAnLianDeptAuthRequest extends YiAnLianRequest {

    @ApiModelProperty(value = "组织id", required = true, example = "xxx")
    private String groupId;

    @ApiModelProperty(value = "角色id", example = "xxx")
    private String roleId;

    @ApiModelProperty(value = "应用id", example = "xxx")
    private String serviceId;

    @ApiModelProperty(value = "应用组id", example = "xxx")
    private String serviceGroupId;
}
