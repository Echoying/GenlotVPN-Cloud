package com.ruoyi.yianlian.service.yianlian.impl;

import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.client.dto.vo.YiAnLianUserVO;
import com.ruoyi.yianlian.client.OpenApiClient;
import com.ruoyi.yianlian.client.dto.YiAnLianUserCreateResultItem;
import com.ruoyi.yianlian.client.dto.YiAnLianUserListRequest;
import com.ruoyi.yianlian.client.dto.YiAnLianUserListResp;
import com.ruoyi.yianlian.client.dto.YiAnLianUserPasswordResetRequest;
import com.ruoyi.yianlian.client.dto.YiAnLianUserSessionRequest;
import com.ruoyi.yianlian.client.dto.YiAnLianUserSessionResponse;
import com.ruoyi.yianlian.constant.YiAnLianConstants;
import com.ruoyi.yianlian.service.yianlian.IYiAnLianUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 易安联人员服务实现
 */
@Service
@Slf4j
public class YiAnLianUserServiceImpl implements IYiAnLianUserService
{
    @Autowired
    private OpenApiClient openApiClient;

    @Override
    public YiAnLianUserListResp getUserList(YiAnLianUserListRequest request)
    {
        try {
            return openApiClient.post(request.getAppId(), YiAnLianConstants.userListPath, request, YiAnLianUserListResp.class);
        }
        catch (Exception e) {
            log.error("获取人员列表失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public List<YiAnLianUserCreateResultItem> create(String appId, List<YiAnLianUserVO> users)
    {
        if (StringUtils.isNull(users) || users.isEmpty()) {
            log.error("创建人员参数错误: users为空");
            return null;
        }

        for (YiAnLianUserVO user : users) {
            if (StringUtils.isNull(user.getUsername()) || StringUtils.isNull(user.getName())
                    || StringUtils.isNull(user.getGroups()) || user.getGroups().isEmpty()) {
                log.error("创建人员参数错误: {}", user);
                return null;
            }
        }

        try {
            return openApiClient.postForList(appId, YiAnLianConstants.userCreatePath, users, YiAnLianUserCreateResultItem.class);
        }
        catch (Exception e) {
            log.error("创建人员失败: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public Boolean update(String appId, YiAnLianUserVO user)
    {
        if (StringUtils.isNull(user.getId()) || StringUtils.isNull(user.getName())
                || StringUtils.isNull(user.getGroups()) || user.getGroups().isEmpty()) {
            log.error("更新人员参数错误: {}", user);
            return false;
        }

        try {
            return openApiClient.post(appId, YiAnLianConstants.userUpdatePath + "/" + user.getId(), user, Boolean.class);
        }
        catch (Exception e) {
            log.error("更新人员失败: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public Boolean delete(String appId, List<String> ids)
    {
        if (StringUtils.isNull(ids) || ids.isEmpty()) {
            log.error("删除人员参数错误: ids为空");
            return false;
        }

        try {
            return openApiClient.post(appId, YiAnLianConstants.userDeletePath, ids, Boolean.class);
        }
        catch (Exception e) {
            log.error("删除人员失败: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public Boolean resetPassword(String appId, YiAnLianUserPasswordResetRequest request)
    {
        if (StringUtils.isNull(request.getUsername()) || StringUtils.isNull(request.getOldPassword())
                || StringUtils.isNull(request.getNewPassword())) {
            log.error("重置密码参数错误: {}", request);
            return false;
        }

        try {
            return openApiClient.post(appId, YiAnLianConstants.userPasswordResetPath, request, Boolean.class);
        }
        catch (Exception e) {
            log.error("重置密码失败: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public List<YiAnLianUserSessionResponse> getUserSession(YiAnLianUserSessionRequest request)
    {
        try {
            return openApiClient.postForList(request.getAppId(), YiAnLianConstants.userSessionPath, request, YiAnLianUserSessionResponse.class);
        }
        catch (Exception e) {
            log.error("获取用户会话失败: {}", e.getMessage());
            return null;
        }
    }
}
