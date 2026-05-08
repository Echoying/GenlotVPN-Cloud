package com.ruoyi.yianlian.service.vpn.impl;


import com.ruoyi.common.core.constant.CacheConstants;
import com.ruoyi.common.core.exception.ServiceException;

import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.yianlian.domain.LineApp;
import com.ruoyi.yianlian.mapper.LineAppMapper;
import com.ruoyi.yianlian.service.vpn.IVpnLineAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 线路 服务层实现
 * 
 * @author ruoyi
 */
@Service
public class VpnLineAppServiceImpl implements IVpnLineAppService
{
    @Autowired
    private LineAppMapper lineAppMapper;

    @Autowired
    private RedisService  redisService;



    @Override
    public LineApp getLineAppByAppId(String appId){

        // 先redis获取
        LineApp lineApp = redisService.getCacheObject(buildCacheKey(appId));
        if(lineApp == null){
            lineApp = lineAppMapper.selectLineAppById(appId);
            if(lineApp == null){
                throw new ServiceException("线路不存在");
            }
            redisService.setCacheObject(buildCacheKey(appId), lineApp);
        }

        return lineApp;
    }
    /**
     * 查询线路信息
     * 
     * @param appId 线路ID
     * @return 线路信息
     */
    @Override
    public LineApp selectLineAppById(String appId)
    {
        LineApp lineApp = new LineApp();
        lineApp.setAppId(appId);
        return lineAppMapper.selectLineApp(lineApp);
    }


    /**
     * 查询线路列表
     * 
     * @param lineApp 线路信息
     * @return 线路集合
     */
    @Override
    public List<LineApp> selectLineAppList(LineApp lineApp)
    {
        return lineAppMapper.selectLineAppList(lineApp);
    }

    /**
     * 新增线路
     * 
     * @param lineApp 线路信息
     * @return 结果
     */
    @Override
    public int insertLineApp(LineApp lineApp)
    {
        return lineAppMapper.insertLineApp(lineApp);
    }

    /**
     * 修改线路
     * 
     * @param lineApp 线路信息
     * @return 结果
     */
    @Override
    public int updateLineApp(LineApp lineApp)
    {
        LineApp temp = lineAppMapper.selectLineAppById(lineApp.getAppId());
        if(temp == null)
        {
            throw new ServiceException("线路不存在");
        }
        return lineAppMapper.updateLineApp(lineApp);
    }

    /**
     * 批量删除参数信息
     * 
     * @param appIds 需要删除的参数ID
     */
    @Override
    public void deleteLineAppByIds(String[] appIds)
    {
        for (String appId : appIds)
        {
            lineAppMapper.deleteLineAppById(appId);
        }
    }

    private String buildCacheKey(String appId)
    {
        return CacheConstants.YIANLIAN_VPN_LINE_APP + appId;
    }

}
