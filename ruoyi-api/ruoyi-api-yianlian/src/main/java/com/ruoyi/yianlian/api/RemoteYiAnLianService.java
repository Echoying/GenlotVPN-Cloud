package com.ruoyi.yianlian.api;

import com.ruoyi.common.core.constant.ServiceNameConstants;
import com.ruoyi.yianlian.api.factory.RemoteYiAnLianFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;


/**
 * 易安联服务
 *
 * @author ruoyi
 */
@FeignClient(contextId = "remoteYiAnLianService", value = ServiceNameConstants.YIANLIAN_SERVICE, fallbackFactory = RemoteYiAnLianFallbackFactory.class)
public interface RemoteYiAnLianService
{

}
