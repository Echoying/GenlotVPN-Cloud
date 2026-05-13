package com.ruoyi.yianlian.service.yianlian;

import com.ruoyi.yianlian.client.dto.vo.YiAnLianServiceVO;
import com.ruoyi.yianlian.client.dto.YiAnLianServiceListRequest;
import com.ruoyi.yianlian.client.dto.YiAnLianServiceListResp;

import java.util.List;

/**
 * 易安联应用服务
 */
public interface IYiAnLianServiceService
{
    /**
     * 根据应用组查询应用列表
     */
    List<YiAnLianServiceVO> getServiceList(YiAnLianServiceListRequest request);

    /**
     * 创建应用
     */
    Boolean create(String appId, YiAnLianServiceVO service);

    /**
     * 修改应用
     */
    Boolean update(String appId, YiAnLianServiceVO service);

    /**
     * 删除应用
     */
    Boolean delete(String appId, List<String> ids);
}
