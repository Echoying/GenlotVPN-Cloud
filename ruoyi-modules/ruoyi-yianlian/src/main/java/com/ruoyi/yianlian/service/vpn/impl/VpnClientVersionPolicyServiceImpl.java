package com.ruoyi.yianlian.service.vpn.impl;

import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.yianlian.domain.VpnClientVersionPolicy;
import com.ruoyi.yianlian.mapper.VpnClientVersionPolicyMapper;
import com.ruoyi.yianlian.service.vpn.ClientVersionComparator;
import com.ruoyi.yianlian.service.vpn.IVpnClientVersionPolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * VPN 客户端版本策略 服务实现
 *
 * @author ruoyi
 */
@Service
public class VpnClientVersionPolicyServiceImpl implements IVpnClientVersionPolicyService
{
    private static final long POLICY_ID = 1L;

    private static final String CACHE_KEY = "vpn:clientVersion:policy";

    private static final long CACHE_TTL_SECONDS = 60L;

    @Autowired
    private VpnClientVersionPolicyMapper policyMapper;

    @Autowired
    private RedisService redisService;

    @Override
    public VpnClientVersionPolicy getPolicy()
    {
        VpnClientVersionPolicy cached = redisService.getCacheObject(CACHE_KEY);
        if (cached != null)
        {
            return cached;
        }
        VpnClientVersionPolicy policy = policyMapper.selectById(POLICY_ID);
        if (policy == null)
        {
            throw new ServiceException("客户端版本策略不存在");
        }
        redisService.setCacheObject(CACHE_KEY, policy, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        return policy;
    }

    @Override
    public int updatePolicy(VpnClientVersionPolicy policy)
    {
        if (policy == null)
        {
            throw new ServiceException("策略不能为空");
        }
        validate(policy);
        policy.setId(POLICY_ID);
        int rows = policyMapper.update(policy);
        if (rows > 0)
        {
            redisService.deleteObject(CACHE_KEY);
        }
        return rows;
    }

    private void validate(VpnClientVersionPolicy policy)
    {
        String enabled = policy.getEnabled();
        if (!"0".equals(enabled) && !"1".equals(enabled))
        {
            throw new ServiceException("启用状态只能为 0 或 1");
        }
        if ("1".equals(enabled) && !ClientVersionComparator.isValid(policy.getMinVersion()))
        {
            throw new ServiceException("开启拦截时最低版本必须为 x.y.z 格式");
        }
        validateUrl(policy.getDownloadUrlWindows(), "Windows 下载链接");
        validateUrl(policy.getDownloadUrlMacos(), "macOS 下载链接");
    }

    private void validateUrl(String url, String label)
    {
        if (StringUtils.isEmpty(url))
        {
            return;
        }
        String trimmed = url.trim();
        String lower = trimmed.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://"))
        {
            throw new ServiceException(label + "必须以 http:// 或 https:// 开头");
        }
    }
}
