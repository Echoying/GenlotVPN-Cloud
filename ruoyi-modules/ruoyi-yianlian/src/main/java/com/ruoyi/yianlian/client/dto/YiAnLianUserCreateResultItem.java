package com.ruoyi.yianlian.client.dto;

import com.ruoyi.yianlian.client.dto.vo.YiAnLianUserVO;
import lombok.Data;

import java.io.Serializable;

/**
 * 易安联创建用户返回结果项
 */
@Data
public class YiAnLianUserCreateResultItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private YiAnLianUserVO data;

    private String status;

    private String errMessage;
}
