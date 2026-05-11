package com.ruoyi.yianlian.domain;

import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
/**
 * VPN应用组表 vpn_service_group
 */
@Data
public class VpnServiceGroup extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 父应用组ID */
    private Long parentId;

    /** 祖级列表 */
    private String ancestors;

    /** 关联线路ID */
    @NotBlank(message = "线路不能为空")
    private String appId;

    /** 易安联应用组key */
    private String yianlianKey;

    /** 应用组名称 */
    @NotBlank(message = "应用组名称不能为空")
    @Size(min = 0, max = 100, message = "应用组名称长度不能超过100个字符")
    private String groupName;

    /** 显示顺序 */
    private Integer orderNum;

    /** 描述 */
    @Size(min = 0, max = 500, message = "描述长度不能超过500个字符")
    private String description;

    /** 状态（0正常 1停用） */
    private String status;

    /** 删除标志（0存在 2删除） */
    private String delFlag;

    /** 线路名称（非数据库字段，用于展示） */
    private String appName;

    /** 父应用组名称（非数据库字段） */
    private String parentName;

    /** 子应用组 */
    private List<VpnServiceGroup> children = new ArrayList<>();
}
