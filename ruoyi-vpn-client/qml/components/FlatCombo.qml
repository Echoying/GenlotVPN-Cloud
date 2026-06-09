import QtQuick
import QtQuick.Controls
import GenlotVPN 1.0

ComboBox {
    id: control
    implicitHeight: Theme.actionRowHeight
    leftPadding: 14
    font.pixelSize: 13

    background: Rectangle {
        radius: Theme.buttonRadius
        color: Theme.inputBg
        border.color: control.activeFocus ? Theme.inputFocus : Theme.inputBorder
        border.width: 1
    }

    contentItem: Text {
        leftPadding: 14
        rightPadding: control.indicator.width + 8
        text: control.displayText
        font: control.font
        color: control.enabled ? Theme.textPrimary : Theme.textSecondary
        verticalAlignment: Text.AlignVCenter
        elide: Text.ElideRight
    }

    indicator: Text {
        x: control.width - width - 12
        y: (control.height - height) / 2
        text: "▾"
        font.pixelSize: 12
        color: Theme.textSecondary
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
        width: control.width - 8
        contentItem: Text {
            text: modelData
            color: Theme.textPrimary
            font.pixelSize: 13
            elide: Text.ElideRight
            verticalAlignment: Text.AlignVCenter
        }
        highlighted: control.highlightedIndex === index
        background: Rectangle {
            color: highlighted ? Theme.listHover : "transparent"
            radius: 2
        }
    }
}
