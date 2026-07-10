package com.ruoyi.yianlian.service.vpn.impl;


import com.ruoyi.common.core.constant.CacheConstants;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.yianlian.domain.LineApp;
import com.ruoyi.yianlian.mapper.LineAppMapper;
import com.ruoyi.yianlian.service.vpn.IVpnLineAppService;
import com.ruoyi.yianlian.utils.AesUtils;
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

    @Autowired
    private AesUtils aesUtils;


    @Override
    public List<LineApp> getLineAppList(){
        return lineAppMapper.selectLineAppList(new LineApp());
    }

    @Override
    public LineApp getLineAppByAppId(String appId){

        if (StringUtils.isEmpty(appId))
        {
            return null;
        }
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
        // appSecret AES加密
        if (StringUtils.isNotEmpty(lineApp.getAppSecret())) {
            lineApp.setAppSecret(encryptSpaKey(lineApp.getAppSecret()));
        }
        // spaKey AES加密
        if (StringUtils.isNotEmpty(lineApp.getSpaKey())) {
            lineApp.setSpaKey(encryptSpaKey(lineApp.getSpaKey()));
        }
        int ret = lineAppMapper.insertLineApp(lineApp);
        if(ret > 0){
            redisService.setCacheObject(buildCacheKey(lineApp.getAppId()), lineApp);
            clearYianlianTokenCache(lineApp.getAppId());
        }
        return ret;
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
        // appSecret处理：如果为空则不修改，如果有值则AES加密
        if (StringUtils.isEmpty(lineApp.getAppSecret())) {
            lineApp.setAppSecret(null);
        } else {
            lineApp.setAppSecret(encryptSpaKey(lineApp.getAppSecret()));
        }
        // spaKey处理：如果为空则不修改，如果有值则AES加密
        if (StringUtils.isEmpty(lineApp.getSpaKey())) {
            // 留空则保持原值不变
            lineApp.setSpaKey(null);
        } else {
            // 有值则AES加密
            lineApp.setSpaKey(encryptSpaKey(lineApp.getSpaKey()));
        }
        int ret = lineAppMapper.updateLineApp(lineApp);
        if(ret > 0){
            redisService.deleteObject(buildCacheKey(lineApp.getAppId()));
            clearYianlianTokenCache(lineApp.getAppId());
        }
        return ret;
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
            redisService.deleteObject(buildCacheKey(appId));
            clearYianlianTokenCache(appId);
            lineAppMapper.deleteLineAppById(appId);
        }
    }

    private String buildCacheKey(String appId)
    {
        return CacheConstants.YIANLIAN_VPN_LINE_APP + appId;
    }

    /** 线路凭证变更后，同步清除易安联 access_token 缓存 */
    private void clearYianlianTokenCache(String appId)
    {
        if (StringUtils.isNotEmpty(appId))
        {
            redisService.deleteObject(CacheConstants.YIANLIAN_TOKEN_KEY + appId);
        }
    }

    /**
     * AES加密
     */
    private String encryptSpaKey(String spaKey) {
        return aesUtils.encrypt(spaKey);
    }

}
