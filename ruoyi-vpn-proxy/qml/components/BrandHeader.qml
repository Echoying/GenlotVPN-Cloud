import QtQuick
import GenlotVPNProxy 1.0

Column {
    property string subtitle: "VPN 同步代理"
    spacing: 8
    width: parent ? parent.width : 520

    Image {
        source: Theme.assetLogoPng
        width: Math.min(300, parent.width * Theme.logoWidthRatio)
        height: Math.round(width * (88 / 360))
        fillMode: Image.PreserveAspectFit
        smooth: true
        antialiasing: true
        asynchronous: true
        sourceSize.width: Math.min(720, Math.round(width * 2))
        anchors.horizontalCenter: parent.horizontalCenter
    }
    Text {
        width: parent.width
        text: subtitle
        color: Theme.textSecondary
        font.pixelSize: 13
        horizontalAlignment: Text.AlignHCenter
    }
}
