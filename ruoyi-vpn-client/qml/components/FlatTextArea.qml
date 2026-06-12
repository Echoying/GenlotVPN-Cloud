import QtQuick
import QtQuick.Controls
import GenlotVPN 1.0

TextArea {
    id: control
    property bool hasError: false
    property int maximumLength: 32767
    implicitHeight: Theme.actionRowHeight * 2 - 8
    leftPadding: 14
    rightPadding: 14
    topPadding: 10
    bottomPadding: 10
    color: Theme.textPrimary
    placeholderTextColor: Theme.textSecondary
    font.pixelSize: 13
    selectByMouse: true
    wrapMode: TextArea.Wrap

    onTextChanged: {
        if (maximumLength > 0 && text.length > maximumLength) {
            text = text.substring(0, maximumLength)
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
