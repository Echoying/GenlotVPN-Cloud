package com.ruoyi.yianlian.service.vpn;

import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.client.dto.YiAnLianDeptListRequest;
import com.ruoyi.yianlian.client.dto.YiAnLianDeptListResp;
import com.ruoyi.yianlian.client.dto.vo.YiAnLianDeptVO;
import com.ruoyi.yianlian.domain.VpnDept;
import com.ruoyi.yianlian.domain.VpnDeptYianlianMapping;
import com.ruoyi.yianlian.mapper.VpnDeptMapper;
import com.ruoyi.yianlian.service.IVpnDeptYianlianMappingService;
import com.ruoyi.yianlian.service.yianlian.IYiAnLianDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * VPN 部门同步易安联（返回 boolean，由调用方决定事务回滚）
 */
@Service
public class VpnDeptYiAnLianSyncService
{
    @Autowired
    private IYiAnLianDeptService yiAnLianDeptService;

    @Autowired
    private IVpnDeptYianlianMappingService mappingService;

    @Autowired
    private VpnDeptMapper deptMapper;

    /**
     * 新增后同步到易安联
     *
     * @return 是否成功
     */
    public boolean syncOnAdd(VpnDept dept)
    {
        String appId = dept.getAppId();
        YiAnLianDeptListResp listResp = fetchRemoteDeptList(appId);
        if (listResp == null)
        {
            return false;
        }
        if (listResp.getData() != null)
        {
            for (YiAnLianDeptVO vo : listResp.getData())
            {
                if (vo.getName().equals(dept.getDeptName()))
                {
                    return saveMapping(dept.getDeptId(), appId, vo.getId());
                }
            }
        }
        YiAnLianDeptVO createVo = new YiAnLianDeptVO();
        createVo.setName(dept.getDeptName());
        createVo.setParentId(resolveParentYianlianId(dept, listResp));
        createVo.setType("0");
        if (StringUtils.isEmpty(createVo.getName()))
        {
            return false;
        }
        if (!Boolean.TRUE.equals(yiAnLianDeptService.create(appId, createVo)))
        {
            return false;
        }
        return saveMappingAfterCreate(dept.getDeptId(), appId, dept.getDeptName());
    }

    /**
     * 修改后同步到易安联
     *
     * @return 是否成功
     */
    public boolean syncOnEdit(VpnDept dept, boolean isRootDept)
    {
        String appId = dept.getAppId();
        Long deptId = dept.getDeptId();
        VpnDeptYianlianMapping mapping = mappingService.selectByDeptIdAndAppId(deptId, appId);
        YiAnLianDeptListResp listResp = fetchRemoteDeptList(appId);
        if (listResp == null)
        {
            return false;
        }
        if (mapping != null && listResp.getData() != null)
        {
            YiAnLianDeptVO updateVo = null;
            for (YiAnLianDeptVO vo : listResp.getData())
            {
                if (vo.getId().equals(mapping.getYianlianId()))
                {
                    updateVo = new YiAnLianDeptVO();
                    updateVo.setId(vo.getId());
                    updateVo.setName(dept.getDeptName());
                    updateVo.setParentId(isRootDept ? "0" : resolveParentYianlianId(dept, listResp));
                    updateVo.setType(vo.getType());
                    updateVo.setPath(vo.getPath());
                    updateVo.setDescription(vo.getDescription());
                    break;
                }
            }
            if (updateVo == null)
            {
                return false;
            }
            return yiAnLianDeptService.update(appId, updateVo);
        }
        YiAnLianDeptVO createVo = new YiAnLianDeptVO();
        createVo.setName(dept.getDeptName());
        createVo.setParentId(isRootDept ? "0" : resolveParentYianlianId(dept, listResp));
        createVo.setType("0");
        if (!Boolean.TRUE.equals(yiAnLianDeptService.create(appId, createVo)))
        {
            return false;
        }
        return saveMappingAfterCreate(deptId, appId, dept.getDeptName());
    }

    /**
     * 删除前同步易安联（无 mapping 视为成功）
     *
     * @return 是否成功
     */
    public boolean syncOnDelete(VpnDeptYianlianMapping mapping)
    {
        if (mapping == null)
        {
            return true;
        }
        if (StringUtils.isEmpty(mapping.getYianlianId()))
        {
            return false;
        }
        List<String> ids = Collections.singletonList(mapping.getYianlianId());
        return yiAnLianDeptService.delete(mapping.getAppId(), ids);
    }

    private YiAnLianDeptListResp fetchRemoteDeptList(String appId)
    {
        YiAnLianDeptListRequest request = new YiAnLianDeptListRequest();
        request.setAppId(appId);
        request.setPageSize("10000");
        return yiAnLianDeptService.getDeptList(request);
    }

    private boolean saveMappingAfterCreate(Long deptId, String appId, String deptName)
    {
        YiAnLianDeptListResp newListResp = fetchRemoteDeptList(appId);
        if (newListResp == null || newListResp.getData() == null)
        {
            return false;
        }
        for (YiAnLianDeptVO newDeptVO : newListResp.getData())
        {
            if (newDeptVO.getName().equals(deptName))
            {
                return saveMapping(deptId, appId, newDeptVO.getId());
            }
        }
        return false;
    }

    private String resolveParentYianlianId(VpnDept dept, YiAnLianDeptListResp listResp)
    {
        if (dept.getParentId() == null || dept.getParentId() == 0L)
        {
            return "0";
        }
        VpnDeptYianlianMapping parentMapping = mappingService.selectByDeptIdAndAppId(dept.getParentId(), dept.getAppId());
        if (parentMapping != null)
        {
            return parentMapping.getYianlianId();
        }
        VpnDept parentDept = deptMapper.selectDeptById(dept.getParentId());
        if (parentDept == null || listResp.getData() == null)
        {
            return "0";
        }
        for (YiAnLianDeptVO vo : listResp.getData())
        {
            if (vo.getName().equals(parentDept.getDeptName()))
            {
                return vo.getId();
            }
        }
        return "0";
    }

    private boolean saveMapping(Long deptId, String appId, String yianlianId)
    {
        if (StringUtils.isEmpty(yianlianId))
        {
            return false;
        }
        VpnDeptYianlianMapping mapping = new VpnDeptYianlianMapping();
        mapping.setDeptId(deptId);
        mapping.setAppId(appId);
        mapping.setYianlianId(yianlianId);
        mappingService.insert(mapping);
        return true;
    }
}
