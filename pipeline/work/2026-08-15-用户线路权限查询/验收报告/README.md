# 验收报告

管理端 Playwright 报告放这里，文件名：`验收报告-YYYY-MM-DD.md`。

每份至少包含：

- 对应 spec / 模块编号
- 打开的 URL（92 管理端）
- 断言项与通过/失败
- 截图路径或附件
- 回修轮次（0–2）

由 `python pipeline/bin/accept.py --run 2026-08-15-用户线路权限查询` **追加**写入同日文件，不覆盖已有人工整理。失败/通过结束时若已配置钉钉则自动通知。客户端不在此自动出报告，走人验 [PHASE2_CHECKLIST.md](../../../../ruoyi-vpn-client/docs/PHASE2_CHECKLIST.md)。
