package com.ruoyi.yianlian.service.sync.handler;

import com.ruoyi.yianlian.domain.YalDeptAuthBatchDTO;
import com.ruoyi.yianlian.service.IYalDeptAuthService;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncCommand;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 部门授权同步处理器：批量保存（先删后插）+ 远程授权，远程失败回滚本地。
 */
@Component
public class YalDeptAuthSyncHandler implements SyncHandler
{
    @Autowired
    private IYalDeptAuthService yalDeptAuthService;

    @Override
    public String bizType()
    {
        return SyncConstants.BIZ_YAL_DEPT_AUTH;
    }

    @Override
    public void execute(SyncCommand command) throws Exception
    {
        YalDeptAuthBatchDTO dto = command.parsePayload(YalDeptAuthBatchDTO.class);
        yalDeptAuthService.batchSaveDeptAuth(dto.getDeptId(), dto.getLineId(), dto.getAuthList());
    }
}
