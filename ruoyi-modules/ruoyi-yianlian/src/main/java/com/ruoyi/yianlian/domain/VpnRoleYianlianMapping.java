package com.ruoyi.yianlian.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * VPN角色与易安联角色映射对象 vpn_role_yianlian_mapping
 */
public class VpnRoleYianlianMapping implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long roleId;
    private String appId;
    private String yianlianId;
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getYianlianId() {
        return yianlianId;
    }

    public void setYianlianId(String yianlianId) {
        this.yianlianId = yianlianId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
