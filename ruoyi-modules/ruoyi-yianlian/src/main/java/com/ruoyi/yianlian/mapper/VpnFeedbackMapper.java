package com.ruoyi.yianlian.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.yianlian.domain.VpnFeedback;

/**
 * VPN 问题反馈 数据层
 *
 * @author ruoyi
 */
public interface VpnFeedbackMapper
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
     * 新增问题反馈
     *
     * @param feedback 反馈
     * @return 影响行数
     */
    int insertFeedback(VpnFeedback feedback);

    /**
     * 仅更新状态
     *
     * @param id 主键
     * @param status 状态
     * @return 影响行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
