package com.ruoyi.yianlian.api;

import com.ruoyi.common.core.constant.ServiceNameConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.yianlian.api.domain.dto.YiAnLianDeptListResp;
import com.ruoyi.yianlian.api.factory.RemoteYiAnLianFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 易安联服务
 */
@FeignClient(contextId = "remoteYiAnLianService", value = ServiceNameConstants.YIANLIAN_SERVICE, fallbackFactory = RemoteYiAnLianFallbackFactory.class)
public interface RemoteYiAnLianService
{
    /**
     * 获取易安联token
     *
     * @return token结果
     */
    @PostMapping("/yianlian/dept/list")
    R<YiAnLianDeptListResp> getDeptList();


}
