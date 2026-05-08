package com.ruoyi.yianlian.client.dto.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 易安联应用组实体
 */
@Data
@ApiModel("应用组实体")
public class YiAnLianServiceGroupVO implements Serializable
{
    @ApiModelProperty(value = "应用组ID")
    private String id;

    @ApiModelProperty(value = "应用组名称", required = true)
    private String name;

    @ApiModelProperty(value = "描述")
    private String description;

    @ApiModelProperty(value = "上级应用组id", required = true)
    private String parentId;

    @ApiModelProperty(value = "上级应用分组路径", required = true)
    private String path;

    @ApiModelProperty(value = "应用组key")
    private String key;

    @ApiModelProperty(value = "父级key")
    private String parentKey;

    @ApiModelProperty(value = "标题")
    private String title;

    @ApiModelProperty(value = "子节点")
    private List<YiAnLianServiceGroupVO> children;
}
