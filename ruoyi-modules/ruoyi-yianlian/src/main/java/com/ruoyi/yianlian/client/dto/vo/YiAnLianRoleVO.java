package com.ruoyi.yianlian.client.dto.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 易安联角色实体
 * 注意：serviceGroups和services字段在不同场景下类型不同：
 * - 创建/修改时：List<String> (ID列表)
 * - 查询返回时：List<Object> (包含id和name的对象)
 */
@Data
@ApiModel("角色实体")
@JsonIgnoreProperties(ignoreUnknown = true)
public class YiAnLianRoleVO implements Serializable
{
    @ApiModelProperty(value = "角色ID")
    private String id;

    @ApiModelProperty(value = "角色名称", required = true)
    private String name;

    @ApiModelProperty(value = "角色描述")
    private String description;

  @ApiModelProperty(value = "应用组列表 - 创建/修改时传ID字符串列表，查询时返回对象列表")
    private List<Object> serviceGroups;

    @ApiModelProperty(value = "应用列表 - 创建/修改时传ID字符串列表，查询时返回对象列表")
    private List<Object> services;
}
