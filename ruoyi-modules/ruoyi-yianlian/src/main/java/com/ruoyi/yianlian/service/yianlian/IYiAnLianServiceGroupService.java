package com.ruoyi.yianlian.service.yianlian;

import com.ruoyi.yianlian.client.dto.vo.YiAnLianServiceGroupVO;
import com.ruoyi.yianlian.client.dto.YiAnLianServiceGroupListResp;

import java.util.List;

/**
 * 易安联应用组服务
 */
public interface IYiAnLianServiceGroupService
{
    /**
     * 5.4.4 查询应用组列表
     *
     * @return 应用组列表数据
     */
    YiAnLianServiceGroupListResp getServiceGroupList(String appId);

    /**
     * 5.4.1 创建应用组
     *
     * @param serviceGroup 应用组信息
     * @return 是否成功
     */
    Boolean create(String appId, YiAnLianServiceGroupVO serviceGroup);

    /**
     * 5.4.2 修改应用组
     *
     * @param serviceGroup 应用组信息
     * @return 是否成功
     */
    Boolean update(String appId, YiAnLianServiceGroupVO serviceGroup);

    /**
     * 5.4.3 删除应用组
     *
     * @param ids 应用组ID列表
     * @return 是否成功
     */
    Boolean delete(String appId, List<String> ids);
}
