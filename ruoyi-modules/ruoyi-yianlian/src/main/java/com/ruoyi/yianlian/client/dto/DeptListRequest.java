package com.ruoyi.yianlian.client.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 易安联获取token请求
 */
@Data
@ApiModel("获取部门列表接口")
public class DeptListRequest implements Serializable
{

    @ApiModelProperty(value = "第几页数据，默认从0开始", example = "0")
    private String pageIndex;

    @ApiModelProperty(value="每页返回多少条数据，默认为20", example="20")
    private String pageSize;

    @ApiModelProperty(value = "可查询某段时间戳内变动的数据，不传默认查全部，示例2023-06-17 15:37:22", example = "2023-06-17 15:37:22")
    private String beginTime;

    @ApiModelProperty(value="可查询某段时间戳内变动的数据，不传默认查全部，示例2023-06-17 15:37:22", example="2023-06-17 15:37:22")
    private String TimeStamp;

}
