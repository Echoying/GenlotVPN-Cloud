package com.ruoyi.yianlian.service.impl;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.api.domain.vo.YiAnLianDeptVO;
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

    @Override
    public Boolean create(YiAnLianDeptVO request) {
        if(StringUtils.isNull(request.getId()) || StringUtils.isNull(request.getName())
                || StringUtils.isNull(request.getPath())
                || StringUtils.isNull(request.getType())  || StringUtils.isNull(request.getParentId())){
            log.error("创建部门参数错误 {}", request);
            return false;
        }
        try {
            return yiAnLianOpenApiClient.post(YiAnLianConstants.deptCreatePath, request, Boolean.class);
        }
        catch (Exception e){
            log.info(e.getMessage());
        }
        return false;
    }
}
