#!/usr/bin/env python3
"""填充 genlotvpn_zh_CN.ts / genlotvpn_en.ts 翻译条目。"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ZH_TS = ROOT / "i18n" / "genlotvpn_zh_CN.ts"
EN_TS = ROOT / "i18n" / "genlotvpn_en.ts"

EN_MAP = {
    "当前线路": "Current Line",
    "同步代理日志": "Sync Proxy Logs",
    "同步代理服务": "Sync Proxy Service",
    "同步代理: %1": "Sync Proxy: %1",
    "未启动": "Not started",
    "应用网关": "Application Gateways",
    "名称": "Name",
    "网关 IP": "Gateway IP",
    "状态": "Status",
    "操作": "Action",
    "切换中": "Switching",
    "已连接": "Connected",
    "未连接": "Disconnected",
    "切换": "Switch",
    "当前": "Current",
    "应用列表": "Applications",
    "应用名称": "App Name",
    "复制": "Copy",
    "暂无可用应用": "No applications available",
    "请填写登录用途": "Please enter login purpose",
    "登录用途至少填写5个字（还需 %1 字）": "Purpose must be at least 5 characters (%1 more needed)",
    "已达 50 字上限": "Maximum 50 characters reached",
    "已输入 %1/50 字": "Entered %1/50 characters",
    "请选择要连接的 VPN 线路": "Select a VPN line to connect",
    "正在加载授权线路...": "Loading authorized lines...",
    "正在加载线路...": "Loading lines...",
    "登录用途": "Login Purpose",
    "请输入登录用途": "Enter login purpose",
    "确定": "OK",
    "取消": "Cancel",
    "正在连接 %1": "Connecting to %1",
    "连接中...": "Connecting...",
    "选线安全验证": "Line Security Verification",
    "请输入钉钉群收到的 6 位验证码": "Enter the 6-digit code from DingTalk",
    "6位数字验证码": "6-digit code",
    "重新发送 (%1s)": "Resend (%1s)",
    "发送验证码": "Send Code",
    "点击选择线路": "Tap to select a line",
    "请选择线路": "Please select a line",
    "账号": "Account",
    "密码": "Password",
    "计算结果": "Result",
    "记住密码": "Remember password",
    "登 录": "Log In",
    "修改密码": "Change Password",
    "设置": "Settings",
    "用户名": "Username",
    "旧密码": "Current password",
    "新密码（5-20 位）": "New password (5-20 chars)",
    "确认新密码": "Confirm new password",
    "Genlot VPN": "Genlot VPN",
    "Genlot VPN 已在运行中。": "Genlot VPN is already running.",
    "通过 vpnFlow.proxyLogs 访问": "Access via vpnFlow.proxyLogs",
    "界面加载失败，请确认安装目录下存在 GenlotVPN 文件夹。\n详细日志见 logs 目录。": (
        "Failed to load UI. Ensure the GenlotVPN folder exists next to the executable.\n"
        "See the logs directory for details."
    ),
    "返回": "Back",
    "清空": "Clear",
    "监听地址: %1": "Listen address: %1",
    "时间": "Time",
    "方法": "Method",
    "路径": "Path",
    "来源 IP": "Source IP",
    "暂无代理请求记录": "No proxy requests yet",
    "请求日志": "Request Log",
    "选中上方记录查看请求详情": "Select a row above to view request details",
    "响应日志": "Response Log",
    "选中上方记录查看响应详情": "Select a row above to view response details",
    "服务器": "Server",
    "语言": "Language",
    "安全连接": "Security",
    "关于": "About",
    "‹ 返回": "‹ Back",
    "服务器地址": "Server address",
    "请输入服务器地址": "Enter server address",
    "端口": "Port",
    "重试次数": "Retry count",
    "重试间隔（秒）": "Retry interval (seconds)",
    "当前模式: %1": "Current mode: %1",
    "云端 TCP 请求失败时自动重连，默认最多 3 次、间隔 1.5 秒。": (
        "Automatically retries failed cloud TCP requests, up to 3 times by default with a 1.5s interval."
    ),
    "恢复默认": "Restore defaults",
    "保存": "Save",
    "界面语言": "Display language",
    "切换后立即生效，无需重启。": "Takes effect immediately without restart.",
    "启用 TLS": "Enable TLS",
    "证书指纹（主）": "Certificate pin (primary)",
    "64 位十六进制 SPKI SHA-256": "64-char hex SPKI SHA-256",
    "证书指纹（备用，可选）": "Certificate pin (backup, optional)",
    "证书轮换时使用": "Used during certificate rotation",
    "启用 TLS 后必须填写主指纹。导出方式见文档 TLS_PINNING.md": (
        "Primary pin is required when TLS is enabled. See TLS_PINNING.md for export steps."
    ),
    "版本 %1": "Version %1",
    "桌面客户端 · TLS/TCP 云端 + 易安联本地控制器": (
        "Desktop client · TLS/TCP cloud + local YianLian controller"
    ),
    "TLS 连接失败，请检查证书指纹与服务端 TLS 配置": (
        "TLS connection failed. Check certificate pin and server TLS settings."
    ),
    "网络连接失败": "Network connection failed",
    "TLS 握手失败（Windows 与服务器 TLS 特性不兼容）。请重启 ruoyi-vpn-auth 使服务端支持 TLS 1.2+1.3，并确认 config.json 中 useTls 为 true。": (
        "TLS handshake failed (Windows/server TLS mismatch). Restart ruoyi-vpn-auth for TLS 1.2+1.3 "
        "and ensure useTls is true in config.json."
    ),
    "TLS 握手失败（%1）": "TLS handshake failed (%1)",
    "连接失败：服务端已启用 TLS，请在 config.json 设 useTls 为 true 并填写 certPinSha256。": (
        "Connection failed: server uses TLS. Set useTls to true and certPinSha256 in config.json."
    ),
    "退出登录": "Log Out",
    "打开主窗口": "Open Main Window",
    "退出": "Quit",
    "服务器未返回可用线路，请确认 yianlian 模块线路已启用": (
        "No lines returned. Ensure yianlian module lines are enabled."
    ),
    "共 %1 条线路": "%1 line(s) available",
    "暂无授权线路，请联系管理员同步": "No authorized lines. Contact your administrator.",
    "共 %1 条授权线路": "%1 authorized line(s)",
    "您无权访问所选线路": "You are not authorized for the selected line",
    "验证码已发送到 VPN 群": "Verification code sent to VPN group",
    "验证通过": "Verification successful",
    "密码修改成功，请使用新密码登录": "Password changed. Please log in with the new password.",
    "登录失败，请重试": "Login failed. Please try again.",
    "发送验证码失败，请重试": "Failed to send code. Please try again.",
    "验证码校验失败，请重试": "Invalid or expired code. Please try again.",
    "验证失败，请重试": "Verification failed. Please try again.",
    "请求失败，请重试": "Request failed. Please try again.",
    "切换网关失败": "Failed to switch gateway",
    "切换网关失败：%1": "Failed to switch gateway: %1",
    "正在连接 %1 ...": "Connecting to %1 ...",
    "请选择有效线路": "Please select a valid line",
    "登录用途至少填写5个字": "Login purpose must be at least 5 characters",
    "登录用途不能超过50个字": "Login purpose cannot exceed 50 characters",
    "请先在选线页填写登录用途": "Please enter login purpose on the line selection page first",
    "请输入6位数字验证码": "Please enter a 6-digit code",
    "已切换到 %1": "Switched to %1",
    "切换网关后连接超时": "Connection timed out after gateway switch",
    "网关连接超时": "Gateway connection timed out",
    "登录已过期，请重新登录": "Session expired. Please log in again.",
    "请填写完整信息": "Please fill in all fields",
    "两次输入的密码不一致": "Passwords do not match",
    "新密码不能与旧密码相同": "New password must differ from the old one",
    "密码长度在 5 到 20 个字符": "Password must be 5 to 20 characters",
    "同步代理已启动 %1": "Sync proxy started at %1",
    "同步代理启动失败": "Failed to start sync proxy",
    "链接已复制": "Link copied",
    "TLS（未配置指纹）": "TLS (no pin configured)",
    "明文 TCP": "Plain TCP",
    "请输入有效端口号（1-65535）": "Enter a valid port (1-65535)",
    "启用 TLS 时必须填写证书指纹": "Certificate pin is required when TLS is enabled",
    "证书指纹须为 64 位十六进制": "Certificate pin must be 64 hex characters",
    "保存 config.json 失败": "Failed to save config.json",
    "服务器 %1:%2（%3）": "Server %1:%2 (%3)",
    "重试次数须在 0-10 之间": "Retry count must be between 0 and 10",
    "重试间隔须在 0.5-60 秒之间": "Retry interval must be between 0.5 and 60 seconds",
    "连接设置已保存": "Connection settings saved",
    "已恢复默认重连设置": "Default reconnect settings restored",
    "离线登录": "Offline Login",
    "离线": "Offline",
    "选择离线登录文件": "Select Offline Login File",
    "离线登录文件 (*.dat)": "Offline Login Files (*.dat)",
    "离线登录文件无效": "Invalid offline login file",
    "无法打开文件": "Cannot open file",
    "离线登录文件格式无效": "Invalid offline login file format",
    "离线登录文件缺少线路信息": "Offline login file is missing line information",
    "离线登录文件端口无效": "Invalid port in offline login file",
    "离线登录文件缺少 spa_key": "Offline login file is missing spa_key",
    "离线登录文件缺少账号或密码": "Offline login file is missing username or password",
    "离线登录文件缺少有效时间": "Offline login file is missing expiry time",
    "离线登录文件有效时间格式无效": "Invalid expiry time format in offline login file",
    "离线凭证已过期，请重新导出": "Offline credentials have expired. Please export again",
    "内部错误": "Internal error",
    "请选择离线登录线路": "Select an offline login line",
    "正在加载离线线路...": "Loading offline lines...",
    "请将管理端导出的 .dat 文件放入：%1": "Place exported .dat files from the admin console in: %1",
    "未找到可用的离线登录文件": "No valid offline login files found",
    "无法创建离线登录目录": "Cannot create offline login directory",
    "离线登录目录不存在": "Offline login directory does not exist",
    "界面加载失败，请查看 logs 目录中的 [QML] 日志。": "Failed to load UI. Check [QML] entries in the logs folder.",
}


def fill_ts(path: Path, locale: str) -> None:
    text = path.read_text(encoding="utf-8")

    def repl(match: re.Match) -> str:
        source = match.group(1)
        if locale == "zh_CN":
            translation = source
        else:
            translation = EN_MAP.get(source, source)
        return f"<source>{source}</source>\n        <translation>{translation}</translation>"

    pattern = re.compile(
        r"<source>(.*?)</source>\s*<translation[^>]*>.*?</translation>",
        re.DOTALL,
    )
    new_text, count = pattern.subn(repl, text)
    if count == 0:
        print(f"[!] No messages updated in {path}")
        sys.exit(1)
    path.write_text(new_text, encoding="utf-8")
    print(f"[OK] Updated {count} messages in {path.name}")


def main() -> None:
    fill_ts(ZH_TS, "zh_CN")
    fill_ts(EN_TS, "en")
    missing = []
    for src in re.findall(r"<source>(.*?)</source>", ZH_TS.read_text(encoding="utf-8")):
        if src not in EN_MAP and any("\u4e00" <= c <= "\u9fff" for c in src):
            missing.append(src)
    if missing:
        print(f"[!] Missing EN_MAP entries: {len(missing)}")
        for item in missing[:10]:
            print(f"  - {item}")
        sys.exit(1)


if __name__ == "__main__":
    main()
