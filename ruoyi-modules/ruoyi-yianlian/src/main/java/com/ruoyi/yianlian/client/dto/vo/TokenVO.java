package com.ruoyi.yianlian.client.dto.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @ApiModelProperty(value="Token", example="enaccess-e7b0bdca4760c5dd41594ac761c17522")
    private String accessToken;

    @ApiModelProperty(value="状态", example="null")
    private String state;

    @ApiModelProperty(value="过期时间(秒)", example="7023")
    private Integer expireTime;

    @ApiModelProperty(value="刷新token", example="t66_1253e4945759b7bafa5bc6998d2968fc")
    private String refreshToken;

}
