package com.ruoyi.yianlian.service.yianlian.impl;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.client.dto.vo.YiAnLianDeptVO;
import com.ruoyi.yianlian.client.OpenApiClient;
import com.ruoyi.yianlian.client.dto.YiAnLianDeptListRequest;
import com.ruoyi.yianlian.client.dto.YiAnLianDeptListResp;
import com.ruoyi.yianlian.constant.YiAnLianConstants;
import com.ruoyi.yianlian.domain.LineApp;
import com.ruoyi.yianlian.service.yianlian.IYiAnLianDeptService;
import com.ruoyi.yianlian.service.vpn.IVpnLineAppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 易安联token服务实现
 */
@Service
@Slf4j
public class YiAnLianDeptServiceImpl implements IYiAnLianDeptService
{
    @Autowired
    private OpenApiClient openApiClient;


    @Autowired
    private IVpnLineAppService lineAppService;


    public YiAnLianDeptListResp getDeptList(YiAnLianDeptListRequest request){
        try {
            return openApiClient.post(request.getAppId(), YiAnLianConstants.deptListPath, request, YiAnLianDeptListResp.class);
        }
        catch (Exception e){
            log.error("获取易安联部门列表失败, appId: {}, 错误: ", request.getAppId(), e);
            return null;
        }

    }

    @Override
    public Boolean create(String appId, YiAnLianDeptVO request) {
        if(StringUtils.isNull(request.getId()) || StringUtils.isNull(request.getName())
                || StringUtils.isNull(request.getPath())
                || StringUtils.isNull(request.getType())  || StringUtils.isNull(request.getParentId())){
            log.error("创建部门参数错误 {}", request);
            return false;
        }
        try {
            return openApiClient.post(appId,YiAnLianConstants.deptCreatePath, request, Boolean.class);
        }
        catch (Exception e){
            log.error("创建易安联部门失败, appId: {}, request: {}, 错误: ", appId, request, e);
        }
        return false;
    }

    @Override
    public Boolean update(String appId, YiAnLianDeptVO request) {
        if (StringUtils.isNull(request.getId()) || StringUtils.isNull(request.getName())
                || StringUtils.isNull(request.getPath()) || StringUtils.isNull(request.getType())
                || StringUtils.isNull(request.getParentId())) {
            log.error("更新部门参数错误 {}", request);
            return false;
        }
        try {
            return openApiClient.post(appId, YiAnLianConstants.deptUpdatePath + "/" + request.getId(), request, Boolean.class);
        }
        catch (Exception e){
            log.error("更新易安联部门失败, appId: {}, request: {}, 错误: ", appId, request, e);
        }
        return false;
    }

    @Override
    public Boolean delete(String appId, List<String> ids) {
        if (StringUtils.isNull(ids) || ids.isEmpty()) {
            log.error("删除部门参数错误 {}", ids);
            return false;
        }
        try {
            return openApiClient.post(appId, YiAnLianConstants.deptDeletePath, ids, Boolean.class);
        }
        catch (Exception e){
            log.error("删除易安联部门失败, appId: {}, ids: {}, 错误: ", appId, ids, e);
        }
        return false;
    }

    private String GetDeptPath(String appId){
        LineApp lineApp = lineAppService.selectLineAppById(appId);
        if(lineApp == null || StringUtils.isNull(lineApp.getUrl()) || StringUtils.isEmpty(lineApp.getUrl())){
            log.error("获取部门列表参数错误: 线路不存在{}", appId);
            return null;
        }
        return lineApp.getUrl() + YiAnLianConstants.deptListPath ;
    }
}
