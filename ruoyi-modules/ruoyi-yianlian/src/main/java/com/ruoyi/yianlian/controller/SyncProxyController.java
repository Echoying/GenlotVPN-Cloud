package com.ruoyi.yianlian.controller;

import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.security.annotation.InnerAuth;
import com.ruoyi.yianlian.domain.vo.SyncProxyConfigVO;
import com.ruoyi.yianlian.service.sync.ISyncProxyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 同步代理配置（内部服务调用）
 */
@RestController
@RequestMapping("/sync-proxy")
public class SyncProxyController
{
    @Autowired
    private ISyncProxyService syncProxyService;

    @InnerAuth
    @GetMapping("/config/{appId}")
    public R<SyncProxyConfigVO> getConfig(@PathVariable("appId") String appId,
                                          @RequestParam("userId") Long userId,
                                          @RequestHeader(SecurityConstants.FROM_SOURCE) String source)
    {
        return R.ok(syncProxyService.getSyncProxyConfig(appId, userId));
    }
}
