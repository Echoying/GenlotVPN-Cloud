package com.ruoyi.yianlian.client.dto;

import com.ruoyi.yianlian.client.YiAnLianBase.YiAnLianRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 删除人员请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("删除人员请求")
public class YiAnLianUserDeleteRequest extends YiAnLianRequest
{
    @ApiModelProperty(value = "人员ID列表", required = true)
    private List<String> ids;
}
