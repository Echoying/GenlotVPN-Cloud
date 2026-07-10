package com.ruoyi.yianlian.service.sync.handler;

import com.ruoyi.yianlian.domain.YalRoleAuthBatchDTO;
import com.ruoyi.yianlian.service.IYalRoleAuthService;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncCommand;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 角色授权同步处理器：批量保存（先删后插）+ 远程授权，远程失败回滚本地。
 */
@Component
public class YalRoleAuthSyncHandler implements SyncHandler
{
    @Autowired
    private IYalRoleAuthService yalRoleAuthService;

    @Override
    public String bizType()
    {
        return SyncConstants.BIZ_YAL_ROLE_AUTH;
    }

    @Override
    public void execute(SyncCommand command) throws Exception
    {
        YalRoleAuthBatchDTO dto = command.parsePayload(YalRoleAuthBatchDTO.class);
        yalRoleAuthService.batchSaveRoleAuth(dto.getRoleId(), dto.getLineId(), dto.getAuthList());
    }
}
