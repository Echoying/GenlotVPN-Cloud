package com.ruoyi.yianlian.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.yianlian.domain.VpnLogininfor;
import com.ruoyi.yianlian.mapper.VpnLogininforMapper;
import com.ruoyi.yianlian.service.IVpnLogininforService;

/**
 * VPN登录日志 服务层处理
 *
 * @author ruoyi
 */
@Service
public class VpnLogininforServiceImpl implements IVpnLogininforService
{

    @Autowired
    private VpnLogininforMapper logininforMapper;

    /**
     * 新增VPN登录日志
     *
     * @param logininfor 访问日志对象
     */
    @Override
    public int insertLogininfor(VpnLogininfor logininfor)
    {
        return logininforMapper.insertLogininfor(logininfor);
    }

    /**
     * 查询VPN登录日志集合
     *
     * @param logininfor 访问日志对象
     * @return 登录记录集合
     */
    @Override
    public List<VpnLogininfor> selectLogininforList(VpnLogininfor logininfor)
    {
        return logininforMapper.selectLogininforList(logininfor);
    }

    /**
     * 批量删除VPN登录日志
     *
     * @param infoIds 需要删除的登录日志ID
     * @return 结果
   */
    @Override
    public int deleteLogininforByIds(Long[] infoIds)
    {
        return logininforMapper.deleteLogininforByIds(infoIds);
    }

    /**
     * 清空VPN登录日志
     */
    @Override
    public void cleanLogininfor()
    {
  logininforMapper.cleanLogininfor();
    }
}
