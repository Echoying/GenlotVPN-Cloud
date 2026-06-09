import QtQuick
import GenlotVPN 1.0

Item {
    id: card
    default property alias content: contentItem.data
    property int cardWidth: Theme.cardWidth

    width: cardWidth
    implicitHeight: contentItem.childrenRect.height + Theme.cardPadding * 2

    Rectangle {
        id: panel
        anchors.fill: parent
        radius: Theme.cardRadius
        color: Theme.cardBg
        border.color: Theme.cardBorder
        border.width: 1
        clip: true

        Image {
            anchors.fill: parent
            source: Theme.assetBg
            fillMode: Image.PreserveAspectCrop
            asynchronous: true
            opacity: 0.22
        }

        Rectangle {
            anchors.fill: parent
            color: Theme.cardBg
            opacity: 0.88
        }

        Rectangle {
            anchors.top: parent.top
            anchors.left: parent.left
            anchors.right: parent.right
            height: 2
            color: Theme.accent
            opacity: 0.55
        }
    }

    Item {
        id: contentItem
        anchors.fill: parent
        anchors.margins: Theme.cardPadding
    }
}
