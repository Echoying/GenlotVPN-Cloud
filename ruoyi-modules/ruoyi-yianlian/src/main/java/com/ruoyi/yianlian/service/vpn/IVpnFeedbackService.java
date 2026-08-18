package com.ruoyi.yianlian.service.vpn;

import java.util.List;
import com.ruoyi.yianlian.api.domain.VpnFeedbackCreateDTO;
import com.ruoyi.yianlian.domain.VpnFeedback;

/**
 * VPN 问题反馈 服务层
 *
 * @author ruoyi
 */
public interface IVpnFeedbackService
{
    /**
     * 查询问题反馈列表
     *
     * @param feedback 查询条件
     * @return 反馈集合
     */
    List<VpnFeedback> selectFeedbackList(VpnFeedback feedback);

    /**
     * 按主键查询
     *
     * @param id 主键
     * @return 反馈
     */
    VpnFeedback selectFeedbackById(Long id);

    /**
     * 仅更新状态
     *
     * @param id 主键
     * @param status 状态 0/1/2
     * @return 影响行数
     */
    int updateStatus(Long id, String status);

    /**
     * 内部接口落库
     *
     * @param dto 创建体
     * @return 新反馈 id
     */
    Long create(VpnFeedbackCreateDTO dto);
}
