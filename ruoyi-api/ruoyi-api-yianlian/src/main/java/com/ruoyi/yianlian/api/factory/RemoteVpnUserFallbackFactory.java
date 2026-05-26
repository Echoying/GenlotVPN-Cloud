package com.ruoyi.yianlian.api.factory;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.yianlian.api.RemoteVpnUserService;
import com.ruoyi.yianlian.api.domain.VpnUserInfo;
import com.ruoyi.yianlian.api.model.VpnLoginUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 用户服务降级处理
 * 
 * @author ruoyi
 */
@Component
public class RemoteVpnUserFallbackFactory implements FallbackFactory<RemoteVpnUserService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteVpnUserFallbackFactory.class);

    @Override
    public RemoteVpnUserService create(Throwable throwable)
    {
        log.error("用户服务调用失败:{}", throwable.getMessage());
        return new RemoteVpnUserService()
        {
            @Override
            public R<VpnLoginUser> getUserInfo(String username, String source)
            {
                return R.fail("获取用户失败:" + throwable.getMessage());
            }


            @Override
            public R<Boolean> recordUserLogin(VpnUserInfo sysUser, String source)
            {
                return R.fail("记录用户登录信息失败:" + throwable.getMessage());
            }
        };
    }
}
