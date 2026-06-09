import QtQuick
import QtQuick.Controls
import GenlotVPN 1.0

// 主操作行：宽输入/下拉 + 右侧方形按钮
Row {
    id: root
    property alias field: fieldItem
    default property alias fieldContent: fieldItem.data
    property string buttonText: "›"
    property bool buttonEnabled: true
    property bool primaryButton: false
    property int rowHeight: Theme.actionRowHeight
    property int btnSize: Theme.actionBtnSize
    property int actionFontSize: 22
    signal actionClicked()

    spacing: 10
    width: parent ? parent.width : Theme.cardWidth
    height: rowHeight

    Item {
        id: fieldItem
        width: parent.width - root.btnSize - parent.spacing
        height: root.rowHeight
    }

    Button {
        id: actionBtn
        width: root.btnSize
        height: root.btnSize
        enabled: root.buttonEnabled
        onClicked: root.actionClicked()

        background: Rectangle {
            radius: Theme.buttonRadius
            color: {
                if (!actionBtn.enabled)
                    return "#B8C5D6"
                if (root.primaryButton) {
                    if (actionBtn.pressed) return Theme.navyLight
                    if (actionBtn.hovered) return Theme.navySoft
                    return Theme.navy
                }
                if (actionBtn.pressed) return Theme.listHover
                if (actionBtn.hovered) return Theme.cardBg
                return Theme.cardBg
            }
            border.color: root.primaryButton ? "transparent" : Theme.inputBorder
            border.width: root.primaryButton ? 0 : 1
        }

        contentItem: Text {
            text: root.buttonText
            font.pixelSize: root.actionFontSize
            font.bold: true
            color: actionBtn.enabled
                   ? (root.primaryButton ? Theme.textOnPrimary : Theme.navy)
                   : Theme.textSecondary
            horizontalAlignment: Text.AlignHCenter
            verticalAlignment: Text.AlignVCenter
        }
    }
}
