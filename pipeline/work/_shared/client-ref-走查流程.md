# 客户端 AI 基础走查：登录流程（参考，≠ PHASE2）

跨功能约定。当次场景差异写在该次 spec 第 7 节。脚本：`pipeline/work/bin/accept_client_ref.py`，入口 `python -u pipeline/bin/accept.py client-ref`。

**不改客户端源码。** 不靠 objectName。窗口标题含 `Genlot VPN`。门禁只许写 **参考完成 / 参考失败**，禁止写「通过」。PHASE2 仍人勾。

## Agent 自己做（不把验证码交给人）

1. `--step prepare`：脚本启动或复用客户端，填账密，截整窗。
2. 读输出里的 `CAPTCHA_PATH`（或当次 `验收报告/client-ref-captcha-*.png`），**自己**认出右侧算式（如 `8-4=?`），算出数字。
3. `--step submit --code <数字>`：脚本回填并点登录。
4. 读 `RESULT_PATH` 与「探测」行，对照当次 spec 期望。认错或过期则 `--step refresh` 再认，**同一场景最多 2 次**。

账号只读 `envs.yaml` 的 `accept.client_user` / `accept.client_password`（环境变量 `CLIENT_E2E_*`）。对话和报告不写密码。

桌面 / 选线验证码保持开。管理端验证码只允许关 92 Nacos `security.captcha.enabled`。

## 桌面登录（`--scene off` / `on`）

```bash
python -u pipeline/bin/accept.py client-ref --run <当次> --scene off --step prepare
# Agent 读 CAPTCHA_PATH
python -u pipeline/bin/accept.py client-ref --run <当次> --scene off --step submit --code <算式结果>
```

脚本内顺序（不要手改客户端）：

1. 找到标题含 `Genlot VPN` 的窗；没有则启动 `ruoyi-vpn-client/dist/GenlotVPN-win64-*/GenlotVPN-*.exe`。
2. 若已在选线/已登录（窗变宽或变矮），先点右上角 **退出登录**，等到回到登录页再填。
3. 账号、密码**分别点击输入框，Ctrl+A 清空再填**。禁止 Tab 后往已有密码后面追加（会超 20 位，云端回「用户密码不在指定范围」）。
4. 截整窗。算式在验证码图里，不在「计算结果」输入框。
5. submit 再填一遍账密 + `--code`，点海军蓝 **登录**（客户区约 `0.50, 0.68`；脚本按 `#0B2D5B` 找按钮，勿点 `0.57` 那是复选框空隙）。
6. 等探测结果，再截 `client-ref-result-*.png`。

| 探测 | 含义 |
|------|------|
| `left-login-choose` / `left-login-wide` | 已离开登录页（选线或更宽主界面） |
| `upgrade-dialog` | 出现标题 **需要升级客户端** |
| `still-login-or-unknown` | 仍停在登录页（验证码错/过期、密码范围、未点到登录等） |

`on` 场景：**不要点**「打开下载页」。

## 管理端改策略（`--scene admin-set`）

无桌面验证码。`submit` 可不带 `--code`。用 `ADMIN_E2E_*`。

1. 打开 `/yianlian/clientVersion`（或当次 spec 路径）。
2. **等** `.el-loading-mask` 变为 hidden（`getPolicy` 返回）再拨开关。否则开关会被接口数据打回关闭。
3. 不要 `locator("input").first.fill`：第一个 input 是 Switch 的隐藏 checkbox。只填 `.el-form-item .el-input__inner`。
4. 点保存，断言 **保存成功**，且开关带 `is-checked`。

## 踩坑（已写进脚本，不要再踩）

| 现象 | 原因 | 处理 |
|------|------|------|
| 验证码已填、登录没动 | 点在约 0.57（复选框/空隙） | 点 0.68 或让脚本找海军蓝按钮 |
| 「用户密码不在指定范围」 | 记住密码未清空就追加 | 账号、密码都清空再填 |
| 「验证码已失效」 | 算式已刷新 | `refresh` 或再 `prepare`，按新图算 |
| 管理端保存成功但开关仍关 | 加载未完就拨开关 | 等 loading hidden 再拨，保存后再确认 `is-checked` |
| prepare 填到选线页 | 复用了已登录窗口 | 先退出登录 |
| 脚本已打印 `RESULT_PATH` 但进程不退 | Windows 下偶发挂起 | 以探测结果为准，可结束该进程后继续下一步 |

依赖：`pip install -r pipeline/bin/requirements-admin-e2e.txt`（含 pillow），管理端另需 `playwright install chromium`。仅 Windows 能跑桌面点击。
