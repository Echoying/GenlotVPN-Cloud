import QtQuick
import QtQuick.Controls
import GenlotVPN 1.0

Button {
    id: control
    property bool danger: false
    /** 设置页等场景使用较浅的按钮色 */
    property bool soft: false

    implicitHeight: Theme.actionRowHeight
    hoverEnabled: true

    background: Rectangle {
        radius: Theme.buttonRadius
        color: {
            if (!control.enabled) return "#B8C5D6"
            if (control.danger) {
                if (control.pressed) return "#B83838"
                if (control.hovered) return "#C94A4A"
                return Theme.danger
            }
            if (control.soft) {
                if (control.pressed) return "#3A6A9E"
                if (control.hovered) return "#4A7AB5"
                return Theme.navySoft
            }
            if (control.pressed) return Theme.navyLight
            if (control.hovered) return Theme.navySoft
            return Theme.navy
        }
        border.width: 0
    }

    contentItem: Text {
        text: control.text
        font.pixelSize: 14
        font.bold: true
        color: Theme.textOnPrimary
        horizontalAlignment: Text.AlignHCenter
        verticalAlignment: Text.AlignVCenter
        elide: Text.ElideRight
    }
}
