import QtQuick
import GenlotVPN 1.0

// 统一页面骨架：顶区 Logo + 中部操作区 + 底区状态/进度
Item {
    id: root
    default property alias content: bodyColumn.data
    property string statusText: ""
    property string headerSubtitle: ""
    property bool headerSubtitleBold: false
    property int headerSubtitleFontSize: 14
    property bool showProgress: false
    property real progress: 0.35
    property bool compact: false
    property bool spacious: false
    property bool scrollable: true
    property int bodySpacing: 14

    anchors.fill: parent

    PageBackground { anchors.fill: parent }

    Flickable {
        id: scrollArea
        anchors.fill: parent
        anchors.bottomMargin: 8
        contentWidth: width
        contentHeight: root.scrollable
                         ? Math.max(mainColumn.y + mainColumn.height + 16, height)
                         : height
        clip: true
        boundsBehavior: Flickable.StopAtBounds
        interactive: root.scrollable && contentHeight > height + 1
        flickableDirection: root.scrollable ? Flickable.VerticalFlick : Flickable.AutoFlickDirection

        readonly property real pageTopMargin: compact ? 28 : (spacious ? 36 : (vpnFlow.loggedIn ? 20 : Theme.pageTopMargin))
        readonly property real shellWidth: Math.min(
            width * (spacious ? 0.88 : Theme.contentWidthRatio),
            spacious ? 520 : Theme.cardWidth)

        Column {
            id: mainColumn
            x: (scrollArea.width - width) / 2
            y: spacious
                   ? Math.max(scrollArea.pageTopMargin, (scrollArea.height - height) / 2)
                   : scrollArea.pageTopMargin
            width: scrollArea.shellWidth
            spacing: spacious ? 28 : (compact ? 20 : Theme.sectionSpacing)

            BrandHeader {
                width: parent.width
                logoWidth: parent.width * (root.spacious ? 0.52 : Theme.logoWidthRatio)
                subtitle: root.headerSubtitle
                subtitleBold: root.headerSubtitleBold
                subtitleFontSize: root.headerSubtitleFontSize
            }

            Column {
                id: bodyColumn
                width: parent.width
                spacing: root.bodySpacing
            }

            Column {
                width: parent.width
                spacing: 8
                visible: showProgress || statusText.length > 0

                Rectangle {
                    width: parent.width
                    height: 3
                    radius: 1
                    color: Theme.inputBorder
                    visible: showProgress

                    Rectangle {
                        height: parent.height
                        width: Math.max(0, parent.width * Math.min(1, progress))
                        radius: 1
                        color: Theme.navySoft
                        Behavior on width { NumberAnimation { duration: 280; easing.type: Easing.OutCubic } }
                    }
                }

                Text {
                    width: parent.width
                    visible: statusText.length > 0
                    text: statusText
                    color: Theme.textSecondary
                    font.pixelSize: root.spacious ? 14 : 12
                    horizontalAlignment: root.spacious ? Text.AlignHCenter : Text.AlignLeft
                    elide: Text.ElideRight
                }
            }
        }
    }
}
