package com.ruoyi.yianlian.client.dto.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * CS服务器配置
 */
@Data
@ApiModel("CS服务器配置")
public class ReqCsServerVO implements Serializable
{
    @ApiModelProperty(value = "协议 TCP UDP ANY", required = true)
    private String protocol;

    @ApiModelProperty(value = "协议类型 0 http/https 1 TCP/UDP/ANY", required = true)
    private String protocolType;

    @ApiModelProperty(value = "ip地址", required = true)
    private String ip;

    @ApiModelProperty(value = "端口,协议是ANY的,port必须为ANY", required = true)
    private String port;
}
