import QtQuick
import QtQuick.Controls
import GenlotVPN 1.0

Item {
    id: root

    PageShell {
        anchors.fill: parent
        showProgress: vpnFlow.loading && !vpnFlow.verifyDialogVisible
        progress: 0.4
        statusText: vpnFlow.loading && !vpnFlow.verifyDialogVisible
                    ? "正在连接 " + (vpnFlow.pendingLine.appName || "")
                    : (vpnFlow.logs.length > 0 ? vpnFlow.logs[vpnFlow.logs.length - 1].message : "")

        Rectangle {
            width: parent.width
            height: 120
            radius: Theme.buttonRadius
            color: Theme.logBg
            border.color: Theme.logBorder
            border.width: 1
            visible: vpnFlow.logs.length > 0

            ListView {
                anchors.fill: parent
                anchors.margins: 8
                clip: true
                model: vpnFlow.logs
                spacing: 2
                delegate: Text {
                    width: parent.width
                    text: modelData.time + "  " + modelData.message
                    color: modelData.type === "error" ? Theme.danger : Theme.logText
                    font.pixelSize: 11
                    wrapMode: Text.Wrap
                }
            }
        }

        ActionRow {
            width: parent.width
            buttonText: "↻"
            buttonEnabled: !vpnFlow.loading && !vpnFlow.verifyDialogVisible
            onActionClicked: vpnFlow.startAutoConnect()

            Rectangle {
                anchors.fill: parent
                radius: Theme.buttonRadius
                color: Theme.inputBg
                border.color: Theme.inputBorder
                border.width: 1

                Text {
                    anchors.centerIn: parent
                    text: vpnFlow.pendingLine.appName || "连接中..."
                    color: Theme.textSecondary
                    font.pixelSize: 13
                    elide: Text.ElideRight
                    width: parent.width - 20
                    horizontalAlignment: Text.AlignHCenter
                }
            }
        }
    }

    Rectangle {
        id: verifyOverlay
        visible: vpnFlow.verifyDialogVisible
        anchors.fill: parent
        color: "#44000000"
        z: 100

        MouseArea {
            anchors.fill: parent
            onClicked: {}
        }

        Rectangle {
            width: Math.min(parent.width * Theme.contentWidthRatio, Theme.cardWidth)
            anchors.centerIn: parent
            radius: Theme.cardRadius
            color: Theme.cardBg
            border.color: Theme.cardBorder
            border.width: 1
            implicitHeight: verifyColumn.implicitHeight

            Column {
                id: verifyColumn
                width: parent.width - 32
                anchors.horizontalCenter: parent.horizontalCenter
                spacing: 14
                topPadding: 20
                bottomPadding: 20

                Text {
                    width: parent.width
                    text: "选线安全验证"
                    color: Theme.navy
                    font.pixelSize: 15
                    font.bold: true
                }

                Text {
                    width: parent.width
                    text: "请输入钉钉群收到的 6 位验证码"
                    color: Theme.textSecondary
                    font.pixelSize: 12
                    wrapMode: Text.Wrap
                }

                FlatField {
                    id: verifyCodeField
                    width: parent.width
                    placeholderText: "6位数字验证码"
                    maximumLength: 6
                    digitsOnly: true
                    horizontalAlignment: Text.AlignHCenter
                    font.pixelSize: 16
                    hasError: vpnFlow.verifyError.length > 0
                }

                Rectangle {
                    visible: vpnFlow.verifyError.length > 0
                    width: parent.width
                    radius: Theme.buttonRadius
                    color: "#FFF0F0"
                    border.color: Theme.danger
                    border.width: 1
                    implicitHeight: verifyErrorRow.implicitHeight + 20

                    Row {
                        id: verifyErrorRow
                        anchors.fill: parent
                        anchors.margins: 10
                        spacing: 10

                        Rectangle {
                            width: 20
                            height: 20
                            radius: 10
                            color: Theme.danger
                            anchors.verticalCenter: parent.verticalCenter

                            Text {
                                anchors.centerIn: parent
                                text: "!"
                                color: Theme.textOnPrimary
                                font.pixelSize: 13
                                font.bold: true
                            }
                        }

                        Text {
                            width: parent.width - 30
                            anchors.verticalCenter: parent.verticalCenter
                            text: vpnFlow.verifyError
                            color: Theme.danger
                            font.pixelSize: 14
                            font.bold: true
                            wrapMode: Text.Wrap
                        }
                    }
                }

                GhostButton {
                    text: vpnFlow.sendCountdown > 0
                          ? ("重新发送 (" + vpnFlow.sendCountdown + "s)")
                          : "发送验证码"
                    enabled: vpnFlow.sendCountdown <= 0
                    anchors.horizontalCenter: parent.horizontalCenter
                    onClicked: vpnFlow.sendVerifyCode()
                }

                Row {
                    width: parent.width
                    spacing: 10
                    layoutDirection: Qt.RightToLeft

                    PrimaryButton {
                        text: "确定"
                        implicitWidth: 80
                        enabled: !vpnFlow.loading
                        onClicked: vpnFlow.confirmVerifyCode(verifyCodeField.text)
                    }
                    GhostButton {
                        text: "取消"
                        implicitWidth: 64
                        onClicked: vpnFlow.verifyDialogVisible = false
                    }
                }
            }
        }
    }

    Connections {
        target: vpnFlow
        function onVerifyDialogVisibleChanged() {
            if (!vpnFlow.verifyDialogVisible) {
                verifyCodeField.text = ""
            }
        }
    }
}
