package com.ruoyi.yianlian.api.domain;

import java.io.Serializable;

/**
 * VPN 问题反馈内部创建体（Feign 传输）
 *
 * @author ruoyi
 */
public class VpnFeedbackCreateDTO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 标题 */
    private String title;

    /** 描述 */
    private String content;

    /** 分类 connect/login/ui/other，可空 */
    private String category;

    /** vpn_user.user_id，未登录可空 */
    private Long userId;

    /** 账号 */
    private String userName;

    /** 客户端版本 */
    private String clientVersion;

    /** 客户端平台 */
    private String clientPlatform;

    /** 提交 IP */
    private String ipaddr;

    /** 截图 URL JSON 数组 */
    private String imageUrls;

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getClientVersion()
    {
        return clientVersion;
    }

    public void setClientVersion(String clientVersion)
    {
        this.clientVersion = clientVersion;
    }

    public String getClientPlatform()
    {
        return clientPlatform;
    }

    public void setClientPlatform(String clientPlatform)
    {
        this.clientPlatform = clientPlatform;
    }

    public String getIpaddr()
    {
        return ipaddr;
    }

    public void setIpaddr(String ipaddr)
    {
        this.ipaddr = ipaddr;
    }

    public String getImageUrls()
    {
        return imageUrls;
    }

    public void setImageUrls(String imageUrls)
    {
        this.imageUrls = imageUrls;
    }
}
