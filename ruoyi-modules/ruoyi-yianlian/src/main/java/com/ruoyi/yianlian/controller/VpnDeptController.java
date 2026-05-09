package com.ruoyi.yianlian.controller;

import com.ruoyi.common.core.constant.UserConstants;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.yianlian.client.dto.YiAnLianDeptListRequest;
import com.ruoyi.yianlian.client.dto.YiAnLianDeptListResp;
import com.ruoyi.yianlian.client.dto.vo.YiAnLianDeptVO;
import com.ruoyi.yianlian.domain.LineApp;
import com.ruoyi.yianlian.domain.VpnDept;
import com.ruoyi.yianlian.service.vpn.IVpnDeptService;
import com.ruoyi.yianlian.service.vpn.IVpnLineAppService;
import com.ruoyi.yianlian.service.yianlian.IYiAnLianDeptService;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * VPN部门信息
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/vpn/dept")
public class VpnDeptController extends BaseController
{
    @Autowired
    private IVpnDeptService deptService;

    @Autowired
    private IVpnLineAppService lineAppService;

    @Autowired
    private IYiAnLianDeptService yiAnLianDeptService;

    /**
     * 获取部门列表
     */
    @RequiresPermissions("yianlian:dept:list")
    @GetMapping("/list")
    public AjaxResult list(VpnDept dept)
    {
        List<VpnDept> depts = deptService.selectDeptList(dept);
        return success(depts);
    }

    /**
     * 查询部门列表（排除节点）
     */
    @RequiresPermissions("yianlian:dept:list")
    @GetMapping("/list/exclude/{deptId}")
    public AjaxResult excludeChild(@PathVariable(value = "deptId", required = false) Long deptId)
    {
        List<VpnDept> depts = deptService.selectDeptList(new VpnDept());
        depts.removeIf(d -> d.getDeptId().intValue() == deptId || ArrayUtils.contains(StringUtils.split(d.getAncestors(), ","), deptId + ""));
        return success(depts);
    }

    /**
     * 根据部门编号获取详细信息
     */
    @RequiresPermissions("yianlian:dept:query")
    @GetMapping(value = "/{deptId}")
    public AjaxResult getInfo(@PathVariable Long deptId)
    {
        return success(deptService.selectDeptById(deptId));
    }

    /**
     * 获取部门下拉树列表
     */
    @GetMapping("/treeselect")
    public AjaxResult treeselect(VpnDept dept)
    {
        List<VpnDept> depts = deptService.selectDeptList(dept);
        return success(deptService.buildDeptTreeSelect(depts));
    }

    /**
     * 加载对应角色部门列表树
     */
    @GetMapping(value = "/roleDeptTreeselect/{roleId}")
    public AjaxResult roleDeptTreeselect(@PathVariable("roleId") Long roleId)
    {
        List<VpnDept> depts = deptService.selectDeptList(new VpnDept());
        AjaxResult ajax = AjaxResult.success();
        ajax.put("checkedKeys", deptService.selectDeptListByRoleId(roleId));
        ajax.put("depts", deptService.buildDeptTreeSelect(depts));
        return ajax;
    }

