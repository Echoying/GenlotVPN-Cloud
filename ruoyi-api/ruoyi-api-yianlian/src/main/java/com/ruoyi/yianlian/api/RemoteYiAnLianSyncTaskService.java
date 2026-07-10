package com.ruoyi.yianlian.api;

import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.constant.ServiceNameConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.yianlian.api.factory.RemoteYiAnLianSyncTaskFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 易安联同步补偿任务 Feign（供 ruoyi-job 调用）
 */
@FeignClient(contextId = "remoteYiAnLianSyncTaskService", value = ServiceNameConstants.YIANLIAN_SERVICE,
        fallbackFactory = RemoteYiAnLianSyncTaskFallbackFactory.class)
public interface RemoteYiAnLianSyncTaskService
{
    /**
     * 执行补偿：按线路分批处理到期的待补偿任务
     *
     * @param maxAppIds 单次最多处理的线路数
     * @param source    内部调用标识
     * @return 实际处理任务条数
     */
    @PostMapping("/sync-task/run-pending")
    R<Integer> runPending(@RequestParam("maxAppIds") int maxAppIds,
                          @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
