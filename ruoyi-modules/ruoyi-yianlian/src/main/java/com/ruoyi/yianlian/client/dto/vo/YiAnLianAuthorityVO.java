package com.ruoyi.yianlian.client.dto.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 易安联权限实体
 */
@Data
@ApiModel("权限实体")
@JsonIgnoreProperties(ignoreUnknown = true)
public class YiAnLianAuthorityVO implements Serializable
{
    @ApiModelProperty(value = "用户id")
    private String userId;

    @ApiModelProperty(value = "用户名")
    private String username;

    @ApiModelProperty(value = "角色信息")
    private List<IdNameVO> roleInfo;

    @ApiModelProperty(value = "应用列表")
    private List<IdNameVO> services;

    @ApiModelProperty(value = "应用组列表")
    private List<IdNameVO> serviceGroups;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IdNameVO implements Serializable
    {
        @ApiModelProperty(value = "id")
        private String id;

        @ApiModelProperty(value = "名称")
        private String name;
    }
}
