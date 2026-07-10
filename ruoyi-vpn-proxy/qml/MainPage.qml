import QtQuick
import QtQuick.Controls
import GenlotVPNProxy 1.0

Item {
    id: root
    property int activeTab: 0
    property int selectedProxyIndex: -1

    PageBackground { anchors.fill: parent }

    Connections {
        target: proxySession.proxyLogs
        function onEntryAdded() {
            root.selectedProxyIndex = 0
        }
    }

    Column {
        anchors.fill: parent
        anchors.margins: 16
        spacing: 12

        BrandHeader { width: parent.width }

        Item {
            width: parent.width
            height: 36

            Row {
                anchors.right: parent.right
                anchors.verticalCenter: parent.verticalCenter
                spacing: 8
                GhostButton {
                    text: qsTr("清空会话日志")
                    onClicked: proxySession.clearSessionLogs()
                }
                GhostButton {
                    text: qsTr("清空代理日志")
                    onClicked: {
                        proxySession.clearProxyLogs()
                        root.selectedProxyIndex = -1
                    }
                }
            }
        }

        Rectangle {
            width: parent.width
            height: statusCard.implicitHeight + 20
            color: Theme.cardBg
            border.color: Theme.cardBorder
            radius: 4
            Column {
                id: statusCard
                anchors.fill: parent
                anchors.margins: 10
                spacing: 6
                Text {
                    text: qsTr("状态: %1  |  管理: %2  |  代理: %3")
                        .arg(stateLabel(proxySession.sessionState))
                        .arg(proxySession.adminEndpoint)
                        .arg(proxySession.proxyEndpoint || "-")
                    font.pixelSize: 12
                    color: Theme.textPrimary
                    wrapMode: Text.Wrap
                    width: parent.width
                }
                Text {
                    text: qsTr("上游: %1  |  隧道: %2")
                        .arg(proxySession.upstreamUrl || "-")
                        .arg(proxySession.tunnelStatus >= 0 ? proxySession.tunnelStatus : "-")
                    font.pixelSize: 11
                    color: Theme.textSecondary
                    wrapMode: Text.Wrap
                    width: parent.width
                }
            }
        }

        Row {
            spacing: 24
            TabButton {
                id: sessionTabBtn
                text: qsTr("会话日志")
                checked: root.activeTab === 0
                onClicked: root.activeTab = 0
                contentItem: Text {
                    text: sessionTabBtn.text
                    font.pixelSize: 14
                    font.bold: sessionTabBtn.checked
                    color: sessionTabBtn.checked ? Theme.navy : Theme.navySoft
                }
                background: Rectangle {
                    implicitHeight: 32
                    color: "transparent"
                    Rectangle {
                        anchors.bottom: parent.bottom
                        width: parent.width
                        height: 2
                        color: Theme.accent
                        visible: sessionTabBtn.checked
                    }
                }
            }
            TabButton {
                id: proxyTabBtn
                text: qsTr("代理日志")
                checked: root.activeTab === 1
                onClicked: root.activeTab = 1
                contentItem: Text {
                    text: proxyTabBtn.text
                    font.pixelSize: 14
                    font.bold: proxyTabBtn.checked
                    color: proxyTabBtn.checked ? Theme.navy : Theme.navySoft
                }
                background: Rectangle {
                    implicitHeight: 32
                    color: "transparent"
                    Rectangle {
                        anchors.bottom: parent.bottom
                        width: parent.width
                        height: 2
                        color: Theme.accent
                        visible: proxyTabBtn.checked
                    }
                }
            }
        }

        Item {
            width: parent.width
            height: parent.height - y - 8

            // 会话日志 Tab
            Column {
                anchors.fill: parent
                visible: root.activeTab === 0
                spacing: 0
                Rectangle {
                    width: parent.width
                    height: 30
                    color: Theme.listHover
                    Row {
                        anchors.fill: parent
                        anchors.leftMargin: 8
                        Text { width: parent.width * 0.08; text: qsTr("时间"); font.pixelSize: 11; font.bold: true; color: Theme.textSecondary; horizontalAlignment: Text.AlignHCenter; anchors.verticalCenter: parent.verticalCenter }
                        Text { width: parent.width * 0.06; text: qsTr("类型"); font.pixelSize: 11; font.bold: true; color: Theme.textSecondary; horizontalAlignment: Text.AlignHCenter; anchors.verticalCenter: parent.verticalCenter }
                        Text { width: parent.width * 0.16; text: qsTr("消息"); font.pixelSize: 11; font.bold: true; color: Theme.textSecondary; horizontalAlignment: Text.AlignHCenter; anchors.verticalCenter: parent.verticalCenter }
                        Text { width: parent.width * 0.30; text: qsTr("请求报文"); font.pixelSize: 11; font.bold: true; color: Theme.textSecondary; horizontalAlignment: Text.AlignHCenter; anchors.verticalCenter: parent.verticalCenter }
                        Text { width: parent.width * 0.30; text: qsTr("响应报文"); font.pixelSize: 11; font.bold: true; color: Theme.textSecondary; horizontalAlignment: Text.AlignHCenter; anchors.verticalCenter: parent.verticalCenter }
                        Text { width: parent.width * 0.10; text: qsTr("耗时"); font.pixelSize: 11; font.bold: true; color: Theme.textSecondary; horizontalAlignment: Text.AlignHCenter; anchors.verticalCenter: parent.verticalCenter }
                    }
                }
                Item {
                    width: parent.width
                    height: parent.height - 30
                    ListView {
                        id: sessionList
                        anchors.fill: parent
                        clip: true
                        model: proxySession.sessionLogs
                        onCountChanged: if (count > 0) positionViewAtBeginning()
                        delegate: Rectangle {
                            width: sessionList.width
                            height: Math.max(32, Math.max(reqText.implicitHeight, respText.implicitHeight) + 12)
                            color: index % 2 === 0 ? Theme.listStripeA : Theme.listStripeB
                            Row {
                                anchors.fill: parent
                                anchors.leftMargin: 8
                                anchors.topMargin: 6
                                anchors.bottomMargin: 6
                                Text { width: parent.width * 0.08; text: model.time; font.pixelSize: 10; color: Theme.textSecondary; horizontalAlignment: Text.AlignHCenter; anchors.verticalCenter: parent.verticalCenter }
                                Text { width: parent.width * 0.06; text: model.type; font.pixelSize: 11; color: typeColor(model.type); font.bold: true; horizontalAlignment: Text.AlignHCenter; anchors.verticalCenter: parent.verticalCenter }
                                Text {
                                    width: parent.width * 0.16
                                    text: model.message
                                    font.pixelSize: 11
                                    color: Theme.textPrimary
                                    wrapMode: Text.Wrap
                                    maximumLineCount: 6
                                    elide: Text.ElideRight
                                    anchors.verticalCenter: parent.verticalCenter
                                }
                                Text {
                                    id: reqText
                                    width: parent.width * 0.30
                                    text: model.requestLog || "-"
                                    font.pixelSize: 10
                                    color: Theme.logText
                                    wrapMode: Text.Wrap
                                    maximumLineCount: 8
                                    elide: Text.ElideRight
                                }
                                Text {
                                    id: respText
                                    width: parent.width * 0.30
                                    text: model.responseLog || "-"
                                    font.pixelSize: 10
                                    color: Theme.logText
                                    wrapMode: Text.Wrap
                                    maximumLineCount: 8
                                    elide: Text.ElideRight
                                }
                                Text {
                                    width: parent.width * 0.10
                                    text: model.elapsedMs >= 0 ? (model.elapsedMs + "ms") : "-"
                                    font.pixelSize: 10
                                    color: Theme.textSecondary
                                    horizontalAlignment: Text.AlignHCenter
                                    anchors.verticalCenter: parent.verticalCenter
                                }
                            }
                        }
                    }
                    Text {
                        anchors.centerIn: parent
                        visible: !proxySession.sessionLogs || proxySession.sessionLogs.count === 0
                        text: qsTr("暂无会话记录")
                        color: Theme.textSecondary
                        font.pixelSize: 13
                    }
                }
            }

            // 代理日志 Tab
            Column {
                anchors.fill: parent
                visible: root.activeTab === 1
                spacing: 8

                Rectangle {
                    width: parent.width
                    height: 30
                    color: Theme.listHover
                    border.color: Theme.cardBorder
                    Row {
                        anchors.fill: parent
                        anchors.leftMargin: 8
                        Text { width: parent.width * 0.20; text: qsTr("时间"); font.pixelSize: 11; font.bold: true; color: Theme.textSecondary; horizontalAlignment: Text.AlignHCenter; anchors.verticalCenter: parent.verticalCenter }
                        Text { width: parent.width * 0.10; text: qsTr("方法"); font.pixelSize: 11; font.bold: true; color: Theme.textSecondary; horizontalAlignment: Text.AlignHCenter; anchors.verticalCenter: parent.verticalCenter }
                        Text { width: parent.width * 0.34; text: qsTr("URL"); font.pixelSize: 11; font.bold: true; color: Theme.textSecondary; horizontalAlignment: Text.AlignHCenter; anchors.verticalCenter: parent.verticalCenter }
                        Text { width: parent.width * 0.10; text: qsTr("状态"); font.pixelSize: 11; font.bold: true; color: Theme.textSecondary; horizontalAlignment: Text.AlignHCenter; anchors.verticalCenter: parent.verticalCenter }
                        Text { width: parent.width * 0.16; text: qsTr("来源 IP"); font.pixelSize: 11; font.bold: true; color: Theme.textSecondary; horizontalAlignment: Text.AlignHCenter; anchors.verticalCenter: parent.verticalCenter }
                    }
                }

                Rectangle {
                    width: parent.width
                    height: parent.height * 0.42
                    color: Theme.cardBg
                    border.color: Theme.cardBorder
                    clip: true
                    ListView {
                        id: proxyList
                        anchors.fill: parent
                        anchors.margins: 1
                        clip: true
                        model: proxySession.proxyLogs
                        delegate: Rectangle {
                            width: proxyList.width
                            height: 36
                            color: root.selectedProxyIndex === index ? "#EEF4FB" : (index % 2 === 0 ? Theme.listStripeA : Theme.listStripeB)
                            Row {
                                anchors.fill: parent
                                anchors.leftMargin: 8
                                Text { width: parent.width * 0.20; text: model.time; font.pixelSize: 10; color: Theme.textSecondary; horizontalAlignment: Text.AlignHCenter; anchors.verticalCenter: parent.verticalCenter }
                                Text { width: parent.width * 0.10; text: model.method; font.pixelSize: 11; font.bold: true; color: Theme.textPrimary; anchors.verticalCenter: parent.verticalCenter }
                                Text { width: parent.width * 0.34; text: model.path; font.pixelSize: 10; color: Theme.textPrimary; elide: Text.ElideRight; horizontalAlignment: Text.AlignHCenter; anchors.verticalCenter: parent.verticalCenter }
                                Text { width: parent.width * 0.10; text: model.status > 0 ? String(model.status) : "-"; font.pixelSize: 11; font.bold: true; color: (model.success === true) ? Theme.success : Theme.danger; anchors.verticalCenter: parent.verticalCenter }
                                Text { width: parent.width * 0.16; text: model.peerIp; font.pixelSize: 10; color: Theme.textSecondary; horizontalAlignment: Text.AlignHCenter; anchors.verticalCenter: parent.verticalCenter }
                            }
                            MouseArea { anchors.fill: parent; onClicked: root.selectedProxyIndex = index }
                        }
                    }
                    Text {
                        anchors.centerIn: parent
                        visible: !proxySession.proxyLogs || proxySession.proxyLogs.count === 0
                        text: qsTr("暂无代理请求记录")
                        color: Theme.textSecondary
                        font.pixelSize: 13
                    }
                }

                Row {
                    width: parent.width
                    height: parent.height * 0.58 - 46
                    spacing: 8
                    Column {
                        width: (parent.width - parent.spacing) / 2
                        height: parent.height
                        spacing: 6
                        Text { text: qsTr("请求日志"); font.pixelSize: 12; font.bold: true; color: Theme.navy }
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
                                text: root.selectedProxyIndex >= 0 && proxySession.proxyLogs
                                      ? proxySession.proxyLogs.entryAt(root.selectedProxyIndex).requestLog
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
                        Text { text: qsTr("响应日志"); font.pixelSize: 12; font.bold: true; color: Theme.navy }
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
                                text: root.selectedProxyIndex >= 0 && proxySession.proxyLogs
                                      ? proxySession.proxyLogs.entryAt(root.selectedProxyIndex).responseLog
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

    function stateLabel(state) {
        if (state === "ready") return qsTr("已就绪")
        if (state === "connecting") return qsTr("连接中")
        if (state === "error") return qsTr("错误")
        return qsTr("待命")
    }

    function typeColor(type) {
        if (type === "error") return Theme.danger
        if (type === "warn") return "#D4A84B"
        return Theme.navySoft
    }
}
