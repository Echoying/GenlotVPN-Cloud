package com.ruoyi.yianlian.service.yianlian;

import com.ruoyi.yianlian.client.dto.vo.YiAnLianDeptVO;
import com.ruoyi.yianlian.client.dto.YiAnLianDeptListRequest;
import com.ruoyi.yianlian.client.dto.YiAnLianDeptListResp;

import java.util.List;

/**
 * 易安联token服务
 */
public interface IYiAnLianDeptService
{
    /**
     * 获取易安联部门列表
     *
     * @param request 请求参数
     * @return token数据
     */
    YiAnLianDeptListResp getDeptList(YiAnLianDeptListRequest request);

    /**
     * 5.2.2创建部门接口
     *
     * @param request 请求参数
     * @return token数据
     */
    Boolean create(String appId, YiAnLianDeptVO request);

    /**
     * 5.2.3更新部门接口
     *
     * @param request 请求参数
     * @return token数据
     */
    Boolean update(String appId,YiAnLianDeptVO request);

    /**
     * 5.2.4删除部门接口
     *
     * @param ids 请求参数
     * @return token数据
     */
    Boolean delete(String appId, List<String> ids);

}
