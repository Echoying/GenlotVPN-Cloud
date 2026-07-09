package com.ruoyi.yianlian.mapper;

import com.ruoyi.yianlian.domain.LineApp;

import java.util.List;

/**
 *  数据层
 * 
 * @author ruoyi
 */
public interface LineAppMapper
{
    /**
     * 查询线路信息
     * 
     * @param lineApp 线路信息
     * @return 线路信息
     */
    public LineApp selectLineApp(LineApp lineApp);

    /**
     * 通过ID查询配置
     * 
     * @param appId 参数ID
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
     * 删除线路
     * 
     * @param appId 参数ID
     * @return 结果
     */
    public int deleteLineAppById(String appId);

    /**
     * 批量删除参数信息
     * 
     * @param appIds 需要删除的参数ID
     * @return 结果
     */
    public int deleteLineAppByIds(String[] appIds);

    /**
     * 选取待探测的启用线路（按 probe_time 轮询）
     *
     * @param limit 最多条数
     * @return 线路列表
     */
    public List<LineApp> selectLinesForProbe(int limit);

    /**
     * 更新线路探测结果
     *
     * @param lineApp 含 appId、probeStatus、probeTime、probeMsg
     * @return 结果
     */
    public int updateLineProbeResult(LineApp lineApp);
}