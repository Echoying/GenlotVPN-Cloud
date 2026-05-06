package com.ruoyi.yianlian.api.domain.dto;

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
@ApiModel("Yianlian dept所有部门数据")
public class YiAnLianDeptListResp implements Serializable
{

    @ApiModelProperty(value = "数据")
    private Vector<YiAnLianDeptVO> data;
}
