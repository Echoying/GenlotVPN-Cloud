import QtQuick
import QtQuick.Controls
import GenlotVPN 1.0

Button {
    id: control
    property bool danger: false

    implicitHeight: Theme.actionRowHeight
    hoverEnabled: true

    background: Rectangle {
        radius: Theme.buttonRadius
        color: {
            if (!control.enabled) return "#B8C5D6"
            if (control.pressed) return control.danger ? "#B83838" : Theme.navyLight
            if (control.hovered) return control.danger ? "#C94A4A" : Theme.navySoft
            return control.danger ? Theme.danger : Theme.navy
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
