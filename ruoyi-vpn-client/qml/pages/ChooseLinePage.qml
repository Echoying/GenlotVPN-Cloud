import QtQuick
import QtQuick.Controls
import GenlotVPN 1.0

Item {
    id: root

    readonly property bool useAuthorized: vpnFlow.loggedIn
    readonly property var lineList: useAuthorized ? vpnFlow.authorizedLines : vpnFlow.publicLines

    property var pendingSelectLine: null
    property bool purposeAttempted: false

    readonly property int purposeLen: purposeField.text.trim().length
    readonly property int purposeRawLen: purposeField.text.length

    readonly property string purposeHint: {
        if (purposeErrorText.length > 0)
            return purposeErrorText
        if (purposeAttempted && purposeLen === 0)
            return "请填写登录用途"
        if (purposeLen > 0 && purposeLen < 5)
            return "登录用途至少填写5个字（还需 " + (5 - purposeLen) + " 字）"
        if (purposeRawLen >= 50)
            return "已达 50 字上限"
        if (purposeLen >= 5)
            return "已输入 " + purposeLen + "/50 字"
        return "必填，5～50 字"
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

    property string purposeErrorText: ""

    function openSettings() {
        vpnFlow.goToSettings()
    }

    function openPurposeDialog(line) {
        pendingSelectLine = line
        purposeAttempted = false
        purposeErrorText = ""
        purposeField.text = ""
        purposeOverlay.visible = true
    }

    function closePurposeDialog() {
        purposeOverlay.visible = false
        pendingSelectLine = null
        purposeAttempted = false
        purposeErrorText = ""
        purposeField.text = ""
    }

    function confirmPurpose() {
        purposeAttempted = true
        const err = vpnFlow.validateLoginPurpose(purposeField.text)
        if (err.length > 0) {
            purposeErrorText = err
            return
        }
        if (!pendingSelectLine)
            return
        const line = pendingSelectLine
        const purpose = purposeField.text
        closePurposeDialog()
        if (useAuthorized)
            vpnFlow.selectAuthorizedLine(line, purpose)
        else
            vpnFlow.selectPublicLine(line, purpose)
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
                    root.openPurposeDialog(line)
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

    Rectangle {
        id: purposeOverlay
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
            implicitHeight: purposeColumn.implicitHeight

            Column {
                id: purposeColumn
                width: parent.width - 32
                anchors.horizontalCenter: parent.horizontalCenter
                spacing: 12
                topPadding: 20
                bottomPadding: 20

                Text {
                    width: parent.width
                    text: "登录用途"
                    color: Theme.navy
                    font.pixelSize: 15
                    font.bold: true
                }

                Text {
                    width: parent.width
                    text: pendingSelectLine && pendingSelectLine.appName
                          ? ("线路：" + pendingSelectLine.appName)
                          : ""
                    visible: text.length > 0
                    color: Theme.textSecondary
                    font.pixelSize: 12
                    wrapMode: Text.Wrap
                }

                FlatTextArea {
                    id: purposeField
                    width: parent.width
                    placeholderText: "必填，5～50 字"
                    maximumLength: 50
                    hasError: root.purposeIsError
                    onTextChanged: purposeErrorText = ""
                }

                Text {
                    width: parent.width
                    text: root.purposeHint
                    color: root.purposeIsError ? Theme.danger : Theme.textSecondary
                    font.pixelSize: 12
                    wrapMode: Text.Wrap
                }

                Row {
                    width: parent.width
                    spacing: 10
                    layoutDirection: Qt.RightToLeft

                    PrimaryButton {
                        text: "确定"
                        implicitWidth: 80
                        enabled: !vpnFlow.loading
                        onClicked: root.confirmPurpose()
                    }

                    GhostButton {
                        text: "取消"
                        implicitWidth: 64
                        enabled: !vpnFlow.loading
                        onClicked: root.closePurposeDialog()
                    }
                }
            }
        }
    }

    Component.onCompleted: {
        if (useAuthorized)
            vpnFlow.loadAuthorizedLines()
        else
            vpnFlow.loadPublicLines()
    }
}
