import QtQuick
import QtQuick.Controls
import GenlotVPN 1.0

// 线路下拉：绑定查询到的 publicLines（QVariantMap 列表）
ComboBox {
    id: control
    property var lines: []
    property int controlHeight: Theme.actionRowHeight
    property int fontSize: 13
    property bool refreshOnOpen: false
    property bool loading: false
    signal refreshRequested()
    implicitHeight: controlHeight
    font.pixelSize: fontSize

    model: lines
    textRole: "appName"

    onLinesChanged: {
        if (lines.length > 0 && (currentIndex < 0 || currentIndex >= lines.length))
            currentIndex = 0
    }

    displayText: {
        if (control.currentIndex >= 0 && control.currentIndex < lines.length) {
            const line = lines[control.currentIndex]
            const name = line.appName || ""
            const host = line.host || ""
            const port = line.srvPort || ""
            if (host)
                return name + "  (" + host + (port ? (":" + port) : "") + ")"
            return name
        }
        if (lines.length === 0)
            return control.loading ? qsTr("正在加载线路...") : qsTr("点击选择线路")
        return qsTr("请选择线路")
    }

    onPressedChanged: {
        if (pressed && refreshOnOpen)
            refreshRequested()
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
        font.pixelSize: Math.max(12, control.fontSize - 2)
        color: Theme.navySoft
    }

    popup: Popup {
        y: control.height + 2
        width: control.width
        padding: 4

        contentItem: ListView {
            clip: true
            implicitHeight: Math.min(contentHeight, 260)
            model: control.popup.visible ? control.delegateModel : null
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
        height: Math.max(controlHeight - 4, lineName.height + lineHost.height + 16)
        highlighted: control.highlightedIndex === index

        background: Rectangle {
            color: parent.highlighted ? Theme.listHover : "transparent"
            radius: 2
        }

        Column {
            anchors.left: parent.left
            anchors.right: parent.right
            anchors.verticalCenter: parent.verticalCenter
            anchors.leftMargin: 10
            anchors.rightMargin: 10
            spacing: 2

            Text {
                id: lineName
                width: parent.width
                text: modelData.appName || ""
                font.pixelSize: control.fontSize
                font.bold: true
                color: Theme.textPrimary
                elide: Text.ElideRight
            }
            Text {
                id: lineHost
                width: parent.width
                text: (modelData.host || "") + (modelData.srvPort ? (":" + modelData.srvPort) : "")
                font.pixelSize: Math.max(11, control.fontSize - 2)
                color: Theme.textSecondary
                elide: Text.ElideRight
                visible: text.length > 0
            }
        }
    }
}
