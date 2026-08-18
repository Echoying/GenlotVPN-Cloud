package com.ruoyi.yianlian.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.InnerAuth;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.yianlian.api.domain.VpnFeedbackCreateDTO;
import com.ruoyi.yianlian.domain.VpnFeedback;
import com.ruoyi.yianlian.service.vpn.IVpnFeedbackService;

/**
 * VPN 问题反馈
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/vpn/feedback")
public class VpnFeedbackController extends BaseController
{
    @Autowired
    private IVpnFeedbackService feedbackService;

    /**
     * 查询问题反馈列表
     */
    @RequiresPermissions("vpn:feedback:list")
    @GetMapping("/list")
    public TableDataInfo list(VpnFeedback feedback)
    {
        startPage();
        List<VpnFeedback> list = feedbackService.selectFeedbackList(feedback);
        return getDataTable(list);
    }

    /**
     * 查询问题反馈详情
     */
    @RequiresPermissions("vpn:feedback:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(feedbackService.selectFeedbackById(id));
    }

    /**
     * 修改问题反馈状态（只认 id、status）
     */
    @RequiresPermissions("vpn:feedback:edit")
    @Log(title = "问题反馈", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody VpnFeedback feedback)
    {
        return toAjax(feedbackService.updateStatus(feedback.getId(), feedback.getStatus()));
    }

    /**
     * 内部接口：供 vpn-auth 落库
     */
    @InnerAuth
    @PostMapping("/inner")
    public R<Long> create(@RequestBody VpnFeedbackCreateDTO dto)
    {
        return R.ok(feedbackService.create(dto));
    }
}
