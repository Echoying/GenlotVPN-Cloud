package com.ruoyi.yianlian.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.yianlian.domain.VpnLogininfor;
import com.ruoyi.yianlian.domain.vo.VpnDashboardNameValueVO;
import com.ruoyi.yianlian.domain.vo.VpnDashboardTrendItemVO;

/**
 * VPN 仪表盘统计 数据层
 */
public interface VpnDashboardMapper
{
    long countLocalUserTotal();

    long countLineEnabledTotal();

    long countConnectSuccess(@Param("beginTime") Date beginTime, @Param("endTime") Date endTime);

    long countLoginFail(@Param("beginTime") Date beginTime, @Param("endTime") Date endTime);

    List<VpnDashboardTrendItemVO> selectLoginTrend(@Param("beginTime") Date beginTime);

    List<VpnDashboardNameValueVO> selectClientOsTop(@Param("beginTime") Date beginTime, @Param("limit") int limit);

    List<VpnDashboardNameValueVO> selectFailReasonTop(@Param("beginTime") Date beginTime, @Param("limit") int limit);

    List<VpnLogininfor> selectRecentEvents(@Param("limit") int limit);
}
