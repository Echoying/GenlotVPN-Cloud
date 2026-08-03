import QtQuick
import QtQuick.Controls
import GenlotVPN 1.0

TextField {
    id: control
    property bool hasError: false
    property bool digitsOnly: false
    /**
     * digitsOnly 时的数字个数上限。
     * 注意：不要用 maximumLength 限制原始粘贴长度，否则钉钉长文本会被先截成中文再滤成空。
     */
    property int maxDigits: 0

    implicitHeight: Theme.actionRowHeight
    leftPadding: 14
    rightPadding: 14
    color: Theme.textPrimary
    placeholderTextColor: Theme.textSecondary
    font.pixelSize: 13
    selectByMouse: true
    persistentSelection: true
    // 放宽输入法限制，避免系统拦截含中文/空格的剪贴板粘贴
    inputMethodHints: Qt.ImhNone

    function digitCap() {
        if (maxDigits > 0) {
            return maxDigits
        }
        // 兼容旧用法：父组件把 maximumLength 当数字上限
        return maximumLength > 0 ? maximumLength : 0
    }

    function sanitizeDigits(value) {
        let digits = String(value || "").replace(/\D/g, "")
        const cap = digitCap()
        if (cap > 0 && digits.length > cap) {
            digits = digits.substring(0, cap)
        }
        return digits
    }

    function applyDigits(value, preferredCursor) {
        const digits = sanitizeDigits(value)
        if (digits !== text) {
            text = digits
        }
        cursorPosition = Math.min(preferredCursor >= 0 ? preferredCursor : digits.length, digits.length)
    }

    function pasteDigitsFromClipboard() {
        if (!digitsOnly) {
            paste()
            return
        }
        const clip = (typeof vpnFlow !== "undefined" && vpnFlow && vpnFlow.clipboardText)
                     ? String(vpnFlow.clipboardText())
                     : ""
        if (!clip) {
            return
        }
        const insert = sanitizeDigits(clip)
        if (!insert) {
            return
        }
        const start = selectionStart
        const end = selectionEnd
        const before = text.substring(0, start)
        const after = text.substring(end)
        applyDigits(before + insert + after, before.length + insert.length)
    }

    onTextChanged: {
        if (!digitsOnly || _applyingDigits) {
            return
        }
        _applyingDigits = true
        applyDigits(text, -1)
        _applyingDigits = false
    }

    property bool _applyingDigits: false

    Keys.onPressed: function(event) {
        if (!digitsOnly) {
            return
        }
        const isPaste = event.matches(StandardKey.Paste)
                || (event.key === Qt.Key_V && (event.modifiers & Qt.ControlModifier))
                || (event.key === Qt.Key_Insert && (event.modifiers & Qt.ShiftModifier))
        if (!isPaste) {
            return
        }
        event.accepted = true
        pasteDigitsFromClipboard()
    }

    background: Rectangle {
        radius: Theme.buttonRadius
        color: control.hasError ? "#FFF5F5" : Theme.inputBg
        border.color: control.hasError ? Theme.danger
                                        : (control.activeFocus ? Theme.inputFocus : Theme.inputBorder)
        border.width: control.hasError ? 2 : 1
    }
}
