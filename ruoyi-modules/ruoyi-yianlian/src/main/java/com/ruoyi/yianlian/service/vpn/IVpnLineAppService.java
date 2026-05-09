package com.ruoyi.yianlian.service.vpn;
import com.ruoyi.yianlian.domain.LineApp;

import java.util.List;

/**
 * 线路 服务层
 * 
 * @author ruoyi
 */
public interface IVpnLineAppService
{

    /**
     * 查询线路信息
     *
     * @return 线路信息
     */
    public List<LineApp> getLineAppList();

    /**
     * 查询线路信息
     *
     * @param appId 线路ID
     * @return 线路信息
     */
    public LineApp getLineAppByAppId(String appId);

    /**
     * 查询线路信息
     * 
     * @param appId 线路ID
     * @return 线路信息
     */
    public LineApp selectLineAppById(String appId);


    /**
     * 查询线路列表
     * 
     * @param lineApp 线路信息
     * @return 线路集合
     */
    public List<LineApp> selectLineAppList(LineApp lineApp);

    /**
     * 新增线路
     * 
     * @param lineApp 线路信息
     * @return 结果
     */
    public int insertLineApp(LineApp lineApp);

    /**
     * 修改线路
     * 
     * @param lineApp 线路信息
     * @return 结果
     */
    public int updateLineApp(LineApp lineApp);

    /**
     * 批量删除参数信息
     * 
     * @param lineAppIds 需要删除的参数ID
     */
    public void deleteLineAppByIds(String[] lineAppIds);


}
