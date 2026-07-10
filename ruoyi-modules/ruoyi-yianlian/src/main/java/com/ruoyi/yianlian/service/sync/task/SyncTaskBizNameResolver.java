package com.ruoyi.yianlian.service.sync.task;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.domain.VpnDept;
import com.ruoyi.yianlian.domain.VpnLocalUser;
import com.ruoyi.yianlian.domain.VpnRole;
import com.ruoyi.yianlian.domain.VpnService;
import com.ruoyi.yianlian.domain.VpnServiceGroup;
import com.ruoyi.yianlian.domain.VpnUser;
import com.ruoyi.yianlian.domain.YianlianSyncTask;
import com.ruoyi.yianlian.mapper.VpnDeptMapper;
import com.ruoyi.yianlian.mapper.VpnLocalUserMapper;
import com.ruoyi.yianlian.mapper.VpnRoleMapper;
import com.ruoyi.yianlian.mapper.VpnServiceGroupMapper;
import com.ruoyi.yianlian.mapper.VpnServiceMapper;
import com.ruoyi.yianlian.mapper.VpnUserMapper;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 同步补偿任务列表：将 bizId 解析为可展示的业务对象名称
 */
@Component
public class SyncTaskBizNameResolver
{
    @Autowired
    private VpnDeptMapper deptMapper;

    @Autowired
    private VpnRoleMapper roleMapper;

    @Autowired
    private VpnServiceMapper serviceMapper;

    @Autowired
    private VpnServiceGroupMapper serviceGroupMapper;

    @Autowired
    private VpnUserMapper userMapper;

    @Autowired
    private VpnLocalUserMapper localUserMapper;

    public void enrich(YianlianSyncTask task)
    {
        if (task == null)
        {
            return;
        }
        task.setBizName(resolve(task));
    }

    public String resolve(YianlianSyncTask task)
    {
        if (task == null)
        {
            return "-";
        }
        String fromId = resolveByBizId(task.getBizType(), task.getBizId());
        if (StringUtils.isNotEmpty(fromId))
        {
            return fromId;
        }
        String fromPayload = resolveFromPayload(task.getPayload());
        if (StringUtils.isNotEmpty(fromPayload))
        {
            return fromPayload;
        }
        if (StringUtils.isNotEmpty(task.getBizId()))
        {
            return task.getBizId();
        }
        return "新建";
    }

    private String resolveByBizId(String bizType, String bizId)
    {
        if (StringUtils.isEmpty(bizType) || StringUtils.isEmpty(bizId))
        {
            return null;
        }
        try
        {
            Long id = Long.parseLong(bizId.trim());
            switch (bizType)
            {
                case SyncConstants.BIZ_DEPT:
                case SyncConstants.BIZ_YAL_DEPT_AUTH:
                {
                    VpnDept dept = deptMapper.selectDeptById(id);
                    return dept != null ? StringUtils.trimToEmpty(dept.getDeptName()) : null;
                }
                case SyncConstants.BIZ_ROLE:
                case SyncConstants.BIZ_YAL_ROLE_AUTH:
                {
                    VpnRole role = roleMapper.selectRoleById(id);
                    return role != null ? StringUtils.trimToEmpty(role.getRoleName()) : null;
                }
                case SyncConstants.BIZ_SERVICE:
                {
                    VpnService service = serviceMapper.selectServiceById(id);
                    return service != null ? StringUtils.trimToEmpty(service.getName()) : null;
                }
                case SyncConstants.BIZ_SERVICE_GROUP:
                {
                    VpnServiceGroup group = serviceGroupMapper.selectServiceGroupById(id);
                    return group != null ? StringUtils.trimToEmpty(group.getGroupName()) : null;
                }
                case SyncConstants.BIZ_VPN_USER:
                case SyncConstants.BIZ_YAL_USER_AUTH:
                {
                    VpnUser user = userMapper.selectUserById(id);
                    if (user == null)
                    {
                        return null;
                    }
                    return StringUtils.isNotEmpty(user.getNickName()) ? user.getNickName() : user.getUserName();
                }
                case SyncConstants.BIZ_LOCAL_USER:
                {
                    VpnLocalUser localUser = localUserMapper.selectLocalUserById(id);
                    if (localUser == null)
                    {
                        return null;
                    }
                    return StringUtils.isNotEmpty(localUser.getNickName()) ? localUser.getNickName() : localUser.getUserName();
                }
                default:
                    return null;
            }
        }
        catch (NumberFormatException e)
        {
            return bizId;
        }
    }

    private String resolveFromPayload(String payload)
    {
        if (StringUtils.isEmpty(payload))
        {
            return null;
        }
        try
        {
            JSONObject json = JSON.parseObject(payload);
            if (json == null)
            {
                return null;
            }
            String[] keys = {"nickName", "userName", "deptName", "roleName", "name", "groupName", "appName"};
            for (String key : keys)
            {
                String value = json.getString(key);
                if (StringUtils.isNotEmpty(value))
                {
                    return value.trim();
                }
            }
        }
        catch (Exception ignored)
        {
            // payload 非 JSON 或结构不符时忽略
        }
        return null;
    }
}
