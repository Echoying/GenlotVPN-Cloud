import QtQuick
import QtQuick.Controls
import GenlotVPN 1.0

Item {
    id: root

    property bool purposeAttempted: false
    property string purposeErrorText: ""

    readonly property int purposeLen: purposeField.text.trim().length
    readonly property int purposeRawLen: purposeField.text.length

    readonly property string purposeHint: {
        if (purposeErrorText.length > 0)
            return purposeErrorText
        if (purposeAttempted && purposeLen === 0)
            return qsTr("请填写登录用途")
        if (purposeLen > 0 && purposeLen < 5)
            return qsTr("登录用途至少填写5个字（还需 %1 字）").arg(5 - purposeLen)
        if (purposeRawLen >= 50)
            return qsTr("已达 50 字上限")
        if (purposeLen >= 5)
            return qsTr("已输入 %1/50 字").arg(purposeLen)
        return ""
    }

    readonly property bool purposeIsError: {
        if (purposeErrorText.length > 0)
            return true
        if (purposeAttempted && purposeLen === 0)
            return true
        if (purposeLen > 0 && purposeLen < 5)
            return true
        return false
    }

    readonly property bool connecting: vpnFlow.loading && !vpnFlow.verifyDialogVisible

    PageShell {
        anchors.fill: parent
        spacious: true
        scrollable: !root.connecting
        headerSubtitle: root.connecting
                          ? qsTr("正在建立 VPN 连接")
                          : (vpnFlow.pendingLine.appName || qsTr("连接线路"))
        headerSubtitleBold: true
        headerSubtitleFontSize: 15
        showProgress: root.connecting
        progress: connectProgress.value
        statusText: root.connecting
                    ? (vpnFlow.statusMessage.length > 0
                       ? vpnFlow.statusMessage
                       : qsTr("正在连接 %1，请稍候...").arg(vpnFlow.pendingLine.appName || ""))
                    : vpnFlow.statusMessage

        ActionRow {
            width: parent.width
            visible: !root.connecting
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
                    text: vpnFlow.pendingLine.appName || qsTr("等待验证...")
                    color: Theme.textSecondary
                    font.pixelSize: 13
                    elide: Text.ElideRight
                    width: parent.width - 20
                    horizontalAlignment: Text.AlignHCenter
                }
            }
        }
    }

    QtObject {
        id: connectProgress
        property real value: 0.35

        SequentialAnimation on value {
            running: root.connecting
            loops: Animation.Infinite
            NumberAnimation { from: 0.2; to: 0.85; duration: 1800; easing.type: Easing.InOutQuad }
            NumberAnimation { from: 0.85; to: 0.2; duration: 1800; easing.type: Easing.InOutQuad }
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
            id: verifyCard
            width: Math.min(parent.width * Theme.contentWidthRatio, Theme.cardWidth)
            anchors.centerIn: parent
            radius: Theme.cardRadius
            color: Theme.cardBg
            border.color: Theme.cardBorder
            border.width: 1
            height: Math.min(verifyFlickable.contentHeight + 40, verifyOverlay.height - 24)
            clip: true

            Flickable {
                id: verifyFlickable
                anchors.fill: parent
                contentWidth: width
                contentHeight: verifyColumn.implicitHeight
                boundsBehavior: Flickable.StopAtBounds
                interactive: contentHeight > height
                flickableDirection: Flickable.VerticalFlick

                Column {
                    id: verifyColumn
                    width: verifyCard.width - 32
                    x: 16
                    spacing: 12
                    topPadding: 16
                    bottomPadding: 20

                    BrandHeader {
                        width: parent.width
                        logoWidth: parent.width * 0.48
                        subtitle: qsTr("选线验证")
                        subtitleBold: true
                        subtitleFontSize: 15
                    }

                    Text {
                        width: parent.width
                        text: vpnFlow.pendingLine.appName || ""
                        visible: text.length > 0
                        horizontalAlignment: Text.AlignHCenter
                        color: Theme.navy
                        font.pixelSize: 14
                        font.bold: true
                        wrapMode: Text.Wrap
                    }

                    Text {
                        width: parent.width
                        text: qsTr("登录用途")
                        color: Theme.navy
                        font.pixelSize: 14
                        font.bold: true
                    }

                    FlatTextArea {
                        id: purposeField
                        width: parent.width
                        placeholderText: qsTr("请输入登录用途（5-50字）")
                        maximumLength: 50
                        hasError: root.purposeIsError
                        onTextChanged: purposeErrorText = ""
                    }

                    Text {
                        width: parent.width
                        visible: root.purposeHint.length > 0
                        text: root.purposeHint
                        color: root.purposeIsError ? Theme.danger : Theme.textSecondary
                        font.pixelSize: 12
                        wrapMode: Text.Wrap
                    }

                    Text {
                        width: parent.width
                        text: qsTr("钉钉验证码（6位数字）")
                        color: Theme.navy
                        font.pixelSize: 14
                        font.bold: true
                    }

                    FlatField {
                        id: verifyCodeField
                        width: parent.width
                        placeholderText: qsTr("6位数字验证码")
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
                              ? qsTr("重新发送 (%1s)").arg(vpnFlow.sendCountdown)
                              : qsTr("发送验证码")
                        enabled: vpnFlow.sendCountdown <= 0 && !vpnFlow.loading
                        anchors.horizontalCenter: parent.horizontalCenter
                        onClicked: {
                            purposeAttempted = true
                            const err = vpnFlow.validateLoginPurpose(purposeField.text)
                            if (err.length > 0) {
                                purposeErrorText = err
                                return
                            }
                            purposeErrorText = ""
                            vpnFlow.sendVerifyCode(purposeField.text)
                        }
                    }

                    Row {
                        width: parent.width
                        spacing: 10
                        layoutDirection: Qt.RightToLeft

                        PrimaryButton {
                            text: qsTr("确定")
                            implicitWidth: 80
                            enabled: !vpnFlow.loading
                            onClicked: vpnFlow.confirmVerifyCode(verifyCodeField.text)
                        }
                        GhostButton {
                            text: qsTr("取消")
                            implicitWidth: 64
                            onClicked: {
                                vpnFlow.verifyDialogVisible = false
                                vpnFlow.goChooseLine()
                            }
                        }
                    }
                }
            }
        }
    }

    Connections {
        target: vpnFlow
        function onVerifyDialogVisibleChanged() {
            if (vpnFlow.verifyDialogVisible) {
                purposeAttempted = false
                purposeErrorText = ""
            } else {
                verifyCodeField.text = ""
                purposeField.text = ""
                purposeAttempted = false
                purposeErrorText = ""
            }
        }
    }
}
