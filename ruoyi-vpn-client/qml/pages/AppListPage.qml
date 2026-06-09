import QtQuick
import QtQuick.Controls
import GenlotVPN 1.0

Item {
    id: root

    PageBackground { anchors.fill: parent }

    Column {
        id: headerColumn
        anchors.top: parent.top
        anchors.left: parent.left
        anchors.right: parent.right
        anchors.margins: 16
        spacing: 12

        Row {
            width: parent.width
            spacing: 12

            Text {
                width: parent.width
                anchors.verticalCenter: parent.verticalCenter
                text: vpnFlow.selectedLine.appName || vpnFlow.pendingLine.appName || "当前线路"
                color: Theme.navy
                font.pixelSize: 17
                font.bold: true
                elide: Text.ElideRight
            }
        }

        Text {
            text: "应用网关"
            font.bold: true
            font.pixelSize: 15
            color: Theme.navy
            visible: vpnFlow.gateways.length > 0
        }

        Column {
            width: parent.width
            spacing: 0
            visible: vpnFlow.gateways.length > 0

            Rectangle {
                width: parent.width
                height: 34
                radius: Theme.buttonRadius
                color: Theme.listHover
                border.color: Theme.cardBorder
                border.width: 1

                Row {
                    anchors.fill: parent
                    anchors.leftMargin: 12
                    anchors.rightMargin: 8

                    Item {
                        width: parent.width * 0.22
                        height: parent.height
                        Text {
                            anchors.centerIn: parent
                            width: parent.width
                            text: "名称"
                            font.pixelSize: 12
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
                            width: parent.width
                            text: "网关 IP"
                            font.pixelSize: 12
                            font.bold: true
                            color: Theme.textSecondary
                            horizontalAlignment: Text.AlignHCenter
                        }
                    }
                    Item {
                        width: parent.width * 0.18
                        height: parent.height
                        Text {
                            anchors.centerIn: parent
                            width: parent.width
                            text: "状态"
                            font.pixelSize: 12
                            font.bold: true
                            color: Theme.textSecondary
                            horizontalAlignment: Text.AlignHCenter
                        }
                    }
                    Item {
                        width: parent.width * 0.26
                        height: parent.height
                        Text {
                            anchors.centerIn: parent
                            width: parent.width
                            text: "操作"
                            font.pixelSize: 12
                            font.bold: true
                            color: Theme.textSecondary
                            horizontalAlignment: Text.AlignHCenter
                        }
                    }
                }
            }

            Repeater {
                model: vpnFlow.gateways

                delegate: Rectangle {
                    width: parent.width
                    height: 40
                    color: modelData.id === vpnFlow.selectedGatewayId
                           ? "#EEF4FB"
                           : (index % 2 === 0 ? Theme.listStripeA : Theme.listStripeB)
                    border.color: modelData.id === vpnFlow.selectedGatewayId ? Theme.navySoft : Theme.cardBorder
                    border.width: modelData.id === vpnFlow.selectedGatewayId ? 1 : 0

                    Row {
                        anchors.fill: parent
                        anchors.leftMargin: 12
                        anchors.rightMargin: 8

                        Item {
                            width: parent.width * 0.22
                            height: parent.height
                            Text {
                                anchors.centerIn: parent
                                width: parent.width
                                text: modelData.name || "-"
                                font.pixelSize: 12
                                font.bold: true
                                color: Theme.textPrimary
                                horizontalAlignment: Text.AlignHCenter
                                elide: Text.ElideRight
                            }
                        }

                        Item {
                            width: parent.width * 0.34
                            height: parent.height
                            Text {
                                anchors.centerIn: parent
                                width: parent.width
                                text: vpnFlow.gatewayIp(modelData) || "-"
                                font.pixelSize: 12
                                color: Theme.textSecondary
                                horizontalAlignment: Text.AlignHCenter
                                elide: Text.ElideRight
                            }
                        }

                        Item {
                            width: parent.width * 0.18
                            height: parent.height
                            Text {
                                anchors.centerIn: parent
                                text: modelData.connected ? "已连接" : "未连接"
                                font.pixelSize: 12
                                font.bold: true
                                color: modelData.connected ? Theme.success : Theme.danger
                            }
                        }

                        Item {
                            width: parent.width * 0.26
                            height: parent.height

                            PrimaryButton {
                                anchors.centerIn: parent
                                implicitWidth: 56
                                implicitHeight: 26
                                visible: String(modelData.id) !== vpnFlow.selectedGatewayId
                                enabled: vpnFlow.switchingGatewayId === ""
                                text: vpnFlow.switchingGatewayId === String(modelData.id) ? "切换中" : "切换"
                                onClicked: vpnFlow.switchGateway(String(modelData.id))

                                contentItem: Text {
                                    text: parent.text
                                    font.pixelSize: 11
                                    font.bold: true
                                    color: Theme.textOnPrimary
                                    horizontalAlignment: Text.AlignHCenter
                                    verticalAlignment: Text.AlignVCenter
                                }
                            }

                            Text {
                                anchors.centerIn: parent
                                visible: String(modelData.id) === vpnFlow.selectedGatewayId
                                text: "当前"
                                font.pixelSize: 12
                                font.bold: true
                                color: Theme.navySoft
                            }
                        }
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
        }

        Text {
            text: "应用列表"
            font.bold: true
            font.pixelSize: 15
            color: Theme.navy
        }

        Rectangle {
            width: parent.width
            height: 34
            radius: Theme.buttonRadius
            color: Theme.listHover
            border.color: Theme.cardBorder
            border.width: 1

            Row {
                anchors.fill: parent
                anchors.leftMargin: 12
                anchors.rightMargin: 8

                Item {
                    width: parent.width * 0.28
                    height: parent.height
                    Text {
                        anchors.centerIn: parent
                        width: parent.width
                        text: "应用名称"
                        font.pixelSize: 12
                        font.bold: true
                        color: Theme.textSecondary
                        horizontalAlignment: Text.AlignHCenter
                    }
                }
                Item {
                    width: parent.width * 0.54
                    height: parent.height
                    Text {
                        anchors.centerIn: parent
                        width: parent.width
                        text: "URL"
                        font.pixelSize: 12
                        font.bold: true
                        color: Theme.textSecondary
                        horizontalAlignment: Text.AlignHCenter
                    }
                }
                Item {
                    width: parent.width * 0.18
                    height: parent.height
                    Text {
                        anchors.centerIn: parent
                        width: parent.width
                        text: "操作"
                        font.pixelSize: 12
                        font.bold: true
                        color: Theme.textSecondary
                        horizontalAlignment: Text.AlignHCenter
                    }
                }
            }
        }
    }

    Rectangle {
        anchors.top: headerColumn.bottom
        anchors.topMargin: 8
        anchors.left: parent.left
        anchors.leftMargin: 16
        anchors.right: parent.right
        anchors.rightMargin: 16
        anchors.bottom: parent.bottom
        anchors.bottomMargin: 16
        radius: Theme.buttonRadius
        color: Theme.cardBg
        border.color: Theme.cardBorder
        border.width: 1
        clip: true

        ListView {
            id: appListView
            anchors.fill: parent
            model: vpnFlow.apps
            spacing: 0
            clip: true

            delegate: Rectangle {
                width: appListView.width
                height: 44
                color: index % 2 === 0 ? Theme.listStripeA : Theme.listStripeB

                Row {
                    anchors.fill: parent
                    anchors.leftMargin: 12
                    anchors.rightMargin: 8

                    Item {
                        width: parent.width * 0.28
                        height: parent.height

                        Text {
                            anchors.verticalCenter: parent.verticalCenter
                            width: parent.width
                            text: modelData.name || modelData.serviceName || "-"
                            font.pixelSize: 13
                            font.bold: true
                            color: Theme.textPrimary
                            horizontalAlignment: Text.AlignHCenter
                            elide: Text.ElideRight
                        }
                    }

                    Item {
                        width: parent.width * 0.54
                        height: parent.height

                        Text {
                            id: urlText
                            anchors.verticalCenter: parent.verticalCenter
                            width: parent.width
                            text: modelData.url || "-"
                            font.pixelSize: 12
                            color: Theme.textSecondary
                            horizontalAlignment: Text.AlignHCenter
                            elide: Text.ElideRight
                        }

                        MouseArea {
                            id: urlHoverArea
                            anchors.fill: parent
                            hoverEnabled: true
                            acceptedButtons: Qt.NoButton
                        }

                        ToolTip {
                            visible: urlHoverArea.containsMouse
                                    && (modelData.url || "").length > 0
                                    && modelData.url !== "-"
                            text: modelData.url || ""
                            delay: 300
                        }
                    }

                    Item {
                        width: parent.width * 0.18
                        height: parent.height

                        GhostButton {
                            anchors.centerIn: parent
                            text: "复制"
                            visible: vpnFlow.isHttpUrl(modelData.url || "")
                            onClicked: vpnFlow.copyToClipboard(modelData.url || "")
                        }
                    }
                }

                Rectangle {
                    anchors.bottom: parent.bottom
                    width: parent.width
                    height: 1
                    color: Theme.cardBorder
                    opacity: 0.4
                }
            }
        }

        Text {
            anchors.centerIn: parent
            visible: vpnFlow.apps.length === 0
            text: "暂无可用应用"
            color: Theme.textSecondary
            font.pixelSize: 13
        }
    }
}
