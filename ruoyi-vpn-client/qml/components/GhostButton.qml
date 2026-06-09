import QtQuick
import QtQuick.Controls
import GenlotVPN 1.0

Button {
    id: control
    property bool danger: false
    property bool emphasized: false

    implicitHeight: emphasized ? 38 : 36
    flat: true
    hoverEnabled: true

    background: Rectangle {
        radius: Theme.buttonRadius
        color: control.hovered ? (control.danger ? "#1AD64545" : "#140B2D5B") : "transparent"
    }

    contentItem: Text {
        text: control.text
        font.pixelSize: control.emphasized ? 14 : 13
        font.bold: control.danger || control.emphasized
        color: control.danger ? Theme.danger : Theme.navySoft
        horizontalAlignment: Text.AlignHCenter
        verticalAlignment: Text.AlignVCenter
    }
}
