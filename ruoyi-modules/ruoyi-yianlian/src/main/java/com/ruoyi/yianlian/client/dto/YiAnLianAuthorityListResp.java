package com.ruoyi.yianlian.client.dto;

import com.ruoyi.yianlian.client.dto.vo.YiAnLianAuthorityVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 易安联权限列表响应
 */
@Data
@ApiModel("权限列表响应")
public class YiAnLianAuthorityListResp implements Serializable
{
    @ApiModelProperty(value = "第几页数据")
    private Integer pageIndex;

    @ApiModelProperty(value = "每页返回多少条数据")
    private Integer pageSize;

    @ApiModelProperty(value = "总数")
    private Integer total;

    @ApiModelProperty(value = "权限数据列表")
    private List<YiAnLianAuthorityVO> data;
}
