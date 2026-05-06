package com.ruoyi.yianlian.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 易安联token接口返回data
 */
@Data
@ApiModel("部门实体")
public class YiAnLianDeptVO implements Serializable
{

    @ApiModelProperty(value="部门id", example="202305311023123181663732831377641472")
    private String id;

    @ApiModelProperty(value="本地用户组", example="0")
    private String name;

    @ApiModelProperty(value="部门父id", example="202305311023123181663732831377641472")
    private String parentId;

    @ApiModelProperty(value="部门路径", example="/本地用户组")
    private  String path;

    @ApiModelProperty(value="部门类型", example="0")
    private String type;

    @ApiModelProperty(value="部门描述", example="")
    private String description;

}
