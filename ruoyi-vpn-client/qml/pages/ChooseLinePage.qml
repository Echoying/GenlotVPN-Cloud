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

    property string purposeErrorText: ""

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
        headerSubtitle: qsTr("请选择要连接的 VPN 线路")
        headerSubtitleBold: true
        headerSubtitleFontSize: 15
        showProgress: vpnFlow.loading
        progress: vpnFlow.loading ? 0.35 : 0
        statusText: vpnFlow.loading
                    ? (useAuthorized ? qsTr("正在加载授权线路...") : qsTr("正在加载线路..."))
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
                    text: pendingSelectLine && pendingSelectLine.appName
                          ? pendingSelectLine.appName
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
                    text: qsTr("登录用途")
                    color: Theme.navy
                    font.pixelSize: 15
                    font.bold: true
                }

                FlatTextArea {
                    id: purposeField
                    width: parent.width
                    placeholderText: qsTr("请输入登录用途")
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

                Row {
                    width: parent.width
                    spacing: 10
                    layoutDirection: Qt.RightToLeft

                    PrimaryButton {
                        text: qsTr("确定")
                        implicitWidth: 80
                        enabled: !vpnFlow.loading
                        onClicked: root.confirmPurpose()
                    }

                    GhostButton {
                        text: qsTr("取消")
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
