package com.ruoyi.yianlian.client.dto;

import com.ruoyi.yianlian.client.dto.vo.YiAnLianRoleVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 查询角色列表响应
 */
@Data
@ApiModel("查询角色列表响应")
public class YiAnLianRoleListResp implements Serializable
{
    @ApiModelProperty(value = "第几页数据")
    private Integer pageIndex;

    @ApiModelProperty(value = "每页返回多少条数据")
    private Integer pageSize;

    @ApiModelProperty(value = "总数")
    private Integer total;

    @ApiModelProperty(value = "角色数据列表")
    private List<YiAnLianRoleVO> data;
}
