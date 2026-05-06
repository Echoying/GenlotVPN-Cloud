package com.ruoyi.yianlian.client.YiAnLianBase;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@ApiModel(
        description = "请求基础对象"
)
@Data
public class YiAnLianResquest implements Serializable
{
    @ApiModelProperty(value="Token", example="xxx")
    private String token;

}
