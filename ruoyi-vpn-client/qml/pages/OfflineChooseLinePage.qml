import QtQuick
import QtQuick.Controls
import GenlotVPN 1.0

Item {
    id: root

    function openCredentialDialog() {
        if (lineCombo.currentIndex < 0
                || lineCombo.currentIndex >= vpnFlow.offlineLoginLines.length) {
            return
        }
        localUsernameField.text = ""
        localPasswordField.text = ""
        credentialOverlay.visible = true
    }

    function closeCredentialDialog() {
        credentialOverlay.visible = false
        localUsernameField.text = ""
        localPasswordField.text = ""
    }

    function confirmCredential() {
        vpnFlow.connectOfflineLine(lineCombo.currentIndex,
                                   localUsernameField.text,
                                   localPasswordField.text)
    }

    readonly property var selectedLine: {
        if (lineCombo.currentIndex >= 0
                && lineCombo.currentIndex < vpnFlow.offlineLoginLines.length) {
            return vpnFlow.offlineLoginLines[lineCombo.currentIndex]
        }
        return null
    }

    PageShell {
        anchors.fill: parent
        spacious: true
        bodySpacing: 24
        headerSubtitle: qsTr("请选择离线登录线路")
        headerSubtitleBold: true
        headerSubtitleFontSize: 15
        showProgress: vpnFlow.loading
        progress: vpnFlow.loading ? 0.35 : 0
        statusText: vpnFlow.loading
                    ? qsTr("正在加载离线线路...")
                    : vpnFlow.statusMessage

        ActionRow {
            width: parent.width
            rowHeight: 48
            btnSize: 48
            actionFontSize: 24
            primaryButton: true
            buttonText: "›"
            buttonEnabled: !vpnFlow.loading
                           && lineCombo.currentIndex >= 0
                           && vpnFlow.offlineLoginLines.length > 0
            onActionClicked: root.openCredentialDialog()

            LineCombo {
                id: lineCombo
                anchors.fill: parent
                controlHeight: 48
                fontSize: 15
                refreshOnOpen: true
                loading: vpnFlow.loading
                lines: vpnFlow.offlineLoginLines
                onRefreshRequested: vpnFlow.loadOfflineLoginFiles()
            }
        }

        Text {
            width: parent.width
            visible: !vpnFlow.loading && vpnFlow.offlineLoginLines.length === 0
            text: qsTr("请将管理端导出的 .dat 文件放入：%1").arg(vpnFlow.offlineLoginDir)
            color: Theme.textSecondary
            font.pixelSize: 13
            wrapMode: Text.Wrap
            horizontalAlignment: Text.AlignHCenter
        }
    }

    GhostButton {
        z: 20
        anchors.top: parent.top
        anchors.left: parent.left
        anchors.topMargin: 10
        anchors.leftMargin: 12
        text: qsTr("‹ 返回")
        emphasized: true
        onClicked: vpnFlow.goBackToLogin()
    }

    Rectangle {
        id: credentialOverlay
        visible: false
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
            implicitHeight: credentialColumn.implicitHeight

            Column {
                id: credentialColumn
                width: parent.width - 32
                anchors.horizontalCenter: parent.horizontalCenter
                spacing: 12
                topPadding: 20
                bottomPadding: 20

                Text {
                    width: parent.width
                    text: root.selectedLine && root.selectedLine.appName
                          ? root.selectedLine.appName
                          : ""
                    visible: text.length > 0
                    horizontalAlignment: Text.AlignHCenter
                    color: Theme.navy
                    font.pixelSize: 15
                    font.bold: true
                    wrapMode: Text.Wrap
                }

                Text {
                    width: parent.width
                    text: qsTr("本地账号验证")
                    color: Theme.navy
                    font.pixelSize: 15
                    font.bold: true
                }

                FlatField {
                    id: localUsernameField
                    width: parent.width
                    placeholderText: qsTr("请输入本地账号")
                }

                FlatField {
                    id: localPasswordField
                    width: parent.width
                    placeholderText: qsTr("请输入本地密码")
                    echoMode: TextInput.Password
                }

                Row {
                    width: parent.width
                    spacing: 10
                    layoutDirection: Qt.RightToLeft

                    PrimaryButton {
                        text: qsTr("确定")
                        implicitWidth: 80
                        enabled: !vpnFlow.loading
                        onClicked: root.confirmCredential()
                    }

                    GhostButton {
                        text: qsTr("取消")
                        implicitWidth: 64
                        enabled: !vpnFlow.loading
                        onClicked: root.closeCredentialDialog()
                    }
                }
            }
        }
    }

    Component.onCompleted: vpnFlow.loadOfflineLoginFiles()
}
