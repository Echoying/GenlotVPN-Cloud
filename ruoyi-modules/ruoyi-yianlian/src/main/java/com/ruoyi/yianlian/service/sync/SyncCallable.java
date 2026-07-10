package com.ruoyi.yianlian.service.sync;

/**
 * 可抛出受检异常的业务动作，供 {@link ProxySessionScope#run} 包裹执行
 */
@FunctionalInterface
public interface SyncCallable<T>
{
    T call() throws Exception;
}
