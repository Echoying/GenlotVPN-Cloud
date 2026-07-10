package com.ruoyi.job.task;

import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.yianlian.api.RemoteYiAnLianSyncTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 易安联同步补偿定时任务
 */
@Component("yiAnLianSyncRetryTask")
public class YiAnLianSyncRetryTask
{
    private static final Logger log = LoggerFactory.getLogger(YiAnLianSyncRetryTask.class);

    @Autowired
    private RemoteYiAnLianSyncTaskService remoteYiAnLianSyncTaskService;

    public void runPending()
    {
        R<Integer> result = remoteYiAnLianSyncTaskService.runPending(50, SecurityConstants.INNER);
        if (result != null && R.isSuccess(result))
        {
            log.info("易安联同步补偿完成，本次处理 {} 条任务", result.getData());
        }
        else
        {
            String msg = result != null ? result.getMsg() : "无响应";
            log.warn("易安联同步补偿失败: {}", msg);
        }
    }
}
