package com.ruoyi.yianlian.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.annotation.Excel.ColumnType;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * VPN登录日志表 vpn_logininfor
 *
 * @author ruoyi
 */
public class VpnLogininfor extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    @Excel(name = "序号", cellType = ColumnType.NUMERIC)
    private Long infoId;

    /** 用户账号 */
    @Excel(name = "用户账号")
    private String userName;

    /** 状态 0成功 1失败 */
    @Excel(name = "状态", readConverterExp = "0=成功,1=失败")
    private String status;

    /** 地址 */
    @Excel(name = "地址")
    private String ipaddr;

    /** 客户端操作系统 */
    @Excel(name = "操作系统")
    private String clientOs;

    /** 客户端MAC地址 */
    @Excel(name = "MAC地址")
    private String clientMac;

    /** 描述 */
    @Excel(name = "描述")
    private String msg;

    /** 登录用途 */
    @Excel(name = "登录用途")
    private String loginPurpose;

    /** 线路ID */
    @Excel(name = "线路ID")
    private String appId;

    /** 线路名称 */
    @Excel(name = "线路名称")
    private String appName;

    /** 线路多选查询（逗号分隔 app_id，不入库） */
    private String appIds;

    /** 访问时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "访问时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date accessTime;

    public Long getInfoId()
    {
        return infoId;
    }

    public void setInfoId(Long infoId)
    {
        this.infoId = infoId;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getIpaddr()
    {
        return ipaddr;
    }

    public void setIpaddr(String ipaddr)
    {
        this.ipaddr = ipaddr;
    }

    public String getClientOs()
    {
        return clientOs;
    }

    public void setClientOs(String clientOs)
    {
        this.clientOs = clientOs;
    }

    public String getClientMac()
    {
        return clientMac;
    }

    public void setClientMac(String clientMac)
    {
        this.clientMac = clientMac;
    }

    public String getMsg()
    {
        return msg;
    }

    public void setMsg(String msg)
    {
        this.msg = msg;
    }

    public String getLoginPurpose()
    {
        return loginPurpose;
    }

    public void setLoginPurpose(String loginPurpose)
    {
        this.loginPurpose = loginPurpose;
    }

    public String getAppId()
    {
        return appId;
    }

    public void setAppId(String appId)
    {
        this.appId = appId;
    }

    public String getAppName()
    {
        return appName;
    }

    public void setAppName(String appName)
    {
        this.appName = appName;
    }

    public String getAppIds()
    {
        return appIds;
    }

    public void setAppIds(String appIds)
    {
        this.appIds = appIds;
    }

    public Date getAccessTime()
    {
        return accessTime;
    }

    public void setAccessTime(Date accessTime)
    {
        this.accessTime = accessTime;
    }
}
