import QtQuick
import QtQuick.Controls
import GenlotVPN 1.0

ApplicationWindow {
    id: window
    width: Theme.windowWidth
    height: Theme.windowHeight
    minimumWidth: Theme.windowMinWidth
    minimumHeight: Theme.windowMinHeight
    visible: true
    title: vpnApp.windowTitle
    color: Theme.windowBg
    flags: Qt.Window | Qt.WindowTitleHint | Qt.WindowSystemMenuHint
           | Qt.WindowMinimizeButtonHint | Qt.WindowCloseButtonHint

    onClosing: function(close) {
        if (vpnTray.available) {
            close.accepted = false
            vpnTray.hideToTray()
        }
    }

    onVisibilityChanged: {
        if (vpnTray.available && visibility === Window.Minimized) {
            Qt.callLater(vpnTray.hideToTray)
        }
    }

    Component.onCompleted: {
        applyPageWindow("choose")
        if (stackView.depth === 0)
            stackView.replace(choosePageComponent)
    }

    // 先降低 minimum 再设宽高，否则从大窗口切回小窗口时无法缩小
    function applyWindowSize(w, h, minW, minH) {
        window.minimumWidth = minW
        window.minimumHeight = minH
        window.width = w
        window.height = h
    }

    function applyPageWindow(page) {
        if (page === "settings") {
            applyWindowSize(Theme.settingsWindowWidth, Theme.settingsWindowHeight,
                            Theme.settingsWindowMinWidth, Theme.settingsWindowMinHeight)
        } else if (page === "applist") {
            applyWindowSize(Theme.appListWindowWidth, Theme.appListWindowHeight,
                            Theme.appListWindowMinWidth, Theme.appListWindowMinHeight)
        } else if (page === "proxylogs") {
            applyWindowSize(Theme.appListWindowWidth, Theme.appListWindowHeight,
                            Theme.appListWindowMinWidth, Theme.appListWindowMinHeight)
        } else if (page === "choose") {
            applyWindowSize(Theme.chooseLineWindowWidth, Theme.chooseLineWindowHeight,
                            Theme.chooseLineWindowMinWidth, Theme.chooseLineWindowMinHeight)
        } else if (page === "login") {
            applyWindowSize(Theme.loginWindowWidth, Theme.loginWindowHeight,
                            Theme.loginWindowMinWidth, Theme.loginWindowMinHeight)
        } else {
            applyWindowSize(Theme.windowWidth, Theme.windowHeight,
                            Theme.windowMinWidth, Theme.windowMinHeight)
        }
    }

    function showToast(msg, isError) {
        toastLabel.text = msg
        toastIcon.text = isError ? "✕" : "✓"
        toastBar.isError = isError
        toastBar.visible = true
        toastTimer.interval = isError ? 5000 : 3000
        toastTimer.restart()
    }

    Connections {
        target: vpnFlow
        function onNavigateTo(page) {
            applyPageWindow(page)
            if (page === "settings") {
                stackView.push(settingsPageComponent)
                return
            }
            if (page === "proxylogs") {
                stackView.push(proxyLogPageComponent)
                return
            }
            if (page === "applist") {
                stackView.replace(appListPageComponent)
            } else if (page === "choose") {
                stackView.replace(choosePageComponent)
            } else if (page === "login") {
                stackView.replace(loginPageComponent)
            } else if (page === "connect") {
                stackView.replace(connectPageComponent)
            }
        }
        function onToast(message, isError) { window.showToast(message, isError) }
    }

    Rectangle {
        id: topBar
        visible: vpnFlow.loggedIn
        anchors.top: parent.top
        anchors.left: parent.left
        anchors.right: parent.right
        height: Theme.headerHeight
        color: Theme.windowBg
        z: 10

        Text {
            anchors.left: parent.left
            anchors.leftMargin: 16
            anchors.verticalCenter: parent.verticalCenter
            width: Math.min(180, window.width * 0.35)
            text: vpnFlow.username
            visible: vpnFlow.username.length > 0
            color: Theme.textPrimary
            font.pixelSize: 13
            font.bold: true
            elide: Text.ElideRight
        }

        GhostButton {
            anchors.right: parent.right
            anchors.rightMargin: 12
            anchors.verticalCenter: parent.verticalCenter
            text: "退出登录"
            danger: true
            onClicked: vpnFlow.doLogout()
        }
    }

    StackView {
        id: stackView
        anchors.top: topBar.visible ? topBar.bottom : parent.top
        anchors.left: parent.left
        anchors.right: parent.right
        anchors.bottom: parent.bottom
        clip: true
    }

    Rectangle {
        id: toastBar
        property bool isError: false
        visible: false
        anchors.horizontalCenter: parent.horizontalCenter
        anchors.bottom: parent.bottom
        anchors.bottomMargin: 16
        width: Math.min(parent.width - 48, 420)
        height: toastBar.isError ? 44 : 36
        radius: Theme.buttonRadius
        color: isError ? Theme.danger : Theme.navy
        z: 20

        Row {
            anchors.centerIn: parent
            anchors.margins: 8
            spacing: 8
            Text {
                id: toastIcon
                color: Theme.accentLight
                font.pixelSize: toastBar.isError ? 14 : 12
                font.bold: true
            }
            Text {
                id: toastLabel
                color: Theme.textOnPrimary
                font.pixelSize: toastBar.isError ? 13 : 12
                font.bold: toastBar.isError
                width: Math.min(implicitWidth, toastBar.width - 40)
                wrapMode: Text.Wrap
                horizontalAlignment: Text.AlignHCenter
            }
        }
    }

    Timer {
        id: toastTimer
        interval: 3000
        onTriggered: toastBar.visible = false
    }

    Component { id: choosePageComponent; ChooseLinePage {} }
    Component { id: loginPageComponent; LoginPage {} }
    Component { id: connectPageComponent; ConnectLinePage {} }
    Component { id: appListPageComponent; AppListPage {} }
    Component { id: proxyLogPageComponent; ProxyLogPage {} }
    Component { id: settingsPageComponent; SettingsPage {} }
}
