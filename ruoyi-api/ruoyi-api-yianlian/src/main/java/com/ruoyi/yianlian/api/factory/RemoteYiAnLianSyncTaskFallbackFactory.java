package com.ruoyi.yianlian.api.factory;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.yianlian.api.RemoteYiAnLianSyncTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 易安联同步补偿任务服务降级处理
 */
@Component
public class RemoteYiAnLianSyncTaskFallbackFactory implements FallbackFactory<RemoteYiAnLianSyncTaskService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteYiAnLianSyncTaskFallbackFactory.class);

    @Override
    public RemoteYiAnLianSyncTaskService create(Throwable throwable)
    {
        log.error("易安联同步补偿服务调用失败:{}", throwable.getMessage());
        return (maxAppIds, source) -> R.fail("同步补偿失败:" + throwable.getMessage());
    }
}
