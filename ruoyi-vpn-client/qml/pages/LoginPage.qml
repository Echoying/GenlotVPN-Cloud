import QtQuick
import QtQuick.Controls
import GenlotVPN 1.0

Item {
    id: root

    property bool purposeAttempted: false

    readonly property int purposeLen: loginPurposeField.text.trim().length
    readonly property int purposeRawLen: loginPurposeField.text.length

    readonly property string purposeHint: {
        if (vpnFlow.loginError.indexOf("登录用途") >= 0)
            return vpnFlow.loginError
        if (purposeAttempted && purposeLen === 0)
            return "请填写登录用途"
        if (purposeLen > 0 && purposeLen < 5)
            return "登录用途至少填写5个字（还需 " + (5 - purposeLen) + " 字）"
        if (purposeRawLen >= 50)
            return "已达 50 字上限"
        if (purposeLen >= 5)
            return "已输入 " + purposeLen + "/50 字"
        return "必填，5～50 字"
    }

    readonly property bool purposeIsError: {
        if (vpnFlow.loginError.indexOf("登录用途") >= 0)
            return true
        if (purposeAttempted && purposeLen === 0)
            return true
        if (purposeLen > 0 && purposeLen < 5)
            return true
        return false
    }

    function openChangePwdDialog() {
        changePwdUsernameField.text = usernameField.text
        changePwdOldField.text = ""
        changePwdNewField.text = ""
        changePwdConfirmField.text = ""
        changePwdOverlay.visible = true
    }

    function closeChangePwdDialog() {
        changePwdOverlay.visible = false
        changePwdOldField.text = ""
        changePwdNewField.text = ""
        changePwdConfirmField.text = ""
    }

    PageShell {
        anchors.fill: parent
        compact: true
        scrollable: false
        bodySpacing: 10
        headerSubtitle: vpnFlow.pendingLine.appName || "未选择"
        headerSubtitleBold: true
        headerSubtitleFontSize: 14
        showProgress: vpnFlow.loading
        progress: vpnFlow.loading ? 0.5 : 0

        FlatField {
            id: usernameField
            width: parent.width
            placeholderText: "账号"
            hasError: vpnFlow.loginError.length > 0
                      && (vpnFlow.loginError.indexOf("账号") >= 0
                          || vpnFlow.loginError.indexOf("用户") >= 0)
        }

        FlatField {
            id: passwordField
            width: parent.width
            placeholderText: "密码"
            echoMode: TextInput.Password
            hasError: vpnFlow.loginError.length > 0
                      && vpnFlow.loginError.indexOf("验证码") < 0
                      && vpnFlow.loginError.indexOf("登录用途") < 0
        }

        Column {
            width: parent.width
            spacing: 4

            FlatTextArea {
                id: loginPurposeField
                width: parent.width
                placeholderText: "登录用途（必填，5～50字）"
                maximumLength: 50
                hasError: root.purposeIsError
                onTextChanged: {
                    if (vpnFlow.loginError.indexOf("登录用途") >= 0)
                        vpnFlow.clearLoginError()
                }
            }

            Text {
                width: parent.width
                text: root.purposeHint
                color: root.purposeIsError ? Theme.danger : Theme.textSecondary
                font.pixelSize: 12
                wrapMode: Text.Wrap
            }
        }

        Row {
            width: parent.width
            spacing: 8
            visible: vpnFlow.captchaEnabled

            FlatField {
                id: codeField
                width: parent.width - 116
                placeholderText: "计算结果"
                hasError: vpnFlow.loginError.indexOf("验证码") >= 0
            }

            Rectangle {
                width: 108
                height: Theme.actionRowHeight
                radius: Theme.buttonRadius
                color: Theme.inputBg
                border.color: Theme.inputBorder
                clip: true

                Image {
                    anchors.fill: parent
                    anchors.margins: 2
                    source: vpnFlow.captchaImage
                    fillMode: Image.PreserveAspectFit
                }

                MouseArea {
                    anchors.fill: parent
                    cursorShape: Qt.PointingHandCursor
                    onClicked: vpnFlow.prepareLogin()
                }
            }
        }

        CheckBox {
            id: rememberBox
            text: "记住密码"
            checked: false
            font.pixelSize: 13
            spacing: 8

            indicator: Rectangle {
                implicitWidth: 16
                implicitHeight: 16
                x: rememberBox.leftPadding
                y: parent.height / 2 - height / 2
                radius: 2
                color: "transparent"
                border.color: Theme.textPrimary
                border.width: 1

                Text {
                    anchors.centerIn: parent
                    text: "✓"
                    color: Theme.textPrimary
                    font.pixelSize: 11
                    font.bold: true
                    visible: rememberBox.checked
                }
            }

            contentItem: Text {
                text: rememberBox.text
                font.pixelSize: rememberBox.font.pixelSize
                color: Theme.textPrimary
                leftPadding: rememberBox.indicator.width + rememberBox.spacing
                verticalAlignment: Text.AlignVCenter
            }
        }

        PrimaryButton {
            text: "登 录"
            width: parent.width
            enabled: !vpnFlow.loading
            onClicked: {
                root.purposeAttempted = true
                vpnFlow.doLogin(usernameField.text, passwordField.text, codeField.text,
                                rememberBox.checked, loginPurposeField.text)
            }
        }

        Rectangle {
            visible: vpnFlow.loginError.length > 0
            width: parent.width
            radius: Theme.buttonRadius
            color: "#FFF0F0"
            border.color: Theme.danger
            border.width: 1
            implicitHeight: loginErrorRow.implicitHeight + 20

            Row {
                id: loginErrorRow
                anchors.fill: parent
                anchors.margins: 10
                spacing: 10

                Rectangle {
                    width: 20
                    height: 20
                    radius: 10
                    color: Theme.danger
                    anchors.verticalCenter: parent.verticalCenter

                    Text {
                        anchors.centerIn: parent
                        text: "!"
                        color: Theme.textOnPrimary
                        font.pixelSize: 13
                        font.bold: true
                    }
                }

                Text {
                    width: parent.width - 30
                    anchors.verticalCenter: parent.verticalCenter
                    text: vpnFlow.loginError
                    color: Theme.danger
                    font.pixelSize: 14
                    font.bold: true
                    wrapMode: Text.Wrap
                }
            }
        }

        Item {
            width: parent.width
            height: Math.max(changeLineBtn.implicitHeight, changePwdBtn.implicitHeight)

            GhostButton {
                id: changeLineBtn
                anchors.left: parent.left
                anchors.verticalCenter: parent.verticalCenter
                text: "更换线路"
                emphasized: true
                onClicked: vpnFlow.goChooseLine()
            }

            GhostButton {
                id: changePwdBtn
                anchors.right: parent.right
                anchors.verticalCenter: parent.verticalCenter
                text: "修改密码"
                emphasized: true
                onClicked: root.openChangePwdDialog()
            }
        }
    }

    Rectangle {
        id: changePwdOverlay
        visible: false
        anchors.fill: parent
        color: "#44000000"
        z: 100

        MouseArea {
            anchors.fill: parent
            onClicked: {}
        }

        Rectangle {
            width: Math.min(parent.width * Theme.contentWidthRatio, Theme.cardWidth)
            anchors.centerIn: parent
            radius: Theme.cardRadius
            color: Theme.cardBg
            border.color: Theme.cardBorder
            border.width: 1
            implicitHeight: changePwdColumn.implicitHeight

            Column {
                id: changePwdColumn
                width: parent.width - 32
                anchors.horizontalCenter: parent.horizontalCenter
                spacing: 12
                topPadding: 20
                bottomPadding: 20

                Text {
                    width: parent.width
                    text: "修改密码"
                    color: Theme.navy
                    font.pixelSize: 15
                    font.bold: true
                }

                FlatField {
                    id: changePwdUsernameField
                    width: parent.width
                    placeholderText: "用户名"
                }

                FlatField {
                    id: changePwdOldField
                    width: parent.width
                    placeholderText: "旧密码"
                    echoMode: TextInput.Password
                }

                FlatField {
                    id: changePwdNewField
                    width: parent.width
                    placeholderText: "新密码（5-20 位）"
                    echoMode: TextInput.Password
                }

                FlatField {
                    id: changePwdConfirmField
                    width: parent.width
                    placeholderText: "确认新密码"
                    echoMode: TextInput.Password
                }

                Row {
                    width: parent.width
                    spacing: 10
                    layoutDirection: Qt.RightToLeft

                    PrimaryButton {
                        text: "确定"
                        implicitWidth: 80
                        enabled: !vpnFlow.loading
                        onClicked: vpnFlow.changePassword(
                            changePwdUsernameField.text,
                            changePwdOldField.text,
                            changePwdNewField.text,
                            changePwdConfirmField.text)
                    }

                    GhostButton {
                        text: "取消"
                        implicitWidth: 64
                        enabled: !vpnFlow.loading
                        onClicked: root.closeChangePwdDialog()
                    }
                }
            }
        }
    }

    Connections {
        target: vpnFlow
        function onChangePasswordFinished() {
            usernameField.text = changePwdUsernameField.text
            passwordField.text = ""
            if (rememberBox.checked) {
                vpnStorage.saveRememberedUser(usernameField.text, "", false)
                rememberBox.checked = false
            }
            root.closeChangePwdDialog()
        }
    }

    Component.onCompleted: {
        vpnFlow.prepareLogin()
        const saved = vpnStorage.loadRememberedUser()
        if (saved.remember) {
            usernameField.text = saved.username || ""
            passwordField.text = saved.password || ""
            rememberBox.checked = true
        }
    }
}
