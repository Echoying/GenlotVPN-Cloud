package com.ruoyi.yianlian.api;

import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.constant.ServiceNameConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.yianlian.api.factory.RemoteLineProbeFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 线路探测 Feign（供 ruoyi-job 调用）
 */
@FeignClient(contextId = "remoteLineProbeService", value = ServiceNameConstants.YIANLIAN_SERVICE,
        fallbackFactory = RemoteLineProbeFallbackFactory.class)
public interface RemoteLineProbeService
{
    /**
     * 执行线路探测
     *
     * @param limit 单次最多探测条数
     * @param source 内部调用标识
     * @return 实际探测条数
     */
    @PostMapping("/line/probe/run")
    R<Integer> runProbe(@RequestParam("limit") int limit,
                        @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
