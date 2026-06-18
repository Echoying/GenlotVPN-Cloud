import QtQuick
import QtQuick.Controls
import GenlotVPN 1.0

Item {
    id: root

    readonly property bool useAuthorized: vpnFlow.loggedIn
    readonly property var lineList: useAuthorized ? vpnFlow.authorizedLines : vpnFlow.publicLines

    function openSettings() {
        vpnFlow.goToSettings()
    }

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
                    ? (useAuthorized ? "正在加载授权线路..." : "正在加载线路...")
                    : vpnFlow.statusMessage

        ActionRow {
            width: parent.width
            rowHeight: 48
            btnSize: 48
            actionFontSize: 24
            primaryButton: true
            buttonText: "›"
            buttonEnabled: !vpnFlow.loading && lineCombo.currentIndex >= 0 && lineList.length > 0
            onActionClicked: {
                if (lineCombo.currentIndex >= 0 && lineCombo.currentIndex < lineList.length) {
                    const line = lineList[lineCombo.currentIndex]
                    if (useAuthorized)
                        vpnFlow.selectAuthorizedLine(line)
                    else
                        vpnFlow.selectPublicLine(line)
                }
            }

            LineCombo {
                id: lineCombo
                anchors.fill: parent
                controlHeight: 48
                fontSize: 15
                refreshOnOpen: true
                loading: vpnFlow.loading
                lines: lineList
                onRefreshRequested: useAuthorized ? vpnFlow.loadAuthorizedLines() : vpnFlow.loadPublicLines()
            }
        }
    }

    GhostButton {
        id: settingsBtn
        z: 20
        anchors.top: parent.top
        anchors.right: parent.right
        anchors.topMargin: 10
        anchors.rightMargin: 12
        text: "设置"
        emphasized: true
        onClicked: root.openSettings()
    }

    Component.onCompleted: {
        if (useAuthorized)
            vpnFlow.loadAuthorizedLines()
        else
            vpnFlow.loadPublicLines()
    }
}
