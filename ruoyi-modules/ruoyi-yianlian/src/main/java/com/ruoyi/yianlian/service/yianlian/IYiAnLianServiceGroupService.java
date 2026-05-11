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
     * 查询应用组列表
     */
    YiAnLianServiceGroupListResp getServiceGroupList(String appId);

    /**
     * 创建应用组，返回易安联的key
     */
    String create(String appId, YiAnLianServiceGroupVO serviceGroup);

    /**
     * 修改应用组
     */
    Boolean update(String appId, YiAnLianServiceGroupVO serviceGroup);

    /**
     * 删除应用组
     */
    Boolean delete(String appId, List<String> ids);
}
