package com.ruoyi.yianlian.client.dto;

import com.ruoyi.yianlian.client.dto.vo.YiAnLianUserVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 获取人员列表响应
 */
@Data
@ApiModel("获取人员列表响应")
public class YiAnLianUserListResp implements Serializable
{
    @ApiModelProperty(value = "第几页数据，默认从0开始", example = "0")
    private Integer pageIndex;

    @ApiModelProperty(value = "每页返回多少条数据，默认为20", example = "20")
    private Integer pageSize;

    @ApiModelProperty(value = "总数", example = "50")
    private Integer total;

    @ApiModelProperty(value = "人员数据列表")
    private List<YiAnLianUserVO> data;
}
