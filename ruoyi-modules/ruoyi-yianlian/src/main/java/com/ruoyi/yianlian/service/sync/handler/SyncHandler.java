package com.ruoyi.yianlian.service.sync.handler;

import com.ruoyi.yianlian.service.sync.orchestrator.SyncCommand;

/**
 * 实体同步处理器：在已建立的代理会话内执行「远程 + 本地」同步。
 *
 * <p>同一实现同时服务实时 API 与补偿 Job 重放：{@link #execute} 必须能仅凭
 * {@link SyncCommand} 完成同步，且远程失败时不得写入/需回滚本地（保证可安全重试）。</p>
 */
public interface SyncHandler
{
    /**
     * 支持的业务类型（{@link com.ruoyi.yianlian.service.sync.orchestrator.SyncConstants} BIZ_*）
     */
    String bizType();

    /**
     * 代理 login 失败时是否允许延迟入队补偿。
     * 重置密码/改状态等新密码不可重放的操作应返回 false。
     */
    default boolean deferrable(SyncCommand command)
    {
        return true;
    }

    /**
     * 执行同步（已在代理会话内、由编排器调用）
     */
    void execute(SyncCommand command) throws Exception;
}
