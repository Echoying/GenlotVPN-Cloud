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
    title: "Genlot VPN"
    color: Theme.windowBg
    flags: Qt.Window | Qt.WindowTitleHint | Qt.WindowSystemMenuHint
           | Qt.WindowMinimizeButtonHint | Qt.WindowCloseButtonHint

    Component.onCompleted: {
        window.width = Theme.chooseLineWindowWidth
        window.height = Theme.chooseLineWindowHeight
        window.minimumWidth = Theme.chooseLineWindowMinWidth
        window.minimumHeight = Theme.chooseLineWindowMinHeight
        if (stackView.depth === 0)
            stackView.replace(choosePageComponent)
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
            if (page === "applist") {
                window.width = Theme.appListWindowWidth
                window.height = Theme.appListWindowHeight
                window.minimumWidth = Theme.appListWindowMinWidth
                window.minimumHeight = Theme.appListWindowMinHeight
                stackView.replace(appListPageComponent)
            } else {
                if (page === "choose") {
                    window.width = Theme.chooseLineWindowWidth
                    window.height = Theme.chooseLineWindowHeight
                    window.minimumWidth = Theme.chooseLineWindowMinWidth
                    window.minimumHeight = Theme.chooseLineWindowMinHeight
                } else {
                    window.width = Theme.windowWidth
                    window.height = Theme.windowHeight
                    window.minimumWidth = Theme.windowMinWidth
                    window.minimumHeight = Theme.windowMinHeight
                }
                if (page === "choose") stackView.replace(choosePageComponent)
                else if (page === "login") stackView.replace(loginPageComponent)
                else if (page === "connect") stackView.replace(connectPageComponent)
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
}
