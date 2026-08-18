package com.ruoyi.yianlian.domain;

import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * VPN 客户端版本策略 vpn_client_version_policy（单行 id=1）
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VpnClientVersionPolicy extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键，固定为 1 */
    private Long id;

    /** 是否启用（0关 1开） */
    private String enabled;

    /** 最低客户端版本 x.y.z */
    private String minVersion;

    /** Windows 下载链接 */
    private String downloadUrlWindows;

    /** macOS 下载链接 */
    private String downloadUrlMacos;
}
