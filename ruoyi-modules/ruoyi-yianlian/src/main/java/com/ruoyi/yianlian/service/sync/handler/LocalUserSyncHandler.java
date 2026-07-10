package com.ruoyi.yianlian.service.sync.handler;

import com.ruoyi.common.core.exception.yianlian.YiAnLianException;
import com.ruoyi.yianlian.domain.vo.VpnLocalUserSyncRequest;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncCommand;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncConstants;
import com.ruoyi.yianlian.service.vpn.IVpnLocalUserSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 本地用户→线路 同步处理器（多线路场景中每条线路为一次独立命令）。
 *
 * <p>底层复用 {@link IVpnLocalUserSyncService#syncToLine}（@Transactional，远程失败回滚本地）。</p>
 */
@Component
public class LocalUserSyncHandler implements SyncHandler
{
    /** 已同步的幂等提示（重放时视为成功） */
    private static final String ALREADY_SYNCED = "已同步到此线路";

    @Lazy
    @Autowired
    private IVpnLocalUserSyncService localUserSyncService;

    @Override
    public String bizType()
    {
        return SyncConstants.BIZ_LOCAL_USER;
    }

    @Override
    public void execute(SyncCommand command) throws Exception
    {
        if (!SyncConstants.OP_CREATE.equals(command.getOperation()))
        {
            throw new YiAnLianException("本地用户同步不支持的操作：" + command.getOperation());
        }
        VpnLocalUserSyncRequest request = command.parsePayload(VpnLocalUserSyncRequest.class);
        try
        {
            localUserSyncService.syncToLine(request);
        }
        catch (Exception e)
        {
            // 重放时若已同步过，视为幂等成功，避免无谓的补偿失败
            if (e.getMessage() != null && e.getMessage().contains(ALREADY_SYNCED))
            {
                return;
            }
            throw e;
        }
    }
}
