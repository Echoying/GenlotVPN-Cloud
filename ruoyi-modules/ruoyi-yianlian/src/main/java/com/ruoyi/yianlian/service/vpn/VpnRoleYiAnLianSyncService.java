package com.ruoyi.yianlian.service.vpn;

import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.client.dto.YiAnLianRoleListRequest;
import com.ruoyi.yianlian.client.dto.YiAnLianRoleListResp;
import com.ruoyi.yianlian.client.dto.vo.YiAnLianRoleVO;
import com.ruoyi.yianlian.domain.VpnRole;
import com.ruoyi.yianlian.domain.VpnRoleYianlianMapping;
import com.ruoyi.yianlian.service.IVpnRoleYianlianMappingService;
import com.ruoyi.yianlian.service.yianlian.IYiAnLianRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * VPN 角色同步易安联（返回 boolean，由调用方决定事务回滚）
 */
@Service
public class VpnRoleYiAnLianSyncService
{
    @Autowired
    private IYiAnLianRoleService yiAnLianRoleService;

    @Autowired
    private IVpnRoleYianlianMappingService mappingService;

    /**
     * 新增后同步到易安联
     */
    public boolean syncOnAdd(VpnRole role)
    {
        String appId = role.getAppId();
        YiAnLianRoleListResp listResp = fetchRemoteRoleList(appId);
        if (listResp == null)
        {
            return false;
        }
        if (listResp.getData() != null)
        {
            for (YiAnLianRoleVO vo : listResp.getData())
            {
                if (vo.getName().equals(role.getRoleName()))
                {
                    return saveMapping(role.getRoleId(), appId, vo.getId());
                }
            }
        }
        YiAnLianRoleVO createVo = buildRoleVo(role.getRoleName());
        if (!Boolean.TRUE.equals(yiAnLianRoleService.create(appId, createVo)))
        {
            return false;
        }
        return saveMappingAfterCreate(role.getRoleId(), appId, role.getRoleName());
    }

    /**
     * 修改后同步到易安联
     */
    public boolean syncOnEdit(VpnRole role)
    {
        String appId = role.getAppId();
        Long roleId = role.getRoleId();
        VpnRoleYianlianMapping mapping = mappingService.selectByRoleIdAndAppId(roleId, appId);
        YiAnLianRoleListResp listResp = fetchRemoteRoleList(appId);
        if (listResp == null)
        {
            return false;
        }
        if (mapping != null && listResp.getData() != null)
        {
            YiAnLianRoleVO updateVo = null;
            for (YiAnLianRoleVO vo : listResp.getData())
            {
                if (vo.getId().equals(mapping.getYianlianId()))
                {
                    updateVo = buildRoleVo(role.getRoleName());
                    updateVo.setId(vo.getId());
                    break;
                }
            }
            if (updateVo == null)
            {
                return false;
            }
            return Boolean.TRUE.equals(yiAnLianRoleService.update(appId, updateVo));
        }
        YiAnLianRoleVO createVo = buildRoleVo(role.getRoleName());
        if (!Boolean.TRUE.equals(yiAnLianRoleService.create(appId, createVo)))
        {
            return false;
        }
        return saveMappingAfterCreate(roleId, appId, role.getRoleName());
    }

    /**
     * 删除前同步易安联（无 mapping 视为成功）
     */
    public boolean syncOnDelete(VpnRoleYianlianMapping mapping)
    {
        if (mapping == null)
        {
            return true;
        }
        if (StringUtils.isEmpty(mapping.getYianlianId()))
        {
            return false;
        }
        List<String> ids = Collections.singletonList(mapping.getYianlianId());
        return Boolean.TRUE.equals(yiAnLianRoleService.delete(mapping.getAppId(), ids));
    }

    private YiAnLianRoleListResp fetchRemoteRoleList(String appId)
    {
        YiAnLianRoleListRequest request = new YiAnLianRoleListRequest();
        request.setAppId(appId);
        request.setPageIndex("1");
        request.setPageSize("10000");
        return yiAnLianRoleService.getRoleList(request);
    }

    private boolean saveMappingAfterCreate(Long roleId, String appId, String roleName)
    {
        YiAnLianRoleListResp newListResp = fetchRemoteRoleList(appId);
        if (newListResp == null || newListResp.getData() == null)
        {
            return false;
        }
        for (YiAnLianRoleVO newVo : newListResp.getData())
        {
            if (newVo.getName().equals(roleName))
            {
                return saveMapping(roleId, appId, newVo.getId());
            }
        }
        return false;
    }

    private YiAnLianRoleVO buildRoleVo(String roleName)
    {
        YiAnLianRoleVO vo = new YiAnLianRoleVO();
        vo.setName(roleName);
        vo.setDescription(roleName);
        return vo;
    }

    private boolean saveMapping(Long roleId, String appId, String yianlianId)
    {
        if (StringUtils.isEmpty(yianlianId))
        {
            return false;
        }
        VpnRoleYianlianMapping mapping = new VpnRoleYianlianMapping();
        mapping.setRoleId(roleId);
        mapping.setAppId(appId);
        mapping.setYianlianId(yianlianId);
        mapping.setCreateTime(new Date());
        mappingService.insert(mapping);
        return true;
    }
}
