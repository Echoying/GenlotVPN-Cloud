package com.ruoyi.yianlian.service.sync.handler;

import com.ruoyi.common.core.exception.yianlian.YiAnLianException;
import com.ruoyi.yianlian.domain.VpnUser;
import com.ruoyi.yianlian.service.sync.handler.payload.AssignRolesPayload;
import com.ruoyi.yianlian.service.sync.handler.payload.ResetPwdPayload;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncCommand;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncConstants;
import com.ruoyi.yianlian.service.vpn.IVpnUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 线路用户同步处理器：复用 {@link IVpnUserService} 的 *WithSync。
 *
 * <p>重置密码 / 改状态涉及不可重放的敏感态，标记为不可延迟（代理不可用时直接失败）。</p>
 */
@Component
public class VpnUserSyncHandler implements SyncHandler
{
    @Autowired
    private IVpnUserService userService;

    @Override
    public String bizType()
    {
        return SyncConstants.BIZ_VPN_USER;
    }

    @Override
    public boolean deferrable(SyncCommand command)
    {
        return !(SyncConstants.OP_RESET_PASSWORD.equals(command.getOperation())
            || SyncConstants.OP_CHANGE_STATUS.equals(command.getOperation()));
    }

    @Override
    public void execute(SyncCommand command) throws Exception
    {
        switch (command.getOperation())
        {
            case SyncConstants.OP_UPDATE:
                userService.updateUserWithSync(command.parsePayload(VpnUser.class));
                break;
            case SyncConstants.OP_DELETE:
                userService.deleteUserByIdsWithSync(new Long[]{Long.valueOf(command.getBizId())});
                break;
            case SyncConstants.OP_RESET_PASSWORD:
            {
                ResetPwdPayload payload = command.parsePayload(ResetPwdPayload.class);
                userService.resetPwdWithSync(payload.getUser(), payload.getPlainPassword());
                break;
            }
            case SyncConstants.OP_CHANGE_STATUS:
                userService.updateUserStatusWithSync(command.parsePayload(VpnUser.class));
                break;
            case SyncConstants.OP_ASSIGN_ROLES:
            {
                AssignRolesPayload payload = command.parsePayload(AssignRolesPayload.class);
                userService.insertUserAuth(payload.getUserId(), payload.getRoleIds());
                break;
            }
            default:
                throw new YiAnLianException("线路用户同步不支持的操作：" + command.getOperation());
        }
    }
}
