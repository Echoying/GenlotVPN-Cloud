import QtQuick
import QtQuick.Controls
import GenlotVPN 1.0

Item {
    id: root

    property var appWindow: Window.window
    property int selectedIndex: -1

    readonly property int headerHeight: 44
    readonly property int statusBarHeight: vpnFlow.syncProxyRunning ? 28 : 0
    readonly property int tableHeaderHeight: 34

    function goBack() {
        if (appWindow && typeof appWindow.applyPageWindow === "function") {
            appWindow.applyPageWindow("applist")
        }
        if (StackView.view)
            StackView.view.pop()
    }

    function selectRow(index) {
        selectedIndex = index
    }

    Connections {
        target: vpnFlow.proxyLogs
        function onEntryAdded() {
            root.selectedIndex = 0
        }
    }

    Rectangle {
        anchors.fill: parent
        color: Theme.windowBg
    }

    Column {
        anchors.fill: parent
        spacing: 0

        Rectangle {
            width: parent.width
            height: headerHeight
            color: Theme.cardBg

            Rectangle {
                anchors.bottom: parent.bottom
                width: parent.width
                height: 1
                color: Theme.cardBorder
            }

            GhostButton {
                anchors.left: parent.left
                anchors.leftMargin: 8
                anchors.verticalCenter: parent.verticalCenter
                text: qsTr("返回")
                onClicked: root.goBack()
            }

            Text {
                anchors.centerIn: parent
                text: qsTr("同步代理日志")
                font.pixelSize: 15
                font.bold: true
                color: Theme.navy
            }

            GhostButton {
                anchors.right: parent.right
                anchors.rightMargin: 8
                anchors.verticalCenter: parent.verticalCenter
                text: qsTr("清空")
                onClicked: vpnFlow.clearProxyLogs()
            }
        }

        Rectangle {
            width: parent.width
            height: statusBarHeight
            color: Theme.listHover
            visible: height > 0

            Text {
                anchors.left: parent.left
                anchors.leftMargin: 12
                anchors.verticalCenter: parent.verticalCenter
                text: qsTr("监听地址: %1").arg(vpnFlow.syncProxyEndpoint || "-")
                font.pixelSize: 11
                color: Theme.textSecondary
            }
        }

        Rectangle {
            width: parent.width
            height: tableHeaderHeight
            color: Theme.listHover
            border.color: Theme.cardBorder
            border.width: 1

            Row {
                anchors.fill: parent
                anchors.leftMargin: 8
                anchors.rightMargin: 8

                Item {
                    width: parent.width * 0.20
                    height: parent.height
                    Text {
                        anchors.centerIn: parent
                        width: parent.width
                        text: qsTr("时间")
                        font.pixelSize: 11
                        font.bold: true
                        color: Theme.textSecondary
                        horizontalAlignment: Text.AlignHCenter
                    }
                }
                Item {
                    width: parent.width * 0.10
                    height: parent.height
                    Text {
                        anchors.centerIn: parent
                        text: qsTr("方法")
                        font.pixelSize: 11
                        font.bold: true
                        color: Theme.textSecondary
                        horizontalAlignment: Text.AlignHCenter
                    }
                }
                Item {
                    width: parent.width * 0.34
                    height: parent.height
                    Text {
                        anchors.centerIn: parent
                        text: qsTr("URL")
                        font.pixelSize: 11
                        font.bold: true
                        color: Theme.textSecondary
                        horizontalAlignment: Text.AlignHCenter
                    }
                }
                Item {
                    width: parent.width * 0.10
                    height: parent.height
                    Text {
                        anchors.centerIn: parent
                        text: qsTr("状态")
                        font.pixelSize: 11
                        font.bold: true
                        color: Theme.textSecondary
                        horizontalAlignment: Text.AlignHCenter
                    }
                }
                Item {
                    width: parent.width * 0.16
                    height: parent.height
                    Text {
                        anchors.centerIn: parent
                        text: qsTr("来源 IP")
                        font.pixelSize: 11
                        font.bold: true
                        color: Theme.textSecondary
                        horizontalAlignment: Text.AlignHCenter
                    }
                }
            }
        }

        Item {
            width: parent.width
            height: parent.height - headerHeight - statusBarHeight - tableHeaderHeight

            Column {
                anchors.fill: parent
                spacing: 8

                Rectangle {
                    width: parent.width
                    height: parent.height * 0.42
                    color: Theme.cardBg
                    border.color: Theme.cardBorder
                    border.width: 1
                    clip: true

                    ListView {
                        id: logListView
                        anchors.fill: parent
                        anchors.margins: 1
                        clip: true
                        model: vpnFlow.proxyLogs

                        delegate: Rectangle {
                            width: logListView.width
                            height: 36
                            color: root.selectedIndex === index
                                   ? "#EEF4FB"
                                   : (index % 2 === 0 ? Theme.listStripeA : Theme.listStripeB)

                            Row {
                                anchors.fill: parent
                                anchors.leftMargin: 8
                                anchors.rightMargin: 8

                                Item {
                                    width: parent.width * 0.20
                                    height: parent.height
                                    Text {
                                        anchors.centerIn: parent
                                        width: parent.width
                                        text: model.time || "-"
                                        font.pixelSize: 10
                                        color: Theme.textSecondary
                                        horizontalAlignment: Text.AlignHCenter
                                        elide: Text.ElideRight
                                    }
                                }
                                Item {
                                    width: parent.width * 0.10
                                    height: parent.height
                                    Text {
                                        anchors.centerIn: parent
                                        text: model.method || "-"
                                        font.pixelSize: 11
                                        font.bold: true
                                        color: Theme.textPrimary
                                    }
                                }
                                Item {
                                    width: parent.width * 0.34
                                    height: parent.height
                                    Text {
                                        anchors.centerIn: parent
                                        width: parent.width
                                        text: model.path || "-"
                                        font.pixelSize: 11
                                        color: Theme.textPrimary
                                        horizontalAlignment: Text.AlignHCenter
                                        elide: Text.ElideRight
                                    }
                                }
                                Item {
                                    width: parent.width * 0.10
                                    height: parent.height
                                    Text {
                                        anchors.centerIn: parent
                                        text: model.status > 0 ? String(model.status) : "-"
                                        font.pixelSize: 11
                                        font.bold: true
                                        color: model.success ? Theme.success : Theme.danger
                                    }
                                }
                                Item {
                                    width: parent.width * 0.16
                                    height: parent.height
                                    Text {
                                        anchors.centerIn: parent
                                        width: parent.width
                                        text: model.peerIp || "-"
                                        font.pixelSize: 10
                                        color: Theme.textSecondary
                                        horizontalAlignment: Text.AlignHCenter
                                        elide: Text.ElideRight
                                    }
                                }
                            }

                            MouseArea {
                                anchors.fill: parent
                                onClicked: root.selectRow(index)
                            }

                            Rectangle {
                                anchors.bottom: parent.bottom
                                width: parent.width
                                height: 1
                                color: Theme.cardBorder
                                opacity: 0.35
                            }
                        }
                    }

                    Text {
                        anchors.centerIn: parent
                        visible: !vpnFlow.proxyLogs || vpnFlow.proxyLogs.count === 0
                        text: qsTr("暂无代理请求记录")
                        color: Theme.textSecondary
                        font.pixelSize: 13
                    }
                }

                Row {
                    width: parent.width
                    height: parent.height * 0.58 - parent.spacing
                    spacing: 8

                    Column {
                        width: (parent.width - parent.spacing) / 2
                        height: parent.height
                        spacing: 6

                        Text {
                            text: qsTr("请求日志")
                            font.pixelSize: 12
                            font.bold: true
                            color: Theme.navy
                        }

                        ScrollView {
                            width: parent.width
                            height: parent.height - 22
                            clip: true

                            TextArea {
                                readOnly: true
                                wrapMode: TextArea.Wrap
                                font.family: "Consolas"
                                font.pixelSize: 11
                                color: Theme.logText
                                text: root.selectedIndex >= 0 && vpnFlow.proxyLogs
                                      ? vpnFlow.proxyLogs.entryAt(root.selectedIndex).requestLog
                                      : qsTr("选中上方记录查看请求详情")
                                background: Rectangle {
                                    radius: Theme.buttonRadius
                                    color: Theme.logBg
                                    border.color: Theme.logBorder
                                    border.width: 1
                                }
                            }
                        }
                    }

                    Column {
                        width: (parent.width - parent.spacing) / 2
                        height: parent.height
                        spacing: 6

                        Text {
                            text: qsTr("响应日志")
                            font.pixelSize: 12
                            font.bold: true
                            color: Theme.navy
                        }

                        ScrollView {
                            width: parent.width
                            height: parent.height - 22
                            clip: true

                            TextArea {
                                readOnly: true
                                wrapMode: TextArea.Wrap
                                font.family: "Consolas"
                                font.pixelSize: 11
                                color: Theme.logText
                                text: root.selectedIndex >= 0 && vpnFlow.proxyLogs
                                      ? vpnFlow.proxyLogs.entryAt(root.selectedIndex).responseLog
                                      : qsTr("选中上方记录查看响应详情")
                                background: Rectangle {
                                    radius: Theme.buttonRadius
                                    color: Theme.logBg
                                    border.color: Theme.logBorder
                                    border.width: 1
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
