package com.ruoyi.yianlian.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.security.annotation.InnerAuth;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.yianlian.domain.YianlianSyncTask;
import com.ruoyi.yianlian.service.sync.YiAnLianSyncRetryRunner;
import com.ruoyi.yianlian.service.sync.task.IYiAnLianSyncTaskService;

/**
 * 易安联同步补偿任务
 */
@RestController
public class YiAnLianSyncTaskController extends BaseController
{
    @Autowired
    private YiAnLianSyncRetryRunner syncRetryRunner;

    @Autowired
    private IYiAnLianSyncTaskService syncTaskService;

    /**
     * 执行补偿：按线路分批处理到期的待补偿任务（供 ruoyi-job 内部调用）
     */
    @InnerAuth
    @PostMapping("/sync-task/run-pending")
    public R<Integer> runPending(@RequestParam("maxAppIds") int maxAppIds,
                                 @RequestHeader(SecurityConstants.FROM_SOURCE) String source)
    {
        return R.ok(syncRetryRunner.runPending(maxAppIds));
    }

    /**
     * 分页查询同步任务列表（管理端）
     */
    @RequiresPermissions("yianlian:synctask:list")
    @GetMapping("/synctask/list")
    public TableDataInfo list(YianlianSyncTask query)
    {
        startPage();
        List<YianlianSyncTask> list = syncTaskService.selectSyncTaskList(query);
        return getDataTable(list);
    }

    /**
     * 查询同步任务详情（含 payload）
     */
    @RequiresPermissions("yianlian:synctask:query")
    @GetMapping("/synctask/{taskId}")
    public AjaxResult getInfo(@PathVariable Long taskId)
    {
        YianlianSyncTask task = syncTaskService.selectSyncTaskById(taskId);
        if (task == null)
        {
            return error("同步任务不存在");
        }
        return success(task);
    }
}
