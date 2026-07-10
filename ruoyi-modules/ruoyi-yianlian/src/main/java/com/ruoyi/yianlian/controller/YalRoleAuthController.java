package com.ruoyi.yianlian.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.yianlian.domain.YalRoleAuth;
import com.ruoyi.yianlian.domain.YalRoleAuthBatchDTO;
import com.ruoyi.yianlian.service.IYalRoleAuthService;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncCommand;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncConstants;
import com.ruoyi.yianlian.service.sync.orchestrator.YiAnLianSyncOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色授权 信息操作处理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/roleAuth")
public class YalRoleAuthController extends BaseController
{
    @Autowired
    private IYalRoleAuthService yalRoleAuthService;

    @Autowired
    private YiAnLianSyncOrchestrator syncOrchestrator;

    /**
     * 获取角色授权列表
     */
    @GetMapping("/list")
    public TableDataInfo list(YalRoleAuth yalRoleAuth)
    {
        startPage();
        List<YalRoleAuth> list = yalRoleAuthService.selectYalRoleAuthList(yalRoleAuth);
        return getDataTable(list);
    }

    /**
     * 根据角色ID获取授权列表
     */
    @GetMapping("/listByRoleId/{roleId}")
    public AjaxResult listByRoleId(@PathVariable Long roleId, @RequestParam String lineId)
    {
        List<YalRoleAuth> list = yalRoleAuthService.selectByRoleIdAndLineId(roleId, lineId);
        return success(list);
    }

    /**
     * 根据主键获取详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(yalRoleAuthService.selectYalRoleAuthById(id));
    }

    /**
     * 新增角色授权
     */
    @Log(title = "角色授权管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody YalRoleAuth yalRoleAuth)
    {
        yalRoleAuth.setCreateBy(SecurityUtils.getUsername());
        return toAjax(yalRoleAuthService.insertYalRoleAuth(yalRoleAuth));
    }

    /**
     * 修改角色授权
     */
    @Log(title = "角色授权管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody YalRoleAuth yalRoleAuth)
    {
        yalRoleAuth.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(yalRoleAuthService.updateYalRoleAuth(yalRoleAuth));
    }

    /**
     * 删除角色授权
     */
    @Log(title = "角色授权管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(yalRoleAuthService.deleteYalRoleAuthByIds(ids));
    }

    /**
     * 批量保存角色授权（先删后插）
     */
    @Log(title = "角色授权管理", businessType = BusinessType.UPDATE)
    @PostMapping("/batchSave")
    public AjaxResult batchSave(@RequestBody YalRoleAuthBatchDTO batchDTO)
    {
        syncOrchestrator.execute(SyncCommand.ofApi(SyncConstants.BIZ_YAL_ROLE_AUTH, SyncConstants.OP_UPDATE,
            batchDTO.getLineId(), batchDTO.getRoleId(), batchDTO));
        return success();
    }

    /**
     * 获取应用服务树结构
     */
    @GetMapping("/serviceTree")
    public AjaxResult serviceTree(@RequestParam(required = false) String appId)
    {
        return success(yalRoleAuthService.buildServiceTree(appId));
    }
}
