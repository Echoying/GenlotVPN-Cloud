package com.ruoyi.yianlian.client.dto;

import com.ruoyi.yianlian.client.YiAnLianBase.YiAnLianRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 根据应用组查询应用列表请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("根据应用组查询应用列表请求")
public class YiAnLianServiceListRequest extends YiAnLianRequest
{
    @ApiModelProperty(value = "应用组id，为0时查询全部", required = true)
    private String serviceGroupId;

    @ApiModelProperty(value = "第几页数据，默认从0开始")
    private String pageIndex;

    @ApiModelProperty(value = "每页返回多少条数据，默认为20")
    private String pageSize;
}
