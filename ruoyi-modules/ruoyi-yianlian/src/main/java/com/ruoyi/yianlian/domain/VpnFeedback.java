package com.ruoyi.yianlian.domain;

import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * VPN 客户端问题反馈 vpn_feedback
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VpnFeedback extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 标题 */
    private String title;

    /** 描述 */
    private String content;

    /** 分类 connect/login/ui/other */
    private String category;

    /** 状态 0待处理 1已排期 2已修复 */
    private String status;

    /** vpn_user.user_id，可空 */
    private Long userId;

    /** 账号 */
    private String userName;

    /** 客户端版本 */
    private String clientVersion;

    /** 客户端平台 windows/macos/unknown */
    private String clientPlatform;

    /** 提交 IP */
    private String ipaddr;

    /** 截图 URL JSON 数组，最多 3 个 */
    private String imageUrls;
}
