package com.ruoyi.yianlian.domain;

import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.yianlian.client.dto.vo.ReqCsServerVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * VPN应用表 vpn_service
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class VpnService extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    @NotBlank(message = "线路不能为空")
    private String appId;

    @NotNull(message = "应用组不能为空")
    private Long serviceGroupId;

    /** 易安联应用key */
    private String yianlianKey;

    @NotBlank(message = "应用名称不能为空")
    @Size(max = 200)
    private String name;

    @NotBlank(message = "应用类型不能为空")
    private String type;

    @NotBlank(message = "应用地址不能为空")
    private String url;

    @NotBlank(message = "浏览器类型不能为空")
    private String browserType;

    @NotBlank(message = "端口不能为空")
    private String webPort;
    private String creditLevelId;
    private String icon;
    private Boolean ifShow;
    private String secondAuthEnable;
    private Boolean ifSelfApply;
    private Boolean ifSAlarmTip;
    private Boolean ifCustomAlarmContent;
    private String customAlarmContent;
    private String createType;
    private String description;
    private String status;
    private String delFlag;

    /** CS服务器配置JSON */
    private String reqCsServerVosJson;

    /** 非数据库字段 */
    private String appName;
    private String serviceGroupName;
    private List<ReqCsServerVO> reqCsServerVos;
}
