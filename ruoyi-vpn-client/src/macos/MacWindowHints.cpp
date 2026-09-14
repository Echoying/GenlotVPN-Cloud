#include "MacWindowHints.h"

#include <QtGlobal>
#ifdef Q_OS_MACOS

#include <QEvent>
#include <QQuickWindow>
#include <QTimer>
#include <QWindow>
#include <QWindowStateChangeEvent>

#include <objc/message.h>
#include <objc/runtime.h>

namespace vpn {
namespace {

using MsgId = id (*)(id, SEL);
using MsgIdULong = id (*)(id, SEL, unsigned long);
using MsgVoidBool = void (*)(id, SEL, BOOL);
using MsgULong = unsigned long (*)(id, SEL);
using MsgVoidULong = void (*)(id, SEL, unsigned long);

// NSWindowZoomButton = 2；NSWindowCollectionBehaviorFullScreenNone = 1 << 9
constexpr unsigned long kNsWindowZoomButton = 2;
constexpr unsigned long kFullScreenNone = 1UL << 9;

void hideMacZoomButton(QWindow *window)
{
    if (!window) {
        return;
    }
    void *viewPtr = reinterpret_cast<void *>(window->winId());
    if (!viewPtr) {
        return;
    }
    id nsView = static_cast<id>(viewPtr);
    id nsWindow = reinterpret_cast<MsgId>(objc_msgSend)(nsView, sel_registerName("window"));
    if (!nsWindow) {
        return;
    }
    id zoomBtn = reinterpret_cast<MsgIdULong>(objc_msgSend)(
        nsWindow, sel_registerName("standardWindowButton:"), kNsWindowZoomButton);
    if (zoomBtn) {
        reinterpret_cast<MsgVoidBool>(objc_msgSend)(
            zoomBtn, sel_registerName("setHidden:"), YES);
    }
    unsigned long behavior = reinterpret_cast<MsgULong>(objc_msgSend)(
        nsWindow, sel_registerName("collectionBehavior"));
    behavior |= kFullScreenNone;
    reinterpret_cast<MsgVoidULong>(objc_msgSend)(
        nsWindow, sel_registerName("setCollectionBehavior:"), behavior);
}

class MacRestoreFilter : public QObject
{
public:
    explicit MacRestoreFilter(QWindow *window)
        : QObject(window)
        , m_window(window)
    {
    }

protected:
    bool eventFilter(QObject *obj, QEvent *event) override
    {
        if (obj != m_window) {
            return QObject::eventFilter(obj, event);
        }
        const bool minimized = m_window->windowStates() & Qt::WindowMinimized;
        if (event->type() == QEvent::Expose && m_window->isExposed() && !minimized) {
            refreshMacQuickWindow(m_window, false);
        }
        if (event->type() == QEvent::WindowStateChange
            && m_window->isVisible()
            && !minimized) {
            // 仅「从最小化恢复」才抖动尺寸：抖动本身会再派发 WindowStateChange，
            // 无条件处理会同步递归到栈溢出
            auto *stateEvent = static_cast<QWindowStateChangeEvent *>(event);
            if (stateEvent->oldState() & Qt::WindowMinimized) {
                scheduleNudge();
            }
        }
        return QObject::eventFilter(obj, event);
    }

private:
    // 推迟到事件循环下一轮，避免在窗口事件派发栈内同步改几何
    void scheduleNudge()
    {
        if (m_nudgeScheduled) {
            return;
        }
        m_nudgeScheduled = true;
        QTimer::singleShot(0, this, [this]() {
            m_nudgeScheduled = false;
            refreshMacQuickWindow(m_window, true);
        });
    }

    QWindow *m_window = nullptr;
    bool m_nudgeScheduled = false;
};

} // namespace

void refreshMacQuickWindow(QWindow *window, bool nudgeSize)
{
    auto *quick = qobject_cast<QQuickWindow *>(window);
    if (!quick) {
        return;
    }
    quick->requestUpdate();
    if (!nudgeSize) {
        return;
    }
    const QSize size = quick->size();
    if (size.width() <= 1 || size.height() <= 1) {
        return;
    }
    // resize 会同步派发窗口状态/曝光事件，重入会一路递归到栈溢出
    static bool nudging = false;
    if (nudging) {
        return;
    }
    nudging = true;
    quick->resize(size.width() + 1, size.height());
    quick->resize(size);
    nudging = false;
}

void applyMacWindowChrome(QWindow *window)
{
    if (!window) {
        return;
    }
    Qt::WindowFlags flags = window->flags();
    flags |= Qt::CustomizeWindowHint;
    flags |= Qt::WindowMinimizeButtonHint;
    flags |= Qt::WindowCloseButtonHint;
    flags &= ~Qt::WindowMaximizeButtonHint;
    flags &= ~Qt::WindowFullscreenButtonHint;
    window->setFlags(flags);

    hideMacZoomButton(window);
    QTimer::singleShot(0, window, [window]() {
        hideMacZoomButton(window);
    });

    if (!window->property("_genlotMacChrome").toBool()) {
        window->setProperty("_genlotMacChrome", true);
        window->installEventFilter(new MacRestoreFilter(window));
    }
}

} // namespace vpn

#endif
