package com.ruoyi.yianlian.client.dto;

import com.ruoyi.yianlian.client.dto.vo.YiAnLianServiceVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 应用列表响应
 */
@Data
@ApiModel("应用列表响应")
public class YiAnLianServiceListResp implements Serializable
{
    @ApiModelProperty(value = "第几页数据")
    private Integer pageIndex;

    @ApiModelProperty(value = "每页返回多少条数据")
    private Integer pageSize;

    @ApiModelProperty(value = "总数")
    private Integer total;

    @ApiModelProperty(value = "应用数据列表")
    private List<YiAnLianServiceVO> data;
}
