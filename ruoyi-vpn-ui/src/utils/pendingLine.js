const PENDING_LINE_KEY = 'vpn-pending-line'

/** 保存登录前选择的线路 */
export function setPendingLine(line) {
  if (line) {
    sessionStorage.setItem(PENDING_LINE_KEY, JSON.stringify(line))
  } else {
    sessionStorage.removeItem(PENDING_LINE_KEY)
  }
}

/** 读取登录前选择的线路 */
export function getPendingLine() {
  const raw = sessionStorage.getItem(PENDING_LINE_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw)
  } catch (e) {
    return null
  }
}

export function clearPendingLine() {
  sessionStorage.removeItem(PENDING_LINE_KEY)
}
