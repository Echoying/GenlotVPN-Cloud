package com.ruoyi.yianlian.client.dto;

import com.ruoyi.yianlian.client.YiAnLianBase.YiAnLianRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 重置密码请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("重置密码请求")
public class YiAnLianUserPasswordResetRequest extends YiAnLianRequest
{
    @ApiModelProperty(value = "用户账号", required = true)
    private String username;

    @ApiModelProperty(value = "老密码", required = true)
    private String oldPassword;

    @ApiModelProperty(value = "新密码", required = true)
    private String newPassword;
}
