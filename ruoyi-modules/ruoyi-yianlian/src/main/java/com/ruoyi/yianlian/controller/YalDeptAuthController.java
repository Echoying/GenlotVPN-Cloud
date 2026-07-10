package com.ruoyi.yianlian.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.yianlian.domain.YalDeptAuth;
import com.ruoyi.yianlian.domain.YalDeptAuthBatchDTO;
import com.ruoyi.yianlian.domain.vo.ServiceTreeSelect;
import com.ruoyi.yianlian.service.IYalDeptAuthService;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncCommand;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncConstants;
import com.ruoyi.yianlian.service.sync.orchestrator.YiAnLianSyncOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门授权 信息操作处理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/deptAuth")
public class YalDeptAuthController extends BaseController
{
    @Autowired
    private IYalDeptAuthService yalDeptAuthService;

    @Autowired
    private YiAnLianSyncOrchestrator syncOrchestrator;

    /**
     * 获取部门授权列表
     */
    @GetMapping("/list")
    public TableDataInfo list(YalDeptAuth yalDeptAuth)
    {
        startPage();
        List<YalDeptAuth> list = yalDeptAuthService.selectYalDeptAuthList(yalDeptAuth);
        return getDataTable(list);
    }

    /**
     * 根据部门ID获取授权列表
     */
    @GetMapping("/listByDeptId/{deptId}")
    public AjaxResult listByDeptId(@PathVariable Long deptId, @RequestParam String lineId)
    {
        List<YalDeptAuth> list = yalDeptAuthService.selectByDeptIdAndLineId(deptId, lineId);
        return success(list);
    }

    /**
     * 根据主键获取详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(yalDeptAuthService.selectYalDeptAuthById(id));
    }

    /**
     * 新增部门授权
     */
    @Log(title = "部门授权管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody YalDeptAuth yalDeptAuth)
    {
        yalDeptAuth.setCreateBy(SecurityUtils.getUsername());
        return toAjax(yalDeptAuthService.insertYalDeptAuth(yalDeptAuth));
    }

    /**
     * 修改部门授权
     */
    @Log(title = "部门授权管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody YalDeptAuth yalDeptAuth)
    {
        yalDeptAuth.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(yalDeptAuthService.updateYalDeptAuth(yalDeptAuth));
    }

    /**
     * 删除部门授权
     */
    @Log(title = "部门授权管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(yalDeptAuthService.deleteYalDeptAuthByIds(ids));
    }

    /**
     * 批量保存部门授权（先删后插）
     */
    @Log(title = "部门授权管理", businessType = BusinessType.UPDATE)
    @PostMapping("/batchSave")
    public AjaxResult batchSave(@RequestBody YalDeptAuthBatchDTO batchDTO)
    {
        syncOrchestrator.execute(SyncCommand.ofApi(SyncConstants.BIZ_YAL_DEPT_AUTH, SyncConstants.OP_UPDATE,
            batchDTO.getLineId(), batchDTO.getDeptId(), batchDTO));
        return success();
    }

    /**
     * 获取应用服务树结构（按线路分组，应用组为父节点，应用为子节点）
     */
    @GetMapping("/serviceTree")
    public AjaxResult serviceTree(@RequestParam(required = false) String appId)
    {
        return success(yalDeptAuthService.buildServiceTree(appId));
    }
}
