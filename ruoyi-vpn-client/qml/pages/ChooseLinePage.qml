import QtQuick
import QtQuick.Controls
import GenlotVPN 1.0

Item {
    id: root

    PageShell {
        anchors.fill: parent
        spacious: true
        bodySpacing: 24
        headerSubtitle: "请选择要连接的 VPN 线路"
        headerSubtitleBold: true
        headerSubtitleFontSize: 15
        showProgress: vpnFlow.loading
        progress: vpnFlow.loading ? 0.35 : 0
        statusText: vpnFlow.loading
                    ? "正在加载线路..."
                    : vpnFlow.statusMessage

        ActionRow {
            width: parent.width
            rowHeight: 48
            btnSize: 48
            actionFontSize: 24
            primaryButton: true
            buttonText: "›"
            buttonEnabled: !vpnFlow.loading && lineCombo.currentIndex >= 0 && vpnFlow.publicLines.length > 0
            onActionClicked: {
                if (lineCombo.currentIndex >= 0 && lineCombo.currentIndex < vpnFlow.publicLines.length)
                    vpnFlow.selectPublicLine(vpnFlow.publicLines[lineCombo.currentIndex])
            }

            LineCombo {
                id: lineCombo
                anchors.fill: parent
                controlHeight: 48
                fontSize: 15
                refreshOnOpen: true
                loading: vpnFlow.loading
                lines: vpnFlow.publicLines
                onRefreshRequested: vpnFlow.loadPublicLines()
            }
        }
    }

    Component.onCompleted: vpnFlow.loadPublicLines()
}
