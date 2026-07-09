import QtQuick

pragma Singleton

QtObject {
    readonly property color navy: "#0B2D5B"
    readonly property color navySoft: "#2E5F8F"
    readonly property color accent: "#D4A84B"
    readonly property color success: "#2E9E6A"
    readonly property color danger: "#D64545"
    readonly property color textPrimary: "#1A2B3C"
    readonly property color textSecondary: "#8A96A8"
    readonly property color windowBg: "#F2F4F7"
    readonly property color cardBg: "#FFFFFF"
    readonly property color cardBorder: "#D8DEE8"
    readonly property color listHover: "#EEF2F8"
    readonly property color listStripeA: "#FFFFFF"
    readonly property color listStripeB: "#EEF2F8"
    readonly property color logBg: "#F8FAFD"
    readonly property color logText: "#5A6B80"
    readonly property color logBorder: "#E2E8F0"
    readonly property int appListWindowWidth: 920
    readonly property int appListWindowHeight: 620
    readonly property int appListWindowMinWidth: 800
    readonly property int appListWindowMinHeight: 520
    readonly property int buttonRadius: 4
    readonly property real logoWidthRatio: 0.42
    readonly property string assetLogoPng: "qrc:/GenlotVPNProxy/assets/images/genlot-logo-official.png"
    readonly property string assetIcon: "qrc:/GenlotVPNProxy/assets/images/genlot-app-icon-official.png"
    readonly property string assetAppIco: "qrc:/GenlotVPNProxy/assets/images/genlot-app.ico"
}
