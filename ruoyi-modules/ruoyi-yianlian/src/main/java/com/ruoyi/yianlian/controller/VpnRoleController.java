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
            syncCreateToAllLines(role);
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
            syncUpdateToAllLines(role);
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
        for (Long roleId : roleIds) {
            syncDeleteToAllLines(roleId);
        }
        return toAjax(roleService.deleteRoleByIds(roleIds));
    }

    /**
     * 获取角色选择框列表
     */
    @RequiresPermissions("yianlian:role:query")
    @GetMapping("/optionselect")
    public AjaxResult optionselect() {
        return success(roleService.selectRoleAll());
    }

    // ======================== 易安联同步方法 ========================

    private void syncCreateToAllLines(VpnRole role) {
        List<LineApp> lines = lineAppService.selectLineAppList(new LineApp());
        for (LineApp line : lines) {
            try {
                YiAnLianRoleVO vo = buildYiAnLianRoleVO(role);
                yiAnLianRoleService.create(line.getAppId(), vo);

                String yianlianId = queryYianlianRoleIdByName(line.getAppId(), role.getRoleName());
                if (yianlianId != null) {
                    VpnRoleYianlianMapping mapping = new VpnRoleYianlianMapping();
                    mapping.setRoleId(role.getRoleId());
                    mapping.setAppId(line.getAppId());
                    mapping.setYianlianId(yianlianId);
                    mapping.setCreateTime(new Date());
                    mappingService.insert(mapping);
                }
            } catch (Exception e) {
                log.error("同步创建角色到线路[{}]失败: {}", line.getAppId(), e.getMessage(), e);
            }
        }
    }

    private void syncUpdateToAllLines(VpnRole role) {
        List<VpnRoleYianlianMapping> mappings = mappingService.selectByRoleId(role.getRoleId());
        for (VpnRoleYianlianMapping mapping : mappings) {
            try {
                YiAnLianRoleVO vo = buildYiAnLianRoleVO(role);
                vo.setId(mapping.getYianlianId());
                yiAnLianRoleService.update(mapping.getAppId(), vo);
            } catch (Exception e) {
                log.error("同步更新角色到线路[{}]失败: {}", mapping.getAppId(), e.getMessage(), e);
            }
        }
    }

    private void syncDeleteToAllLines(Long roleId) {
        List<VpnRoleYianlianMapping> mappings = mappingService.selectByRoleId(roleId);
        for (VpnRoleYianlianMapping mapping : mappings) {
            try {
                yiAnLianRoleService.delete(mapping.getAppId(), Collections.singletonList(mapping.getYianlianId()));
            } catch (Exception e) {
                log.error("同步删除角色到线路[{}]失败: {}", mapping.getAppId(), e.getMessage(), e);
            }
        }
        mappingService.deleteByRoleId(roleId);
    }

    private String queryYianlianRoleIdByName(String appId, String roleName) {
        YiAnLianRoleListRequest request = new YiAnLianRoleListRequest();
        request.setAppId(appId);
        request.setPageIndex("1");
        request.setPageSize("100");
        YiAnLianRoleListResp resp = yiAnLianRoleService.getRoleList(request);
        if (resp != null && resp.getData() != null) {
            for (YiAnLianRoleVO item : resp.getData()) {
                if (roleName.equals(item.getName())) {
                    return item.getId();
                }
            }
        }
        return null;
    }

    private YiAnLianRoleVO buildYiAnLianRoleVO(VpnRole role) {
        YiAnLianRoleVO vo = new YiAnLianRoleVO();
        vo.setName(role.getRoleName());
        vo.setDescription(role.getRoleName());
        return vo;
    }
}
