package com.ruoyi.yianlian.service.sync.orchestrator;

/**
 * 判断补偿任务失败是否属于「永久错误」（不应继续重试）。
 */
public final class SyncRetryClassifier
{
    private SyncRetryClassifier()
    {
    }

    /** 错误信息命中以下片段时视为不可重试 */
    private static final String[] NON_RETRYABLE_MARKERS = {
        "请先将",
        "未配置代理登录用户",
        "未注册",
        "不支持的操作",
        "名称已存在",
        "已分配,不能删除",
        "已分配，不能删除",
        "不能重复选择",
        "登录账号已存在",
        "未同步到易安联",
        "本地用户已停用",
        "本地用户不存在",
        "代理登录用户",
        "线路不存在",
        "线路已停用",
        "无法解析本地用户密码",
        "归属部门与当前线路不一致",
        "上级部门不能是自己",
    };

    public static boolean isNonRetryable(Throwable e)
    {
        if (e == null)
        {
            return false;
        }
        if (e instanceof SyncNonRetryableException)
        {
            return true;
        }
        String msg = resolveMessage(e);
        if (msg == null || msg.isEmpty())
        {
            return false;
        }
        for (String marker : NON_RETRYABLE_MARKERS)
        {
            if (msg.contains(marker))
            {
                return true;
            }
        }
        return false;
    }

    private static String resolveMessage(Throwable e)
    {
        if (e.getMessage() != null && !e.getMessage().isEmpty())
        {
            return e.getMessage();
        }
        Throwable cause = e.getCause();
        if (cause != null && cause != e)
        {
            return cause.getMessage();
        }
        return null;
    }
}
