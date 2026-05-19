package com.ruoyi.yianlian.controller;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.yianlian.client.dto.YiAnLianRoleListRequest;
import com.ruoyi.yianlian.client.dto.YiAnLianRoleListResp;
import com.ruoyi.yianlian.client.dto.vo.YiAnLianRoleVO;
import com.ruoyi.yianlian.domain.LineApp;
import com.ruoyi.yianlian.domain.VpnRole;
import com.ruoyi.yianlian.domain.VpnRoleYianlianMapping;
import com.ruoyi.yianlian.service.IVpnRoleYianlianMappingService;
import com.ruoyi.yianlian.service.vpn.IVpnLineAppService;
import com.ruoyi.yianlian.service.vpn.IVpnRoleService;
import com.ruoyi.yianlian.service.yianlian.IYiAnLianRoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 角色信息
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/vpn/role")
public class VpnRoleController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(VpnRoleController.class);

    @Autowired
    private IVpnRoleService roleService;

    @Autowired
    private IVpnLineAppService lineAppService;

    @Autowired
    private IYiAnLianRoleService yiAnLianRoleService;

    @Autowired
    private IVpnRoleYianlianMappingService mappingService;

    @RequiresPermissions("yianlian:role:list")
    @GetMapping("/list")
    public TableDataInfo list(VpnRole role) {
        startPage();
        List<VpnRole> list = roleService.selectRoleList(role);
        return getDataTable(list);
    }

    @Log(title = "VPN角色管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("yianlian:role:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, VpnRole role) {
        List<VpnRole> list = roleService.selectRoleList(role);
        ExcelUtil<VpnRole> util = new ExcelUtil<VpnRole>(VpnRole.class);
        util.exportExcel(response, list, "VPN角色数据");
    }

    /**
     * 根据角色编号获取详细信息
     */
    @RequiresPermissions("yianlian:role:query")
    @GetMapping(value = "/{roleId}")
    public AjaxResult getInfo(@PathVariable Long roleId) {
        return success(roleService.selectRoleById(roleId));
    }

    /**
     * 新增角色 — 同步到所有易安联线路
     */
    @RequiresPermissions("yianlian:role:add")
    @Log(title = "VPN角色管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody VpnRole role) {
        if (!roleService.checkRoleNameUnique(role)) {
            return error("新增角色'" + role.getRoleName() + "'失败，角色名称已存在");
        }
        role.setCreateBy(SecurityUtils.getUsername());
        int rows = roleService.insertRole(role);
        if (rows > 0) {
            // 获取所有线路
            List<LineApp> lineApps = lineAppService.selectLineAppList(new LineApp());
            for (LineApp lineApp : lineApps) {
                // 获取所有yianlian角色数据
                YiAnLianRoleListRequest request = new YiAnLianRoleListRequest();
                request.setAppId(lineApp.getAppId());
                request.setPageIndex("1");
                request.setPageSize("10000");
                YiAnLianRoleListResp resp = yiAnLianRoleService.getRoleList(request);
                if (resp == null || resp.getData() == null) {
                    continue;
                }
                // 根据名称匹配yianlian角色ID
                String matchedYianlianId = null;
                for (YiAnLianRoleVO vo : resp.getData()) {
                    if (vo.getName().equals(role.getRoleName())) {
                        matchedYianlianId = vo.getId();
                        break;
                    }
                }
                // 如果匹配到，保存映射关系
                if (matchedYianlianId != null) {
                    VpnRoleYianlianMapping mapping = new VpnRoleYianlianMapping();
                    mapping.setRoleId(role.getRoleId());
                    mapping.setAppId(lineApp.getAppId());
                    mapping.setYianlianId(matchedYianlianId);
                    mapping.setCreateTime(new Date());
                    mappingService.insert(mapping);
                    continue;
                }
                // 不存在则创建
                YiAnLianRoleVO vo = new YiAnLianRoleVO();
                vo.setName(role.getRoleName());
                vo.setDescription(role.getRoleName());
                Boolean ret = yiAnLianRoleService.create(lineApp.getAppId(), vo);
                if (ret != null && ret) {
                    // 创建成功后，重新获取角色列表以获取新创建角色的ID
                    YiAnLianRoleListResp newResp = yiAnLianRoleService.getRoleList(request);
                    if (newResp != null && newResp.getData() != null) {
                        for (YiAnLianRoleVO newVo : newResp.getData()) {
                            if (newVo.getName().equals(role.getRoleName())) {
                                VpnRoleYianlianMapping mapping = new VpnRoleYianlianMapping();
                                mapping.setRoleId(role.getRoleId());
                                mapping.setAppId(lineApp.getAppId());
                                mapping.setYianlianId(newVo.getId());
                                mapping.setCreateTime(new Date());
                                mappingService.insert(mapping);
                                break;
                            }
                        }
                    }
                } else {
                    log.error("创建角色失败, appId: {}, roleName: {}", lineApp.getAppId(), role.getRoleName());
                }
            }
        }
        return toAjax(rows);
    }

    /**
     * 修改保存角色 — 同步到所有易安联线路
     */
    @RequiresPermissions("yianlian:role:edit")
    @Log(title = "VPN角色管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody VpnRole role) {
        if (!roleService.checkRoleNameUnique(role)) {
            return error("修改角色'" + role.getRoleName() + "'失败，角色名称已存在");
        }
        role.setUpdateBy(SecurityUtils.getUsername());
        int rows = roleService.updateRole(role);
        if (rows > 0) {
            // 获取所有线路
            List<LineApp> lineApps = lineAppService.selectLineAppList(new LineApp());
            for (LineApp lineApp : lineApps) {
                // 查询映射表获取yianlian角色ID
                VpnRoleYianlianMapping mapping = mappingService.selectByRoleIdAndAppId(role.getRoleId(), lineApp.getAppId());
                if (mapping != null) {
                    // 存在映射，使用映射表中的yianlian ID进行更新
                    YiAnLianRoleVO vo = new YiAnLianRoleVO();
                    vo.setId(mapping.getYianlianId());
                    vo.setName(role.getRoleName());
                    vo.setDescription(role.getRoleName());
                    boolean ret = yiAnLianRoleService.update(lineApp.getAppId(), vo);
                    if (!ret) {
                        log.error("更新角色失败, appId: {}, yianlianId: {}", lineApp.getAppId(), mapping.getYianlianId());
                    }
                } else {
                    // 不存在映射，创建新角色并保存映射
                    YiAnLianRoleVO vo = new YiAnLianRoleVO();
                    vo.setName(role.getRoleName());
                    vo.setDescription(role.getRoleName());
                    Boolean ret = yiAnLianRoleService.create(lineApp.getAppId(), vo);
                    if (ret != null && ret) {
                        // 创建成功后，重新获取角色列表以获取新创建角色的ID
                        YiAnLianRoleListRequest request = new YiAnLianRoleListRequest();
                        request.setAppId(lineApp.getAppId());
                        request.setPageIndex("1");
                        request.setPageSize("10000");
                        YiAnLianRoleListResp newResp = yiAnLianRoleService.getRoleList(request);
                        if (newResp != null && newResp.getData() != null) {
                            for (YiAnLianRoleVO newVo : newResp.getData()) {
                                if (newVo.getName().equals(role.getRoleName())) {
                                    VpnRoleYianlianMapping newMapping = new VpnRoleYianlianMapping();
                                    newMapping.setRoleId(role.getRoleId());
                                    newMapping.setAppId(lineApp.getAppId());
                                    newMapping.setYianlianId(newVo.getId());
                                    newMapping.setCreateTime(new Date());
                                    mappingService.insert(newMapping);
                                    break;
                                }
                            }
                        }
                    } else {
                        log.error("创建角色失败, appId: {}, roleName: {}", lineApp.getAppId(), role.getRoleName());
                    }
                }
            }
        }
        return toAjax(rows);
    }

    /**
     * 状态修改
     */
    @RequiresPermissions("yianlian:role:edit")
    @Log(title = "VPN角色管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody VpnRole role) {
        role.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(roleService.updateRoleStatus(role));
    }

    /**
     * 删除角色 — 同步到所有易安联线路
     */
    @RequiresPermissions("yianlian:role:remove")
    @Log(title = "VPN角色管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{roleIds}")
    public AjaxResult remove(@PathVariable Long[] roleIds) {
        int rows = roleService.deleteRoleByIds(roleIds);
        if (rows > 0) {
            for (Long roleId : roleIds) {
                // 通过映射表获取yianlianId进行删除
                List<VpnRoleYianlianMapping> mappings = mappingService.selectByRoleId(roleId);
                for (VpnRoleYianlianMapping mapping : mappings) {
                    boolean ret = yiAnLianRoleService.delete(mapping.getAppId(), Collections.singletonList(mapping.getYianlianId()));
                    if (!ret) {
                        log.error("删除易安联角色失败, appId: {}, yianlianId: {}", mapping.getAppId(), mapping.getYianlianId());
                    }
                }
                // 删除映射记录
                mappingService.deleteByRoleId(roleId);
            }
        }
        return toAjax(rows);
    }

    /**
     * 获取角色选择框列表
     */
    @RequiresPermissions("yianlian:role:query")
    @GetMapping("/optionselect")
    public AjaxResult optionselect() {
        return success(roleService.selectRoleAll());
    }
}
