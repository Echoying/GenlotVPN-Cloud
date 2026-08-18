#ifdef Q_OS_MACOS

#include "MacWindowHints.h"

#include <QEvent>
#include <QQuickWindow>
#include <QTimer>
#include <QWindow>

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
        if (event->type() == QEvent::Expose && m_window->isExposed() && !m_window->isMinimized()) {
            refreshMacQuickWindow(m_window, false);
        }
        if (event->type() == QEvent::WindowStateChange
            && m_window->isVisible()
            && !m_window->isMinimized()) {
            refreshMacQuickWindow(m_window, true);
        }
        return QObject::eventFilter(obj, event);
    }

private:
    QWindow *m_window = nullptr;
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
    quick->resize(size.width() + 1, size.height());
    quick->resize(size);
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
