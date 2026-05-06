package com.ruoyi.yianlian.client.dto;

import com.ruoyi.yianlian.api.domain.vo.YiAnLianDeptVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Vector;

/**
 * 易安联获取token请求
 */
@Data
@ApiModel("Yianlian Token请求实体")
public class DeptListResp implements Serializable
{

    @ApiModelProperty(value = "第几页数据，默认从0开始", example = "0")
    private String pageIndex;

    @ApiModelProperty(value="每页返回多少条数据，默认为20", example="20")
    private String pageSize;

    @ApiModelProperty(value = "总数", example = "50")
    private Integer total;

    @ApiModelProperty(value = "数据")
    private Vector<YiAnLianDeptVO> data;
}
