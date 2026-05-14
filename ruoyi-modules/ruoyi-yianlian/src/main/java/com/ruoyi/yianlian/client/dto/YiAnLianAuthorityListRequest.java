package com.ruoyi.yianlian.client.dto;

import com.ruoyi.yianlian.client.YiAnLianBase.YiAnLianRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 易安联权限列表请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("权限列表请求")
public class YiAnLianAuthorityListRequest extends YiAnLianRequest
{
    @ApiModelProperty(value = "用户id，非必填，不为空时会匹配数据", example = "xxx")
    private String userId;

    @ApiModelProperty(value = "第几页数据，默认从0开始", example = "0")
    private String pageIndex;

    @ApiModelProperty(value = "每页返回多少条数据，默认为20", example = "20")
    private String pageSize;
}
