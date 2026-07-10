package com.ruoyi.yianlian.service.sync;

import com.ruoyi.common.core.exception.yianlian.YiAnLianException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.config.SyncProxyProperties;
import com.ruoyi.yianlian.constant.SyncProxyConstants;
import com.ruoyi.yianlian.domain.VpnUser;
import com.ruoyi.yianlian.mapper.VpnUserMapper;
import com.ruoyi.yianlian.utils.AesUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 解析代理 login 所需凭证：取该线路固定用户名的 VPN 用户（默认 {@link SyncProxyConstants#LOGIN_USERNAME}），
 * 密码转换为易安联 loginWithAccount 期望的字段级 AES 密文（与 vpn-ui 选线登录一致）。
 */
@Component
public class SyncProxyCredentialResolver
{
    @Autowired
    private VpnUserMapper vpnUserMapper;

    @Autowired
    private AesUtils aesUtils;

    @Autowired
    private SyncProxyProperties syncProxyProperties;

    /**
     * 解析指定线路的代理登录凭证
     *
     * @param appId 线路ID
     * @return 用户名 + 字段级 AES 密文密码
     */
    public ProxyCredential resolve(String appId)
    {
        if (StringUtils.isEmpty(appId))
        {
            throw new YiAnLianException("线路ID为空，无法获取代理登录凭证");
        }
        String loginUsername = StringUtils.trimToEmpty(syncProxyProperties.getLoginUsername());
        if (StringUtils.isEmpty(loginUsername))
        {
            loginUsername = SyncProxyConstants.LOGIN_USERNAME;
        }
        VpnUser user = vpnUserMapper.selectUserByUserNameAndAppId(loginUsername, appId);
        if (user == null)
        {
            throw new YiAnLianException("线路[" + appId + "]未配置代理登录用户[" + loginUsername + "]");
        }
        if (!"0".equals(user.getStatus()))
        {
            throw new YiAnLianException("线路[" + appId + "]代理登录用户[" + loginUsername + "]已停用");
        }
        if (StringUtils.isEmpty(user.getEncryptedPwd()))
        {
            throw new YiAnLianException("线路[" + appId + "]代理登录用户[" + loginUsername + "]未设置密码，请在线路用户管理中重置密码");
        }
        String plain;
        try
        {
            plain = aesUtils.decrypt(user.getEncryptedPwd());
        }
        catch (Exception e)
        {
            throw new YiAnLianException("线路[" + appId + "]代理登录用户[" + loginUsername + "]密码解密失败");
        }
        if (StringUtils.isEmpty(plain))
        {
            throw new YiAnLianException("线路[" + appId + "]代理登录用户[" + loginUsername + "]密码为空");
        }
        // loginWithAccount 期望字段级 AES 密文（与 TokenController#getUserCredentials 一致）
        String fieldCipher = aesUtils.encrypt(plain);
        return new ProxyCredential(user.getUserName(), fieldCipher);
    }

    /**
     * 代理登录凭证
     */
    public static final class ProxyCredential
    {
        private final String username;
        private final String password;

        public ProxyCredential(String username, String password)
        {
            this.username = username;
            this.password = password;
        }

        public String getUsername()
        {
            return username;
        }

        public String getPassword()
        {
            return password;
        }
    }
}
