package com.ruoyi.yianlian.client.dto.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 易安联应用组实体
 * 列表返回字段: key, title, path, parentKey, description, children, nodeKey, nodeParentKey
 * 创建请求字段: name, parentId, path, description
 * 修改请求字段: id, name, description
 * 删除请求: List<String> ids (key值列表)
 */
@Data
@ApiModel("应用组实体")
public class YiAnLianServiceGroupVO implements Serializable
{
    @ApiModelProperty(value = "应用组key(即ID)")
    private String key;

    @ApiModelProperty(value = "应用组标题/名称(列表返回)")
    private String title;

    @ApiModelProperty(value = "应用组路径")
    private String path;

    @ApiModelProperty(value = "父级key(列表返回)")
    private String parentKey;

    @ApiModelProperty(value = "描述")
    private String description;

    @ApiModelProperty(value = "子节点")
    private List<YiAnLianServiceGroupVO> children;

    @ApiModelProperty(value = "节点key")
    private String nodeKey;

    @ApiModelProperty(value = "节点父级key")
    private String nodeParentKey;

    @ApiModelProperty(value = "创建类型")
    private String createType;

    @ApiModelProperty(value = "状态")
    private String state;

    @ApiModelProperty(value = "是否显示输入框")
    private Boolean showInput;

    @ApiModelProperty(value = "应用组名称(创建/修改请求用)")
    private String name;

    @ApiModelProperty(value = "上级应用组id(创建请求用)")
    private String parentId;

    @ApiModelProperty(value = "应用组id(修改请求用)")
    private String id;
}
