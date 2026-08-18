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
           // 不含 Maximize / Fullscreen：macOS 绿钮是 zoom/全屏，必须一起关掉

    property int savedWindowWidth: 0
    property int savedWindowHeight: 0
    property int savedWindowMinWidth: 0
    property int savedWindowMinHeight: 0
    property int lastVisibility: Window.Windowed

    onClosing: function(close) {
        if (vpnTray.available) {
            close.accepted = false
            vpnTray.hideToTray()
        }
    }

    onVisibilityChanged: {
        const wasMinimized = lastVisibility === Window.Minimized
        lastVisibility = visibility
        // macOS：最小化走系统窗口，不要 hide() 进托盘，否则 Dock 点回来 QML 常空白
        if (Qt.platform.os === "osx" || Qt.platform.os === "macos") {
            if (wasMinimized
                    && (visibility === Window.Windowed
                        || visibility === Window.AutomaticVisibility)) {
                Qt.callLater(function() {
                    if (!window.visible)
                        return
                    window.requestActivate()
                    const w = window.width
                    window.width = w + 1
                    window.width = w
                })
            }
            return
        }
        if (vpnTray.available && visibility === Window.Minimized) {
            Qt.callLater(vpnTray.hideToTray)
        }
    }

    Component.onCompleted: {
        applyPageWindow("login")
        if (stackView.depth === 0)
            stackView.replace(loginPageComponent)
    }

    // 先降低 minimum 再设宽高，否则从大窗口切回小窗口时无法缩小
    function applyWindowSize(w, h, minW, minH) {
        window.minimumWidth = minW
        window.minimumHeight = minH
        window.width = w
        window.height = h
    }

    function saveWindowSize() {
        savedWindowWidth = window.width
        savedWindowHeight = window.height
        savedWindowMinWidth = window.minimumWidth
        savedWindowMinHeight = window.minimumHeight
    }

    function restoreWindowSize() {
        if (savedWindowWidth <= 0 || savedWindowHeight <= 0)
            return
        applyWindowSize(savedWindowWidth, savedWindowHeight,
                        savedWindowMinWidth, savedWindowMinHeight)
        savedWindowWidth = 0
        savedWindowHeight = 0
    }

    function applyPageWindow(page) {
        if (page === "settings") {
            applyWindowSize(
                Math.max(window.width, Theme.settingsWindowWidth),
                Math.max(window.height, Theme.settingsWindowHeight),
                Math.max(window.minimumWidth, Theme.settingsWindowMinWidth),
                Math.max(window.minimumHeight, Theme.settingsWindowMinHeight))
        } else if (page === "applist") {
            applyWindowSize(Theme.appListWindowWidth, Theme.appListWindowHeight,
                            Theme.appListWindowMinWidth, Theme.appListWindowMinHeight)
        } else if (page === "choose") {
            applyWindowSize(Theme.chooseLineWindowWidth, Theme.chooseLineWindowHeight,
                            Theme.chooseLineWindowMinWidth, Theme.chooseLineWindowMinHeight)
        } else if (page === "offlinechoose") {
            applyWindowSize(Theme.chooseLineWindowWidth, Theme.chooseLineWindowHeight,
                            Theme.chooseLineWindowMinWidth, Theme.chooseLineWindowMinHeight)
        } else if (page === "login") {
            applyWindowSize(Theme.loginWindowWidth, Theme.loginWindowHeight,
                            Theme.loginWindowMinWidth, Theme.loginWindowMinHeight)
        } else if (page === "connect") {
            applyWindowSize(Theme.connectWindowWidth, Theme.connectWindowHeight,
                            Theme.connectWindowMinWidth, Theme.connectWindowMinHeight)
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
            if (page === "settings") {
                if (stackView.currentItem && stackView.currentItem.objectName === "settingsPage")
                    return
                saveWindowSize()
                applyWindowSize(
                    Math.max(window.width, Theme.settingsWindowWidth),
                    Math.max(window.height, Theme.settingsWindowHeight),
                    Math.max(window.minimumWidth, Theme.settingsWindowMinWidth),
                    Math.max(window.minimumHeight, Theme.settingsWindowMinHeight))
                stackView.push(settingsPageComponent)
                return
            }
            applyPageWindow(page)
            if (page === "applist") {
                stackView.replace(appListPageComponent)
            } else if (page === "choose") {
                stackView.replace(choosePageComponent)
            } else if (page === "offlinechoose") {
                stackView.replace(offlineChoosePageComponent)
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
        visible: vpnFlow.loggedIn || vpnFlow.offlineMode
        anchors.top: parent.top
        anchors.left: parent.left
        anchors.right: parent.right
        height: Theme.headerHeight
        color: Theme.windowBg
        z: 10

        GhostButton {
            anchors.left: parent.left
            anchors.leftMargin: 8
            anchors.verticalCenter: parent.verticalCenter
            text: qsTr("设置")
            emphasized: true
            onClicked: vpnFlow.goToSettings()
        }

        Text {
            anchors.horizontalCenter: parent.horizontalCenter
            anchors.verticalCenter: parent.verticalCenter
            text: vpnFlow.username
            visible: vpnFlow.username.length > 0
            color: Theme.textPrimary
            font.pixelSize: 16
            font.bold: true
            elide: Text.ElideRight
            width: Math.min(240, window.width * 0.42)
            horizontalAlignment: Text.AlignHCenter
        }

        GhostButton {
            anchors.right: parent.right
            anchors.rightMargin: 12
            anchors.verticalCenter: parent.verticalCenter
            text: qsTr("退出登录")
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
    Component { id: offlineChoosePageComponent; OfflineChooseLinePage {} }
    Component { id: loginPageComponent; LoginPage {} }
    Component { id: connectPageComponent; ConnectLinePage {} }
    Component { id: appListPageComponent; AppListPage {} }
    Component { id: settingsPageComponent; SettingsPage {} }
}

