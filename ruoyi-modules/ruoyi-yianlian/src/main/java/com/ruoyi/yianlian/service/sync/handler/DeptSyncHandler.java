package com.ruoyi.yianlian.service.sync.handler;

import com.ruoyi.common.core.exception.yianlian.YiAnLianException;
import com.ruoyi.yianlian.domain.VpnDept;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncCommand;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncConstants;
import com.ruoyi.yianlian.service.vpn.IVpnDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 部门同步处理器：复用 {@link IVpnDeptService} 的 *WithSync（@Transactional，远程失败回滚本地）。
 */
@Component
public class DeptSyncHandler implements SyncHandler
{
    @Autowired
    private IVpnDeptService deptService;

    @Override
    public String bizType()
    {
        return SyncConstants.BIZ_DEPT;
    }

    @Override
    public void execute(SyncCommand command) throws Exception
    {
        switch (command.getOperation())
        {
            case SyncConstants.OP_CREATE:
                deptService.insertDeptWithSync(command.parsePayload(VpnDept.class));
                break;
            case SyncConstants.OP_UPDATE:
                deptService.updateDeptWithSync(command.parsePayload(VpnDept.class));
                break;
            case SyncConstants.OP_DELETE:
                deptService.deleteDeptWithSync(Long.valueOf(command.getBizId()));
                break;
            default:
                throw new YiAnLianException("部门同步不支持的操作：" + command.getOperation());
        }
    }
}
