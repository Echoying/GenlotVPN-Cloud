package com.ruoyi.yianlian.client.dto.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 易安联token接口返回data
 */
@Data
@ApiModel("Yianlian Token实体")
public class TokenVO implements Serializable
{

    @ApiModelProperty(value="Token", example="xxx")
    private String accessToken;

    @ApiModelProperty(value="过期时间", example="7200")
    private Integer expiresIn;

    @ApiModelProperty(value="刷新token", example="xxx")
    private  String refreshToken;

}
