package com.ruoyi.yianlian.controller;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.InnerAuth;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.yianlian.api.domain.VpnClientVersionPolicyDTO;
import com.ruoyi.yianlian.domain.VpnClientVersionPolicy;
import com.ruoyi.yianlian.service.vpn.IVpnClientVersionPolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * VPN 客户端版本策略
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/vpn/clientVersion")
public class VpnClientVersionController extends BaseController
{
    @Autowired
    private IVpnClientVersionPolicyService clientVersionPolicyService;

    /**
     * 查询客户端版本策略
     */
    @RequiresPermissions("vpn:clientVersion:query")
    @GetMapping
    public AjaxResult get()
    {
        return success(clientVersionPolicyService.getPolicy());
    }

    /**
     * 修改客户端版本策略
     */
    @RequiresPermissions("vpn:clientVersion:edit")
    @Log(title = "客户端版本策略", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody VpnClientVersionPolicy policy)
    {
        policy.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(clientVersionPolicyService.updatePolicy(policy));
    }

    /**
     * 内部接口：供 vpn-auth 读取策略
     */
    @InnerAuth
    @GetMapping("/inner")
    public R<VpnClientVersionPolicyDTO> inner()
    {
        VpnClientVersionPolicy p = clientVersionPolicyService.getPolicy();
        VpnClientVersionPolicyDTO dto = new VpnClientVersionPolicyDTO();
        dto.setId(p.getId());
        dto.setEnabled(p.getEnabled());
        dto.setMinVersion(p.getMinVersion());
        dto.setDownloadUrlWindows(p.getDownloadUrlWindows());
        dto.setDownloadUrlMacos(p.getDownloadUrlMacos());
        return R.ok(dto);
    }
}
