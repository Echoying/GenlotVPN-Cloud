import QtQuick
import QtQuick.Controls
import GenlotVPN 1.0

ComboBox {
    id: control
    implicitHeight: Theme.actionRowHeight
    font.pixelSize: 13

    function itemText(data) {
        if (textRole.length > 0 && data && data[textRole] !== undefined)
            return String(data[textRole])
        if (typeof data === "string")
            return data
        return ""
    }

    background: Rectangle {
        radius: Theme.buttonRadius
        color: Theme.inputBg
        border.color: control.activeFocus ? Theme.inputFocus : Theme.inputBorder
        border.width: 1
    }

    contentItem: Text {
        leftPadding: 14
        rightPadding: control.indicator.width + 10
        text: control.displayText.length > 0 ? control.displayText : control.currentText
        font: control.font
        color: Theme.textPrimary
        verticalAlignment: Text.AlignVCenter
        elide: Text.ElideRight
    }

    indicator: Text {
        x: control.width - width - 12
        y: (control.height - height) / 2
        text: "▾"
        font.pixelSize: 12
        color: Theme.navySoft
    }

    popup: Popup {
        y: control.height + 2
        width: control.width
        padding: 4

        contentItem: ListView {
            clip: true
            implicitHeight: Math.min(contentHeight, 220)
            model: control.delegateModel
            currentIndex: control.highlightedIndex
            ScrollIndicator.vertical: ScrollIndicator { }
        }

        background: Rectangle {
            radius: Theme.buttonRadius
            color: Theme.cardBg
            border.color: Theme.cardBorder
            border.width: 1
        }
    }

    delegate: ItemDelegate {
        id: delegateItem
        required property int index
        required property var model

        width: control.width - 8
        height: Theme.actionRowHeight - 4
        leftPadding: 12
        rightPadding: 12
        highlighted: control.highlightedIndex === index

        contentItem: Text {
            width: delegateItem.width - delegateItem.leftPadding - delegateItem.rightPadding
            text: {
                let label = control.itemText(delegateItem.model)
                if (!label && Array.isArray(control.model) && delegateItem.index >= 0
                        && delegateItem.index < control.model.length)
                    label = control.itemText(control.model[delegateItem.index])
                return label
            }
            color: Theme.textPrimary
            font.pixelSize: 13
            elide: Text.ElideRight
            verticalAlignment: Text.AlignVCenter
        }

        background: Rectangle {
            color: delegateItem.highlighted ? Theme.listHover : "transparent"
            radius: 2
        }
    }
}
