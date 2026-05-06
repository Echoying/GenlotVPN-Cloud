package com.ruoyi.yianlian.controller;
import com.ruoyi.yianlian.client.dto.DeptListRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.yianlian.service.YiAnLianDeptService;

/**
 * 易安联Token信息
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/yianlian/dept")
public class YiAnLianDeptController extends BaseController
{
    @Autowired
    private YiAnLianDeptService yiAnLianTokenService;

    /**
     * 优先从缓存获取易安联token
     */
    @PostMapping("/dept")
    public AjaxResult getDeptList()
    {
        DeptListRequest request = new DeptListRequest();
        request.setPageIndex("0");
        request.setPageSize("10000");

        return success(yiAnLianTokenService.getDeptList(request));
    }
}
