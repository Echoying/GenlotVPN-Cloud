package com.ruoyi.job.task;

import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.yianlian.api.RemoteLineProbeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * VPN 线路定时探测任务
 */
@Component("lineProbeTask")
public class LineProbeTask
{
    private static final Logger log = LoggerFactory.getLogger(LineProbeTask.class);

    @Autowired
    private RemoteLineProbeService remoteLineProbeService;

    public void run()
    {
        R<Integer> result = remoteLineProbeService.runProbe(2, SecurityConstants.INNER);
        if (result != null && R.isSuccess(result))
        {
            log.info("VPN线路探测完成，本次探测 {} 条", result.getData());
        }
        else
        {
            String msg = result != null ? result.getMsg() : "无响应";
            log.warn("VPN线路探测失败: {}", msg);
        }
    }
}
