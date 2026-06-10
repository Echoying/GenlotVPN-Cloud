import QtQuick
import QtQuick.Controls
import GenlotVPN 1.0

Switch {
    id: control

    indicator: Rectangle {
        implicitWidth: 46
        implicitHeight: 26
        radius: 13
        color: control.checked ? Theme.navySoft : "#FFFFFF"
        border.color: control.checked ? Theme.navySoft : Theme.inputBorder
        border.width: 1

        Rectangle {
            width: 20
            height: 20
            radius: 10
            anchors.verticalCenter: parent.verticalCenter
            x: control.checked ? parent.width - width - 3 : 3
            color: "#FFFFFF"
            border.color: control.checked ? Theme.navySoft : Theme.cardBorder
            border.width: 1

            Behavior on x {
                NumberAnimation {
                    duration: 160
                    easing.type: Easing.OutCubic
                }
            }
        }
    }
}
