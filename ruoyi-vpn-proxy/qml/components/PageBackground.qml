import QtQuick
import GenlotVPNProxy 1.0

Item {
    anchors.fill: parent
    Rectangle { anchors.fill: parent; color: Theme.windowBg }
    Canvas {
        anchors.top: parent.top
        anchors.left: parent.left
        anchors.right: parent.right
        height: parent.height * 0.55
        opacity: 0.35
        onPaint: {
            const ctx = getContext("2d")
            ctx.clearRect(0, 0, width, height)
            ctx.strokeStyle = "#C5CED9"
            ctx.lineWidth = 1
            const step = 28
            for (let x = 0; x <= width; x += step) {
                for (let y = 0; y <= height; y += step) {
                    ctx.beginPath()
                    ctx.arc(x, y, 1.2, 0, Math.PI * 2)
                    ctx.fillStyle = "#B8C4D4"
                    ctx.fill()
                }
            }
        }
        onWidthChanged: requestPaint()
        onHeightChanged: requestPaint()
        Component.onCompleted: requestPaint()
    }
}
