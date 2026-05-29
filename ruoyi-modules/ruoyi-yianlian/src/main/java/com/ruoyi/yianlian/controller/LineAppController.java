package com.ruoyi.yianlian.controller;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.yianlian.domain.LineApp;
import com.ruoyi.yianlian.utils.AesUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.yianlian.service.vpn.IVpnLineAppService;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 线路 信息操作处理
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/line")
public class LineAppController extends BaseController
{
    @Autowired
    private IVpnLineAppService lineAppService;

    @Autowired
    private AesUtils aesUtils;

    /**
     * 获取线路列表
     */
    @RequiresPermissions("yianlian:line:list")
    @GetMapping("/list")
    public TableDataInfo list(LineApp lineApp)
    {
        startPage();
        List<LineApp> list = lineAppService.selectLineAppList(lineApp);
        // 解密敏感字段用于前端展示
        for (LineApp line : list) {
            decryptLineAppForDisplay(line);
        }
        return getDataTable(list);
    }

    @Log(title = "线路管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("yianlian:line:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, LineApp lineApp)
    {
        List<LineApp> list = lineAppService.selectLineAppList(lineApp);
        ExcelUtil<LineApp> util = new ExcelUtil<LineApp>(LineApp.class);
        util.exportExcel(response, list, "线路数据");
    }

    /**
     * 根据参数编号获取详细信息
     */
    @GetMapping(value = "/{appId}")
    public AjaxResult getInfo(@PathVariable String appId)
    {
        LineApp lineApp = lineAppService.selectLineAppById(appId);
        decryptLineAppForDisplay(lineApp);
        return success(lineApp);
    }


    /**
     * 新增线路
     */
    @RequiresPermissions("yianlian:line:add")
    @Log(title = "线路管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody LineApp lineApp)
    {
        lineApp.setCreateBy(SecurityUtils.getUsername());
        return toAjax(lineAppService.insertLineApp(lineApp));
    }

    /**
     * 修改线路
     */
    @RequiresPermissions("yianlian:line:edit")
    @Log(title = "线路管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody LineApp lineApp)
    {
        lineApp.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(lineAppService.updateLineApp(lineApp));
    }

    /**
     * 删除线路
     */
    @RequiresPermissions("yianlian:line:remove")
    @Log(title = "参数管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{appIds}")
    public AjaxResult remove(@PathVariable String[] appIds)
    {
        lineAppService.deleteLineAppByIds(appIds);
        return success();
    }

    /**
     * 返回前端前解密敏感字段
     * appSecret: AES解密后展示明文
     * spaKey: AES解密后做MD5(32位小写)展示
     */
    private void decryptLineAppForDisplay(LineApp lineApp) {
        if (lineApp == null) return;
        if (lineApp.getAppSecret() != null && !lineApp.getAppSecret().isEmpty()) {
            lineApp.setAppSecret(aesUtils.decrypt(lineApp.getAppSecret()));
        }
        if (lineApp.getSpaKey() != null && !lineApp.getSpaKey().isEmpty()) {
            String plainSpaKey = aesUtils.decrypt(lineApp.getSpaKey());
            lineApp.setSpaKey(AesUtils.md5(plainSpaKey));
        }
    }
}
