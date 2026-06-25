import QtQuick
import QtQuick.Controls
import GenlotVPN 1.0

Item {
    id: root

    property var appWindow: Window.window
    property int currentSection: 0

    readonly property var sections: [
        { key: "server", title: qsTr("服务器") },
        { key: "language", title: qsTr("语言") },
        { key: "security", title: qsTr("安全连接") },
        { key: "about", title: qsTr("关于") }
    ]

    readonly property int serverPortFieldWidth: 96
    readonly property int compactFieldWidth: Math.max(120, (panelColumn.width - 12) / 2)

    function formatDelaySeconds(delayMs) {
        const delaySec = delayMs / 1000
        return Number.isInteger(delaySec) ? String(delaySec) : delaySec.toFixed(1)
    }

    function loadFields() {
        serverHostField.text = vpnFlow.serverHost || ""
        serverPortField.text = String(vpnFlow.serverPort || 9443)
        tlsSwitch.checked = vpnFlow.serverUseTls
        pinField.text = vpnFlow.certPinSha256 || ""
        pinBackupField.text = vpnFlow.certPinSha256Backup || ""
        reconnectMaxField.text = String(vpnFlow.tcpReconnectMaxRetries)
        reconnectDelayField.text = formatDelaySeconds(vpnFlow.tcpReconnectDelayMs)
    }

    function saveSettings() {
        const port = parseInt(serverPortField.text, 10)
        if (!vpnFlow.applyServerConfig(
                serverHostField.text,
                port,
                tlsSwitch.checked,
                pinField.text,
                pinBackupField.text)) {
            return
        }

        const maxRetries = parseInt(reconnectMaxField.text, 10)
        const delaySec = parseFloat(String(reconnectDelayField.text).replace(",", "."))
        if (!isNaN(maxRetries) && !isNaN(delaySec)) {
            vpnFlow.applyTcpReconnectConfig(maxRetries, Math.round(delaySec * 1000))
        }
        loadFields()
    }

    function resetReconnectSettings() {
        vpnFlow.resetTcpReconnectToDefault()
        loadFields()
    }

    function goBack() {
        const backPage = vpnFlow.loggedIn ? "choose" : "login"
        if (appWindow && typeof appWindow.applyPageWindow === "function") {
            appWindow.applyPageWindow(backPage)
        } else if (appWindow) {
            if (backPage === "login") {
                appWindow.minimumWidth = Theme.loginWindowMinWidth
                appWindow.minimumHeight = Theme.loginWindowMinHeight
                appWindow.width = Theme.loginWindowWidth
                appWindow.height = Theme.loginWindowHeight
            } else {
                appWindow.minimumWidth = Theme.chooseLineWindowMinWidth
                appWindow.minimumHeight = Theme.chooseLineWindowMinHeight
                appWindow.width = Theme.chooseLineWindowWidth
                appWindow.height = Theme.chooseLineWindowHeight
            }
        }
        if (StackView.view)
            StackView.view.pop()
    }

    // StackView 子项勿设 anchors，由 StackView 自动铺满视口
    Rectangle {
        anchors.fill: parent
        color: Theme.windowBg
    }

    Column {
        anchors.fill: parent
        spacing: 0

        Rectangle {
            width: parent.width
            height: 44
            color: Theme.cardBg

            Rectangle {
                anchors.bottom: parent.bottom
                width: parent.width
                height: 1
                color: Theme.settingsDivider
            }

            GhostButton {
                anchors.left: parent.left
                anchors.leftMargin: 4
                anchors.verticalCenter: parent.verticalCenter
                text: qsTr("‹ 返回")
                emphasized: true
                onClicked: root.goBack()
            }

            Text {
                anchors.centerIn: parent
                text: qsTr("设置")
                color: Theme.textPrimary
                font.pixelSize: 17
                font.bold: true
            }
        }

        Row {
            width: parent.width
            height: parent.height - 44
            spacing: 0

            Rectangle {
                width: Theme.settingsNavWidth
                height: parent.height
                color: Theme.settingsSidebarBg

                Rectangle {
                    anchors.right: parent.right
                    width: 1
                    height: parent.height
                    color: Theme.settingsDivider
                }

                Column {
                    anchors.fill: parent
                    anchors.topMargin: 12
                    spacing: 4

                    Repeater {
                        model: root.sections.length
                        delegate: Rectangle {
                            required property int index
                            width: Theme.settingsNavWidth - 16
                            height: 40
                            x: 8
                            radius: Theme.buttonRadius
                            color: root.currentSection === index
                                   ? Theme.settingsSidebarActive
                                   : (navMouse.containsMouse ? Theme.listHover : "transparent")

                            Text {
                                anchors.left: parent.left
                                anchors.leftMargin: 14
                                anchors.verticalCenter: parent.verticalCenter
                                text: root.sections[index].title
                                color: root.currentSection === index
                                       ? Theme.navy
                                       : Theme.textSecondary
                                font.pixelSize: 13
                                font.bold: root.currentSection === index
                            }

                            Rectangle {
                                visible: root.currentSection === index
                                anchors.left: parent.left
                                anchors.verticalCenter: parent.verticalCenter
                                width: 2
                                height: 18
                                radius: 1
                                color: Theme.navySoft
                            }

                            MouseArea {
                                id: navMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                onClicked: root.currentSection = index
                            }
                        }
                    }
                }
            }

            Rectangle {
                width: parent.width - Theme.settingsNavWidth
                height: parent.height
                color: Theme.cardBg

                Flickable {
                    anchors.fill: parent
                    anchors.margins: 24
                    contentWidth: width
                    contentHeight: panelColumn.height
                    clip: true
                    boundsBehavior: Flickable.StopAtBounds

                    Column {
                        id: panelColumn
                        width: parent.width
                        spacing: 20

                        Text {
                            text: root.sections[root.currentSection].title
                            color: Theme.textPrimary
                            font.pixelSize: 16
                            font.bold: true
                        }

                        // —— 服务器 ——
                        Column {
                            width: parent.width
                            spacing: 16
                            visible: root.currentSection === 0

                            Row {
                                width: parent.width
                                spacing: 12

                                Column {
                                    width: parent.width - root.serverPortFieldWidth - 12
                                    spacing: 6

                                    Text {
                                        text: qsTr("服务器地址")
                                        color: Theme.textSecondary
                                        font.pixelSize: 12
                                    }

                                    FlatField {
                                        id: serverHostField
                                        width: parent.width
                                        placeholderText: qsTr("请输入服务器地址")
                                    }
                                }

                                Column {
                                    width: root.serverPortFieldWidth
                                    spacing: 6

                                    Text {
                                        text: qsTr("端口")
                                        color: Theme.textSecondary
                                        font.pixelSize: 12
                                    }

                                    FlatField {
                                        id: serverPortField
                                        width: parent.width
                                        placeholderText: "9443"
                                        digitsOnly: true
                                        maximumLength: 5
                                    }
                                }
                            }

                            Row {
                                width: parent.width
                                spacing: 12

                                Column {
                                    width: root.compactFieldWidth
                                    spacing: 6

                                    Text {
                                        text: qsTr("重试次数")
                                        color: Theme.textSecondary
                                        font.pixelSize: 12
                                    }

                                    FlatField {
                                        id: reconnectMaxField
                                        width: parent.width
                                        placeholderText: "3"
                                        digitsOnly: true
                                        maximumLength: 2
                                    }
                                }

                                Column {
                                    width: root.compactFieldWidth
                                    spacing: 6

                                    Text {
                                        text: qsTr("重试间隔（秒）")
                                        color: Theme.textSecondary
                                        font.pixelSize: 12
                                    }

                                    FlatField {
                                        id: reconnectDelayField
                                        width: parent.width
                                        placeholderText: "1.5"
                                    }
                                }
                            }

                            Rectangle {
                                width: parent.width
                                height: 1
                                color: Theme.settingsDivider
                            }

                            Text {
                                width: parent.width
                                text: qsTr("当前模式: %1").arg(vpnFlow.connectionModeLabel)
                                color: Theme.textPrimary
                                font.pixelSize: 12
                            }

                            Text {
                                width: parent.width
                                text: qsTr("云端 TCP 请求失败时自动重连，默认最多 3 次、间隔 1.5 秒。")
                                color: Theme.textSecondary
                                font.pixelSize: 11
                                wrapMode: Text.Wrap
                            }

                            Item {
                                width: parent.width
                                height: 40

                                GhostButton {
                                    anchors.left: parent.left
                                    anchors.verticalCenter: parent.verticalCenter
                                    text: qsTr("恢复默认")
                                    onClicked: root.resetReconnectSettings()
                                }

                                PrimaryButton {
                                    anchors.right: parent.right
                                    anchors.verticalCenter: parent.verticalCenter
                                    text: qsTr("保存")
                                    soft: true
                                    width: 88
                                    enabled: !vpnFlow.loading
                                    onClicked: root.saveSettings()
                                }
                            }
                        }

                        // —— 语言 ——
                        Column {
                            width: parent.width
                            spacing: 16
                            visible: root.currentSection === 1

                            FlatCombo {
                                id: localeCombo
                                width: parent.width
                                model: localeManager.availableLocales
                                textRole: "name"
                                currentIndex: localeManager.localeIndex(localeManager.currentLocale)

                                onActivated: {
                                    const locales = localeManager.availableLocales
                                    if (currentIndex >= 0 && currentIndex < locales.length)
                                        localeManager.setLocale(locales[currentIndex].id)
                                }
                            }
                        }

                        // —— 安全连接 ——
                        Column {
                            width: parent.width
                            spacing: 16
                            visible: root.currentSection === 2

                            Row {
                                width: parent.width
                                spacing: 10

                                Text {
                                    anchors.verticalCenter: parent.verticalCenter
                                    text: qsTr("启用 TLS")
                                    color: Theme.textPrimary
                                    font.pixelSize: 13
                                }

                                TlsSwitch {
                                    id: tlsSwitch
                                    anchors.verticalCenter: parent.verticalCenter
                                }
                            }

                            Column {
                                width: parent.width
                                spacing: 6

                                Text {
                                    text: qsTr("证书指纹（主）")
                                    color: Theme.textSecondary
                                    font.pixelSize: 12
                                }

                                FlatField {
                                    id: pinField
                                    width: parent.width
                                    placeholderText: qsTr("64 位十六进制 SPKI SHA-256")
                                    enabled: tlsSwitch.checked
                                }
                            }

                            Column {
                                width: parent.width
                                spacing: 6

                                Text {
                                    text: qsTr("证书指纹（备用，可选）")
                                    color: Theme.textSecondary
                                    font.pixelSize: 12
                                }

                                FlatField {
                                    id: pinBackupField
                                    width: parent.width
                                    placeholderText: qsTr("证书轮换时使用")
                                    enabled: tlsSwitch.checked
                                }
                            }

                            Text {
                                width: parent.width
                                text: qsTr("启用 TLS 后必须填写主指纹。导出方式见文档 TLS_PINNING.md")
                                color: Theme.textSecondary
                                font.pixelSize: 11
                                wrapMode: Text.Wrap
                            }

                            Item {
                                width: parent.width
                                height: 40

                                PrimaryButton {
                                    anchors.right: parent.right
                                    anchors.verticalCenter: parent.verticalCenter
                                    text: qsTr("保存")
                                    soft: true
                                    width: 88
                                    enabled: !vpnFlow.loading
                                    onClicked: root.saveSettings()
                                }
                            }
                        }

                        // —— 关于 ——
                        Column {
                            width: parent.width
                            spacing: 12
                            visible: root.currentSection === 3

                            Text {
                                text: "Genlot VPN"
                                color: Theme.navy
                                font.pixelSize: 18
                                font.bold: true
                            }

                            Text {
                                text: qsTr("版本 %1").arg(vpnApp.versionLabel)
                                color: Theme.textPrimary
                                font.pixelSize: 13
                            }

                            Text {
                                width: parent.width
                                text: qsTr("桌面客户端 · TLS/TCP 云端 + 易安联本地控制器")
                                color: Theme.textSecondary
                                font.pixelSize: 12
                                wrapMode: Text.Wrap
                            }

                            Text {
                                width: parent.width
                                text: "Copyright © Genlot"
                                color: Theme.textSecondary
                                font.pixelSize: 11
                            }
                        }
                    }
                }
            }
        }
    }

    Connections {
        target: localeManager
        function onLocaleChanged() {
            localeCombo.currentIndex = localeManager.localeIndex(localeManager.currentLocale)
        }
    }

    onVisibleChanged: {
        if (visible)
            loadFields()
    }

    Component.onCompleted: loadFields()
}
