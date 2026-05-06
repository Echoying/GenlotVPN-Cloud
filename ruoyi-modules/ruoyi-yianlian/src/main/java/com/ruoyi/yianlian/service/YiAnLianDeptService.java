package com.ruoyi.yianlian.service;

import com.ruoyi.yianlian.api.domain.vo.YiAnLianDeptVO;
import com.ruoyi.yianlian.client.dto.DeptListRequest;
import com.ruoyi.yianlian.client.dto.DeptListResp;

/**
 * 易安联token服务
 */
public interface YiAnLianDeptService
{
    /**
     * 获取易安联部门列表
     *
     * @param request 请求参数
     * @return token数据
     */
    DeptListResp getDeptList(DeptListRequest request);

    /**
     * 5.2.2创建部门接口
     *
     * @param request 请求参数
     * @return token数据
     */
    Boolean create(YiAnLianDeptVO request);

}
