package com.ruoyi.yianlian.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.yianlian.domain.YalUserAuth;
import com.ruoyi.yianlian.domain.YalUserAuthBatchDTO;
import com.ruoyi.yianlian.service.IYalUserAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户授权 信息操作处理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/userAuth")
public class YalUserAuthController extends BaseController
{
    @Autowired
    private IYalUserAuthService yalUserAuthService;

    /**
     * 获取用户授权列表
     */
    @GetMapping("/list")
    public TableDataInfo list(YalUserAuth yalUserAuth)
    {
        startPage();
        List<YalUserAuth> list = yalUserAuthService.selectYalUserAuthList(yalUserAuth);
        return getDataTable(list);
    }

    /**
     * 根据用户ID获取授权列表
     */
    @GetMapping("/listByUserId/{userId}")
    public AjaxResult listByUserId(@PathVariable Long userId)
    {
        List<YalUserAuth> list = yalUserAuthService.selectByUserId(userId);
        return success(list);
    }

    /**
     * 根据主键获取详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(yalUserAuthService.selectYalUserAuthById(id));
    }

    /**
     * 新增用户授权
     */
    @Log(title = "用户授权管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody YalUserAuth yalUserAuth)
    {
        yalUserAuth.setCreateBy(SecurityUtils.getUsername());
        return toAjax(yalUserAuthService.insertYalUserAuth(yalUserAuth));
    }

    /**
     * 修改用户授权
     */
    @Log(title = "用户授权管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody YalUserAuth yalUserAuth)
    {
        yalUserAuth.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(yalUserAuthService.updateYalUserAuth(yalUserAuth));
    }

    /**
     * 删除用户授权
     */
    @Log(title = "用户授权管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(yalUserAuthService.deleteYalUserAuthByIds(ids));
    }

    /**
     * 批量保存用户授权（先删后插）
     */
    @Log(title = "用户授权管理", businessType = BusinessType.UPDATE)
    @PostMapping("/batchSave")
    public AjaxResult batchSave(@RequestBody YalUserAuthBatchDTO batchDTO)
    {
        return toAjax(yalUserAuthService.batchSaveUserAuth(batchDTO.getUserId(), batchDTO.getAuthList()));
    }

    /**
     * 获取应用服务树结构
     */
    @GetMapping("/serviceTree")
    public AjaxResult serviceTree(@RequestParam(required = false) String appId)
    {
        return success(yalUserAuthService.buildServiceTree(appId));
    }
}
