import QtQuick

pragma Singleton

QtObject {
    // Genlot 企业色
    readonly property color navy: "#0B2D5B"
    readonly property color navyLight: "#1E4A7A"
    readonly property color navySoft: "#2E5F8F"
    readonly property color accent: "#D4A84B"
    readonly property color accentLight: "#F0D48A"

    readonly property color primary: navy
    readonly property color success: "#2E9E6A"
    readonly property color danger: "#D64545"

    readonly property color textPrimary: "#1A2B3C"
    readonly property color textSecondary: "#8A96A8"
    readonly property color textOnPrimary: "#FFFFFF"

    readonly property color windowBg: "#F2F4F7"
    readonly property color cardBg: "#FFFFFF"
    readonly property color cardBorder: "#D8DEE8"
    readonly property color listHover: "#EEF2F8"
    readonly property color listNormal: "#F7F9FC"
    readonly property color listStripeA: "#FFFFFF"
    readonly property color listStripeB: "#EEF2F8"

    readonly property color logBg: "#F8FAFD"
    readonly property color logText: "#5A6B80"
    readonly property color logBorder: "#E2E8F0"

    readonly property color inputBg: "#ECEFF4"
    readonly property color inputBorder: "#D0D7E2"
    readonly property color inputFocus: navySoft

    // 窗口比例 3:2（对齐 Easy Connect）
    readonly property int windowWidth: 720
    readonly property int windowHeight: 480
    readonly property int windowMinWidth: 600
    readonly property int windowMinHeight: 400

    // 选线页内容较少，窗口略紧凑
    readonly property int chooseLineWindowWidth: 640
    readonly property int chooseLineWindowHeight: 380
    readonly property int chooseLineWindowMinWidth: 520
    readonly property int chooseLineWindowMinHeight: 300

    // 设置页
    readonly property int settingsWindowWidth: 680
    readonly property int settingsWindowHeight: 420
    readonly property int settingsWindowMinWidth: 560
    readonly property int settingsWindowMinHeight: 360
    readonly property int settingsNavWidth: 168

    // 设置页扁平配色
    readonly property color settingsSidebarBg: "#F5F7FA"
    readonly property color settingsSidebarActive: "#FFFFFF"
    readonly property color settingsDivider: "#E4E9F0"

    // 应用列表页放大窗口
    readonly property int appListWindowWidth: 920
    readonly property int appListWindowHeight: 620
    readonly property int appListWindowMinWidth: 800
    readonly property int appListWindowMinHeight: 520

    readonly property real contentWidthRatio: 0.72
    readonly property real logoWidthRatio: 0.48
    readonly property int pageTopMargin: 48
    readonly property int sectionSpacing: 28
    readonly property int actionRowHeight: 44
    readonly property int actionBtnSize: 44

    readonly property int cardRadius: 4
    readonly property int cardPadding: 0
    readonly property int cardWidth: 520
    readonly property int buttonRadius: 4
    readonly property int headerHeight: 40

    readonly property string assetBg: Qt.resolvedUrl("../assets/images/genlot-bg-light.png")
    readonly property string assetLogoSvg: Qt.resolvedUrl("../assets/images/genlot-logo.svg")
    readonly property string assetLogoPng: Qt.resolvedUrl("../assets/images/genlot-logo-official.png")
    readonly property string assetIcon: Qt.resolvedUrl("../assets/images/genlot-app-icon-official.png")
}
