package com.ruoyi.yianlian.client.YiAnLianBase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 易安联请求基类
 *
 * @author ruoyi
 */

@ApiModel(
        description = "请求基础对象"
)
@Data
public class YiAnLianRequest implements Serializable
{
    /**
     * 应用ID（序列化时忽略）
     */
    @JsonIgnore
    @ApiModelProperty(value="应用ID", example="xxx")
    private String appId;
}
