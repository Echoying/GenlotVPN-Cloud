package com.ruoyi.yianlian.controller;
import com.ruoyi.yianlian.api.domain.vo.YiAnLianDeptVO;
import com.ruoyi.yianlian.client.dto.DeptListRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.yianlian.service.YiAnLianDeptService;

import java.util.List;

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
     * 获取部门列表
     */
    @PostMapping("/dept")
    public AjaxResult getDeptList()
    {
        DeptListRequest request = new DeptListRequest();
        request.setPageIndex("0");
        request.setPageSize("10000");

        return success(yiAnLianTokenService.getDeptList(request));
    }

    /**
     * 创建部门
     */
    @PostMapping("/create")
    public AjaxResult create(@RequestBody YiAnLianDeptVO request)
    {
        return success(yiAnLianTokenService.create(request));
    }

    /**
     * 更新部门
     */
    @PostMapping("/update")
    public AjaxResult update(@RequestBody YiAnLianDeptVO request)
    {
        return success(yiAnLianTokenService.update(request));
    }

    /**
     * 删除部门
     */
    @PostMapping("/delete")
    public AjaxResult delete(@RequestBody List<String> ids)
    {
        return success(yiAnLianTokenService.delete(ids));
    }
}
