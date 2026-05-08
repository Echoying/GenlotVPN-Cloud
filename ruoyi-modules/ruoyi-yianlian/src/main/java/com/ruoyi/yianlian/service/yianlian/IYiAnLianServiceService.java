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
     * 5.4.5 根据应用组查询应用列表
     *
     * @param request 请求参数
     * @return 应用列表数据
     */
    YiAnLianServiceListResp getServiceList(YiAnLianServiceListRequest request);

    /**
     * 5.4.6 创建应用
     *
     * @param service 应用信息
     * @return 是否成功
     */
    Boolean create(String appId, YiAnLianServiceVO service);

    /**
     * 5.4.7 修改应用
     *
     * @param service 应用信息
     * @return 是否成功
     */
    Boolean update(String appId, YiAnLianServiceVO service);

    /**
     * 5.4.8 删除应用
     *
     * @param ids 应用ID列表
     * @return 是否成功
     */
    Boolean delete(String appId, List<String> ids);
}
