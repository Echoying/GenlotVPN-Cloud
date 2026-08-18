#pragma once

class QWindow;

namespace vpn {

#ifdef Q_OS_MACOS
/** 关掉绿钮 zoom/全屏，并在从最小化恢复时强制 QML 重绘 */
void applyMacWindowChrome(QWindow *window);
void refreshMacQuickWindow(QWindow *window, bool nudgeSize);
#else
inline void applyMacWindowChrome(QWindow *) {}
inline void refreshMacQuickWindow(QWindow *, bool) {}
#endif

} // namespace vpn
