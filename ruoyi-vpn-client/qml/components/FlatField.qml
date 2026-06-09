import QtQuick
import QtQuick.Controls
import GenlotVPN 1.0

TextField {
    id: control
    property bool hasError: false
    property bool digitsOnly: false
    implicitHeight: Theme.actionRowHeight
    leftPadding: 14
    rightPadding: 14
    color: Theme.textPrimary
    placeholderTextColor: Theme.textSecondary
    font.pixelSize: 13
    selectByMouse: true
    inputMethodHints: digitsOnly ? Qt.ImhDigitsOnly : Qt.ImhNone

    onTextChanged: {
        if (!digitsOnly) {
            return
        }
        const digits = text.replace(/\D/g, "")
        const limited = digits.length > maximumLength ? digits.substring(0, maximumLength) : digits
        if (limited !== text) {
            text = limited
        }
    }

    background: Rectangle {
        radius: Theme.buttonRadius
        color: control.hasError ? "#FFF5F5" : Theme.inputBg
        border.color: control.hasError ? Theme.danger
                                        : (control.activeFocus ? Theme.inputFocus : Theme.inputBorder)
        border.width: control.hasError ? 2 : 1
    }
}
