package com.ruoyi.yianlian.service.sync.handler;

import com.ruoyi.common.core.exception.yianlian.YiAnLianException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.client.dto.vo.YiAnLianServiceGroupVO;
import com.ruoyi.yianlian.domain.VpnServiceGroup;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncCommand;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncConstants;
import com.ruoyi.yianlian.service.vpn.IVpnServiceGroupService;
import com.ruoyi.yianlian.service.yianlian.IYiAnLianServiceGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 应用组同步处理器：远程优先（远程失败则本地不写/不改），成功后落库 yianlianKey。
 */
@Component
public class ServiceGroupSyncHandler implements SyncHandler
{
    @Autowired
    private IVpnServiceGroupService serviceGroupService;

    @Autowired
    private IYiAnLianServiceGroupService yiAnLianServiceGroupService;

    @Override
    public String bizType()
    {
        return SyncConstants.BIZ_SERVICE_GROUP;
    }

    @Override
    public void execute(SyncCommand command) throws Exception
    {
        switch (command.getOperation())
        {
            case SyncConstants.OP_CREATE:
                doCreate(command.parsePayload(VpnServiceGroup.class));
                break;
            case SyncConstants.OP_UPDATE:
                doUpdate(command.parsePayload(VpnServiceGroup.class));
                break;
            case SyncConstants.OP_DELETE:
                doDelete(Long.valueOf(command.getBizId()));
                break;
            default:
                throw new YiAnLianException("应用组同步不支持的操作：" + command.getOperation());
        }
    }

    private void doCreate(VpnServiceGroup group)
    {
        YiAnLianServiceGroupVO vo = new YiAnLianServiceGroupVO();
        vo.setName(group.getGroupName());
        vo.setDescription(group.getDescription());
        vo.setParentId("0");
        vo.setPath("/" + group.getGroupName());
        if (group.getParentId() != null && group.getParentId() != 0)
        {
            VpnServiceGroup parent = serviceGroupService.selectServiceGroupById(group.getParentId());
            if (parent != null && StringUtils.isNotEmpty(parent.getYianlianKey()))
            {
                vo.setParentId(parent.getYianlianKey());
                vo.setPath(parent.getGroupName() + "/" + group.getGroupName());
            }
        }
        String yianlianKey = yiAnLianServiceGroupService.create(group.getAppId(), vo);
        if (StringUtils.isEmpty(yianlianKey))
        {
            throw new YiAnLianException("同步创建应用组到易安联失败");
        }
        group.setYianlianKey(yianlianKey);
        serviceGroupService.insertServiceGroup(group);
    }

    private void doUpdate(VpnServiceGroup group)
    {
        VpnServiceGroup oldGroup = serviceGroupService.selectServiceGroupById(group.getId());
        if (oldGroup == null)
        {
            throw new YiAnLianException("应用组不存在");
        }
        if (StringUtils.isNotEmpty(oldGroup.getYianlianKey()))
        {
            String appId = group.getAppId() != null ? group.getAppId() : oldGroup.getAppId();
            YiAnLianServiceGroupVO updateVO = new YiAnLianServiceGroupVO();
            updateVO.setId(oldGroup.getYianlianKey());
            updateVO.setName(group.getGroupName());
            updateVO.setDescription(group.getDescription());
            Boolean syncResult = yiAnLianServiceGroupService.update(appId, updateVO);
            if (syncResult == null || !syncResult)
            {
                throw new YiAnLianException("同步更新应用组到易安联失败");
            }
        }
        serviceGroupService.updateServiceGroup(group);
    }

    private void doDelete(Long id)
    {
        VpnServiceGroup group = serviceGroupService.selectServiceGroupById(id);
        if (group == null)
        {
            // 补偿重放时本地可能已删，视为幂等成功
            return;
        }
        if (StringUtils.isNotEmpty(group.getYianlianKey()))
        {
            Boolean syncResult = yiAnLianServiceGroupService.delete(group.getAppId(),
                Collections.singletonList(group.getYianlianKey()));
            if (syncResult == null || !syncResult)
            {
                throw new YiAnLianException("同步删除应用组到易安联失败");
            }
        }
        serviceGroupService.deleteServiceGroupById(id);
    }
}
