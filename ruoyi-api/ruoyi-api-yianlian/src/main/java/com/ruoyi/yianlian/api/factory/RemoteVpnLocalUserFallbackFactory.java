package com.ruoyi.yianlian.api.factory;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.yianlian.api.RemoteVpnLocalUserService;
import com.ruoyi.yianlian.api.domain.VpnChangePasswordRequest;
import com.ruoyi.yianlian.api.domain.VpnUserInfo;
import com.ruoyi.yianlian.api.model.VpnLoginUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 本地用户服务降级处理
 */
@Component
public class RemoteVpnLocalUserFallbackFactory implements FallbackFactory<RemoteVpnLocalUserService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteVpnLocalUserFallbackFactory.class);

    @Override
    public RemoteVpnLocalUserService create(Throwable throwable)
    {
        log.error("本地用户服务调用失败:{}", throwable.getMessage());
        return new RemoteVpnLocalUserService()
        {
            @Override
            public R<VpnLoginUser> getUserInfo(String username, String source)
            {
                return R.fail("获取本地用户失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> recordUserLogin(VpnUserInfo vpnUser, String source)
            {
                return R.fail("记录本地用户登录信息失败:" + throwable.getMessage());
            }

            @Override
            public R<List<Map<String, Object>>> getAuthorizedLines(Long localUserId, String source)
            {
                return R.fail("获取授权线路失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> isAuthorizedForLine(Long localUserId, String appId, String source)
            {
                return R.fail("校验线路授权失败:" + throwable.getMessage());
            }

            @Override
            public R<Map<String, String>> getLineUserCredentials(Long localUserId, String appId, String source)
            {
                return R.fail("获取线路用户凭证失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> changePassword(VpnChangePasswordRequest request, String source)
            {
                return R.fail("修改密码失败:" + throwable.getMessage());
            }
        };
    }
}
