import QtQuick
import GenlotVPN 1.0

Column {
    id: root
    property string subtitle: ""
    property bool subtitleBold: false
    property int subtitleFontSize: 13
    property real logoWidth: Math.min(280, root.width * Theme.logoWidthRatio)
    spacing: 8
    width: parent ? parent.width : Theme.cardWidth

    Image {
        id: logoImage
        source: Theme.assetLogoPng
        width: Math.min(logoWidth, root.width)
        height: Math.round(width * (88 / 360))
        fillMode: Image.PreserveAspectFit
        anchors.horizontalCenter: parent.horizontalCenter
        sourceSize.width: Math.min(720, Math.round(width * 2))
        asynchronous: true
    }

    Text {
        width: root.width
        visible: subtitle.length > 0
        text: subtitle
        color: subtitleBold ? Theme.textPrimary : Theme.textSecondary
        font.pixelSize: subtitleBold ? subtitleFontSize : 13
        font.bold: subtitleBold
        horizontalAlignment: Text.AlignHCenter
        wrapMode: Text.Wrap
        elide: Text.ElideRight
        maximumLineCount: 2
    }
}