    /**
     * 新增部门
     */
    @RequiresPermissions("yianlian:dept:add")
    @Log(title = "VPN部门管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody VpnDept dept)
    {
        if (!deptService.checkDeptNameUnique(dept))
        {
            return error("新增部门'" + dept.getDeptName() + "'失败，部门名称已存在");
        }
        dept.setCreateBy(SecurityUtils.getUsername());
        int ret = deptService.insertDept(dept);
        // 添加部门信息到线路
        if(ret > 0){
            // 获取所有线路
            List<LineApp> lineApps = lineAppService.selectLineAppList(new LineApp());
            for (LineApp lineApp : lineApps) {
                // 获取所有的yianlian部门数据
                YiAnLianDeptListRequest yiAnLianDeptListRequest = new YiAnLianDeptListRequest();
                yiAnLianDeptListRequest.setAppId(lineApp.getAppId());
                yiAnLianDeptListRequest.setPageSize("10000");
                YiAnLianDeptListResp yiAnLianDeptListResp = yiAnLianDeptService.getDeptList(yiAnLianDeptListRequest);
                // 根据名称看是否部门存在 存在就跳过
                boolean isExist = false;
                for (YiAnLianDeptVO yiAnLianDeptVO : yiAnLianDeptListResp.getData()) {
                    if (yiAnLianDeptVO.getName().equals(dept.getDeptName())) {
                        isExist = true;
                        break;
                    }
                }
                if (isExist){
                    continue;
                }
                // 不存在则创建
                YiAnLianDeptVO yiAnLianDeptVO = new YiAnLianDeptVO();
                // 如果是根部门就直接创建了
                if(dept.getParentId() == 0){
                    yiAnLianDeptVO.setName(dept.getDeptName());
                    yiAnLianDeptVO.setParentId(dept.getParentId().toString());
                    yiAnLianDeptVO.setType("0");
                }else {
                    // 找到父部门
                    VpnDept parentDept = deptService.selectDeptById(dept.getParentId());
                    //找到yianlian的父部门
                    for (YiAnLianDeptVO yiAnLianDeptVO2 : yiAnLianDeptListResp.getData()) {
                        if (yiAnLianDeptVO2.getName().equals(parentDept.getDeptName())) {
                            yiAnLianDeptVO.setName(dept.getDeptName());
                            yiAnLianDeptVO.setParentId(yiAnLianDeptVO2.getId());
                            yiAnLianDeptVO.setType("0");
                            break;
                        }
                    }
                }
                // 创建部门
                Boolean ret2 = yiAnLianDeptService.create(lineApp.getAppId(),yiAnLianDeptVO);
                if(!ret2){
                    logger.error("创建部门失败.｛｝", yiAnLianDeptVO);
                }
            }

        }
        return toAjax(ret);
    }

    /**
     * 修改部门
     */
    @RequiresPermissions("yianlian:dept:edit")
    @Log(title = "VPN部门管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody VpnDept dept)
    {
        Long deptId = dept.getDeptId();
        if (!deptService.checkDeptNameUnique(dept))
        {
            return error("修改部门'" + dept.getDeptName() + "'失败，部门名称已存在");
        }
        else if (dept.getParentId().equals(deptId))
        {
            return error("修改部门'" + dept.getDeptName() + "'失败，上级部门不能是自己");
        }
        else if (StringUtils.equals(UserConstants.DEPT_DISABLE, dept.getStatus()) && deptService.selectNormalChildrenDeptById(deptId) > 0)
        {
            return error("该部门包含未停用的子部门！");
        }
        dept.setUpdateBy(SecurityUtils.getUsername());

        int ret = deptService.updateDept(dept);
        if(ret > 0){
            // 获取所有线路
            List<LineApp> lineApps = lineAppService.selectLineAppList(new LineApp());
            for (LineApp lineApp : lineApps) {
                // 获取所有的yianlian部门数据
                YiAnLianDeptListRequest yiAnLianDeptListRequest = new YiAnLianDeptListRequest();
                yiAnLianDeptListRequest.setAppId(lineApp.getAppId());
                yiAnLianDeptListRequest.setPageSize("10000");
                YiAnLianDeptListResp yiAnLianDeptListResp = yiAnLianDeptService.getDeptList(yiAnLianDeptListRequest);
                // 根据名称看是否部门存在
                YiAnLianDeptVO yiAnLianDeptVO = new YiAnLianDeptVO();
                boolean isExist = false;
                for (YiAnLianDeptVO yiAnLianDeptVO1 : yiAnLianDeptListResp.getData()) {
                    if (yiAnLianDeptVO1.getName().equals(dept.getDeptName())) {
                        // 用属性拷贝的
                        BeanUtils.copyProperties(yiAnLianDeptVO, yiAnLianDeptVO1);
                        isExist = true;
                        break;
                    }
                }
                //存在则更新
                if (isExist){
                    // 非跟部门找到父部门
                    if(!yiAnLianDeptVO.getParentId().equals("0")){
                        for (YiAnLianDeptVO yiAnLianDeptVO2 : yiAnLianDeptListResp.getData()) {
                            if (yiAnLianDeptVO2.getName().equals(dept.getParentName())) {
                                yiAnLianDeptVO.setParentId(yiAnLianDeptVO2.getId());
                                break;
                            }
                        }
                    }
                    yiAnLianDeptVO.setName(dept.getDeptName());
                    boolean ret2 = yiAnLianDeptService.update(lineApp.getAppId(),yiAnLianDeptVO);
                    if(!ret2){
                        logger.error("更新部门失败.｛｝", yiAnLianDeptVO);
                    }
                }else {
                    yiAnLianDeptVO.setName(dept.getDeptName());
                    yiAnLianDeptVO.setParentId("0");
                    yiAnLianDeptVO.setType("0");
                    // 找到父部门
                    for (YiAnLianDeptVO yiAnLianDeptVO2 : yiAnLianDeptListResp.getData()) {
                        if (yiAnLianDeptVO2.getName().equals(dept.getParentName())) {
                            yiAnLianDeptVO.setParentId(yiAnLianDeptVO2.getId());
                            break;
                        }
                    }
                    boolean ret2 = yiAnLianDeptService.create(lineApp.getAppId(),yiAnLianDeptVO);
                    if(!ret2){
                        logger.error("创建部门失败.｛｝", yiAnLianDeptVO);
                    }
                }
            }
        }

        return toAjax(ret);
    }

    /**
     * 保存部门排序
     */
    @RequiresPermissions("yianlian:dept:edit")
    @Log(title = "保存VPN部门排序", businessType = BusinessType.UPDATE)
    @PutMapping("/updateSort")
    public AjaxResult updateSort(@RequestBody Map<String, String> params)
    {
        String[] deptIds = params.get("deptIds").split(",");
        String[] orderNums = params.get("orderNums").split(",");
        deptService.updateDeptSort(deptIds, orderNums);
        return success();
    }

    /**
     * 删除部门
     */
    @RequiresPermissions("yianlian:dept:remove")
    @Log(title = "VPN部门管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{deptId}")
    public AjaxResult remove(@PathVariable Long deptId)
    {
        if (deptService.hasChildByDeptId(deptId))
        {
            return warn("存在下级部门,不允许删除");
        }
        if (deptService.checkDeptExistUser(deptId))
        {
            return warn("部门存在用户,不允许删除");
        }
        int ret = deptService.deleteDeptById(deptId);
        if (ret > 0){
            // 获取所有线路
            List<LineApp> lineApps = lineAppService.selectLineAppList(new LineApp());
            for (LineApp lineApp : lineApps) {
                // 获取所有的yianlian部门数据
                YiAnLianDeptListRequest yiAnLianDeptListRequest = new YiAnLianDeptListRequest();
                yiAnLianDeptListRequest.setAppId(lineApp.getAppId());
                yiAnLianDeptListRequest.setPageSize("10000");
                YiAnLianDeptListResp yiAnLianDeptListResp = yiAnLianDeptService.getDeptList(yiAnLianDeptListRequest);
                for (YiAnLianDeptVO yiAnLianDeptVO : yiAnLianDeptListResp.getData()) {
                    if (yiAnLianDeptVO.getName().equals(deptId)) {
                        List<String> ids = new ArrayList<>();
                        ids.add(yiAnLianDeptVO.getId());
                        boolean ret2 = yiAnLianDeptService.delete(lineApp.getAppId(),ids);
                        if(!ret2){
                            logger.error("删除部门失败.｛｝", yiAnLianDeptVO);
                        }
                        break;
                    }
                }
            }
        }
        return toAjax(ret);
    }
}
