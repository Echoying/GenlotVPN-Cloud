package com.ruoyi.yianlian.client.dto.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 易安联应用实体
 */
@Data
@ApiModel("应用实体")
@JsonIgnoreProperties(ignoreUnknown = true)
public class YiAnLianServiceVO implements Serializable
{
    @ApiModelProperty(value = "应用ID")
    private String id;

    @ApiModelProperty(value = "应用名称", required = true)
    private String name;

    @ApiModelProperty(value = "应用类型 web应用(web),隧道应用(cs)", required = true)
    private String type;

    @ApiModelProperty(value = "应用地址", required = true)
    private String url;

    @ApiModelProperty(value = "支持浏览器 默认浏览器：0  IE浏览器: 1 非IE浏览器: 2  国密浏览器: 3", required = true)
    private String browserType;

    @ApiModelProperty(value = "应用服务端口", required = true)
    private String webPort;

    @ApiModelProperty(value = "等级Id 高(00000000000001),中(00000000000002)低(00000000000003)", required = true)
    private String creditLevelId;

    @ApiModelProperty(value = "应用分组Id", required = true)
    private List<String> serviceGroupIds;

    @ApiModelProperty(value = "应用图标", required = true)
    private String icon;

    @ApiModelProperty(value = "应用列表是否展示应用", required = true)
    private Boolean ifShow;

    @ApiModelProperty(value = "是否启用二次认证 0：启用  1: 禁用", required = true)
    private String secondAuthEnable;

    @ApiModelProperty(value = "是否允许用户自助申请此应用", required = true)
    private Boolean ifSelfApply;

    @ApiModelProperty(value = "是否开启告警开关", required = true)
    private Boolean ifSAlarmTip;

    @ApiModelProperty(value = "是否自定义告警内容", required = true)
    private Boolean ifCustomAlarmContent;

    @ApiModelProperty(value = "自定义告警内容")
    private String customAlarmContent;

    @ApiModelProperty(value = "应用创建类型,3(三方导入)", required = true)
    private String createType;

    @ApiModelProperty(value = "CS服务器配置列表")
    private List<ReqCsServerVO> reqCsServerVos;

    @ApiModelProperty(value = "描述")
    private String description;

    @ApiModelProperty(value = "是否启用监控")
    private Boolean monitorEnable;

    @ApiModelProperty(value = "健康状态")
    private String monitorStatus;
}
