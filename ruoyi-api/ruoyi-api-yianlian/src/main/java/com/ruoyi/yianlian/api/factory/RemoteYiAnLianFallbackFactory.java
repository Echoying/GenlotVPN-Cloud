package com.ruoyi.yianlian.api.factory;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.yianlian.api.RemoteYiAnLianService;
import com.ruoyi.yianlian.api.domain.dto.YiAnLianDeptListResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 易安联服务降级处理
 */
@Component
public class RemoteYiAnLianFallbackFactory implements FallbackFactory<RemoteYiAnLianService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteYiAnLianFallbackFactory.class);

    @Override
    public RemoteYiAnLianService create(Throwable throwable)
    {
        log.error("易安联服务调用失败:{}", throwable.getMessage());
        return new RemoteYiAnLianService()
        {
            @Override
            public R<YiAnLianDeptListResp> getDeptList( )
            {
                return R.fail("获取易安联部门失败:" + throwable.getMessage());
            }
        };
    }
}
