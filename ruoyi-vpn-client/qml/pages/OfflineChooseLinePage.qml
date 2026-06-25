import QtQuick
import QtQuick.Controls
import GenlotVPN 1.0

Item {
    id: root

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
            onActionClicked: vpnFlow.connectOfflineLine(lineCombo.currentIndex)

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

    Component.onCompleted: vpnFlow.loadOfflineLoginFiles()
}
