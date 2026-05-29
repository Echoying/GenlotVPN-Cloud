package com.ruoyi.yianlian.domain;

import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/**
 * 线路表 line_app
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class LineApp extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 线路Id */
    private String appId;

    /** 线路名称 */
    private String appName;

    /** 线路密钥 */
    private String appSecret;

    /** 管理系统URL */
    private String url;

    /** 服务器域名或IP */
    private String host;

    /** 服务器端口号 */
    private Integer srvPort;

    /** 敲门端口 */
    private Integer spaPort;

    /** 预共享秘钥（AES加密存储） */
    private String spaKey;

    /** 线路状态:0启用,1停用 */
    private String status;


    @NotBlank(message = "线路名称不能为空")
    @Size(min = 0, max = 60, message = "部门名称长度不能超过60个字符")
    public String getAppName()
    {
        return appName;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("appId", getAppId())
            .append("appName", getAppName())
            .append("appSecret", getAppSecret())
            .append("url", getUrl())
            .append("status", getStatus())
            .toString();
    }
}
