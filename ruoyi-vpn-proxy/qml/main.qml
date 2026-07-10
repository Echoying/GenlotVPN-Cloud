import QtQuick
import QtQuick.Controls
import QtQuick.Window
import GenlotVPNProxy 1.0

ApplicationWindow {
    id: appWindow
    visibility: Window.Maximized
    width: Theme.appListWindowWidth
    height: Theme.appListWindowHeight
    minimumWidth: Theme.appListWindowMinWidth
    minimumHeight: Theme.appListWindowMinHeight
    title: qsTr("GenlotVPN Proxy")
    color: Theme.windowBg
    flags: Qt.Window | Qt.WindowTitleHint | Qt.WindowSystemMenuHint
           | Qt.WindowMinimizeButtonHint | Qt.WindowMaximizeButtonHint | Qt.WindowCloseButtonHint

    Component.onCompleted: {
        visibility = Window.Maximized
    }

    onClosing: function(close) {
        if (vpnTray.available) {
            close.accepted = false
            vpnTray.hideToTray()
        }
    }

    onVisibilityChanged: function(v) {
        if (vpnTray.available && v === Window.Minimized) {
            Qt.callLater(vpnTray.hideToTray)
        }
    }

    MainPage { anchors.fill: parent }
}
