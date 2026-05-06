package com.ruoyi.yianlian.service.impl;
import com.ruoyi.yianlian.client.OpenApiClient;
import com.ruoyi.yianlian.client.dto.DeptListRequest;
import com.ruoyi.yianlian.client.dto.DeptListResp;
import com.ruoyi.yianlian.constant.YiAnLianConstants;
import com.ruoyi.yianlian.service.YiAnLianDeptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 易安联token服务实现
 */
@Service
@Slf4j
public class YiAnLianDeptServiceImpl implements YiAnLianDeptService
{
    @Autowired
    private OpenApiClient yiAnLianOpenApiClient;


    public DeptListResp getDeptList(DeptListRequest request){
        try {
            return yiAnLianOpenApiClient.post(YiAnLianConstants.deptListPath, request, DeptListResp.class);
        }
        catch (Exception e){
            log.info(e.getMessage());
            return null;
        }

    }
}
