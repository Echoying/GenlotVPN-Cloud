import QtQuick
import QtQuick.Controls
import GenlotVPNProxy 1.0

Button {
    id: control
    implicitHeight: 36
    flat: true
    hoverEnabled: true
    background: Rectangle {
        radius: Theme.buttonRadius
        color: control.hovered ? "#140B2D5B" : "transparent"
    }
    contentItem: Text {
        text: control.text
        font.pixelSize: 13
        color: Theme.navySoft
        horizontalAlignment: Text.AlignHCenter
        verticalAlignment: Text.AlignVCenter
    }
}
