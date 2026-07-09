package com.ruoyi.yianlian.controller;

import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.security.annotation.InnerAuth;
import com.ruoyi.yianlian.service.vpn.ILineProbeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 线路探测内部接口（供 ruoyi-job Feign 调用）
 */
@RestController
@RequestMapping("/line/probe")
public class LineProbeController
{
    @Autowired
    private ILineProbeService lineProbeService;

    @InnerAuth
    @PostMapping("/run")
    public R<Integer> runProbe(@RequestParam(value = "limit", defaultValue = "2") int limit,
                               @RequestHeader(SecurityConstants.FROM_SOURCE) String source)
    {
        return R.ok(lineProbeService.runScheduledProbe(limit));
    }
}
