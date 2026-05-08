package com.ruoyi.yianlian.client.dto.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 易安联角色实体
 */
@Data
@ApiModel("角色实体")
public class YiAnLianRoleVO implements Serializable
{
    @ApiModelProperty(value = "角色ID")
    private String id;

    @ApiModelProperty(value = "角色名称", required = true)
    private String name;

    @ApiModelProperty(value = "角色描述")
    private String description;

    @ApiModelProperty(value = "应用组id列表")
    private List<Object> serviceGroups;

    @ApiModelProperty(value = "应用id列表")
    private List<Object> services;
}
