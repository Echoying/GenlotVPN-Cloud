package com.ruoyi.yianlian.service.vpn;

/**
 * 线路定时探测服务
 */
public interface ILineProbeService
{
    /**
     * 执行定时探测，最多处理 maxCount 条启用线路
     *
     * @param maxCount 单次最多探测条数
     * @return 实际探测条数
     */
    int runScheduledProbe(int maxCount);
}
