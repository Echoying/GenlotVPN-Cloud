package com.ruoyi.yianlian.service.sync.handler;

import com.ruoyi.common.core.exception.yianlian.YiAnLianException;
import com.ruoyi.yianlian.domain.VpnRole;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncCommand;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncConstants;
import com.ruoyi.yianlian.service.vpn.IVpnRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 角色同步处理器：复用 {@link IVpnRoleService} 的 *WithSync。
 */
@Component
public class RoleSyncHandler implements SyncHandler
{
    @Autowired
    private IVpnRoleService roleService;

    @Override
    public String bizType()
    {
        return SyncConstants.BIZ_ROLE;
    }

    @Override
    public void execute(SyncCommand command) throws Exception
    {
        switch (command.getOperation())
        {
            case SyncConstants.OP_CREATE:
                roleService.insertRoleWithSync(command.parsePayload(VpnRole.class));
                break;
            case SyncConstants.OP_UPDATE:
                roleService.updateRoleWithSync(command.parsePayload(VpnRole.class));
                break;
            case SyncConstants.OP_DELETE:
                roleService.deleteRoleByIdsWithSync(new Long[]{Long.valueOf(command.getBizId())});
                break;
            default:
                throw new YiAnLianException("角色同步不支持的操作：" + command.getOperation());
        }
    }
}
