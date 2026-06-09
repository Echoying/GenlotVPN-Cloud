import QtQuick
import GenlotVPN 1.0

Item {
    anchors.fill: parent

    Rectangle {
        anchors.fill: parent
        color: Theme.windowBg
    }

    // 上半区淡网格纹理（对齐 Easy Connect 顶部装饰）
    Canvas {
        id: meshCanvas
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
                    if (x + step <= width) {
                        ctx.beginPath()
                        ctx.moveTo(x, y)
                        ctx.lineTo(x + step, y)
                        ctx.stroke()
                    }
                    if (y + step <= height) {
                        ctx.beginPath()
                        ctx.moveTo(x, y)
                        ctx.lineTo(x, y + step)
                        ctx.stroke()
                    }
                }
            }
        }

        onWidthChanged: requestPaint()
        onHeightChanged: requestPaint()
        Component.onCompleted: requestPaint()
    }

    Rectangle {
        anchors.fill: parent
        gradient: Gradient {
            GradientStop { position: 0.0; color: "#00000000" }
            GradientStop { position: 0.45; color: "#00000000" }
            GradientStop { position: 1.0; color: Theme.windowBg }
        }
        opacity: 0.5
    }
}
