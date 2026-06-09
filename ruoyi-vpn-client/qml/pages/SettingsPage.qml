import QtQuick
import QtQuick.Controls
import GenlotVPN 1.0

Item {
    id: root

    property var appWindow: Window.window
    property int currentSection: 0

    readonly property var sections: [
        { key: "server", title: "服务器" }
    ]

    function loadServerFields() {
        serverHostField.text = vpnFlow.serverHost || ""
        serverPortField.text = String(vpnFlow.serverPort || 9443)
    }

    function goBack() {
        if (appWindow) {
            appWindow.width = Theme.chooseLineWindowWidth
            appWindow.height = Theme.chooseLineWindowHeight
            appWindow.minimumWidth = Theme.chooseLineWindowMinWidth
            appWindow.minimumHeight = Theme.chooseLineWindowMinHeight
        }
        if (StackView.view)
            StackView.view.pop()
    }

    anchors.fill: parent

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
            border.color: Theme.settingsDivider
            border.width: 0
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
                text: "‹ 返回"
                emphasized: true
                onClicked: root.goBack()
            }

            Text {
                anchors.centerIn: parent
                text: "设置"
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

                Column {
                    anchors.fill: parent
                    anchors.margins: 24
                    spacing: 18

                    Text {
                        text: root.sections[root.currentSection].title
                        color: Theme.textPrimary
                        font.pixelSize: 16
                        font.bold: true
                    }

                    Column {
                        width: parent.width
                        spacing: 14
                        visible: root.currentSection === 0

                        Column {
                            width: parent.width
                            spacing: 6

                            Text {
                                text: "服务器地址"
                                color: Theme.textSecondary
                                font.pixelSize: 12
                            }

                            FlatField {
                                id: serverHostField
                                width: parent.width
                                placeholderText: "请输入服务器地址"
                            }
                        }

                        Column {
                            width: parent.width
                            spacing: 6

                            Text {
                                text: "端口"
                                color: Theme.textSecondary
                                font.pixelSize: 12
                            }

                            FlatField {
                                id: serverPortField
                                width: Math.min(parent.width, 160)
                                placeholderText: "9443"
                                digitsOnly: true
                                maximumLength: 5
                            }
                        }

                        PrimaryButton {
                            text: "保存"
                            width: 88
                            enabled: !vpnFlow.loading
                            onClicked: {
                                const port = parseInt(serverPortField.text, 10)
                                vpnFlow.applyServerConfig(serverHostField.text, port)
                            }
                        }
                    }
                }
            }
        }
    }

    onVisibleChanged: {
        if (visible)
            loadServerFields()
    }

    Component.onCompleted: loadServerFields()
}
