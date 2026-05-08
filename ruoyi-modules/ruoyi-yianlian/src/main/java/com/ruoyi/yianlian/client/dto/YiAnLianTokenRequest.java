package com.ruoyi.yianlian.client.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 易安联获取token请求
 */
@Data
@ApiModel("Yianlian Token请求实体")
public class YiAnLianTokenRequest implements Serializable
{

    @ApiModelProperty(value = "应用唯一id", example = "xxx")
    private String appId;

    @ApiModelProperty(value="应用密钥", example="xxx")
    private String appSecret;

}
