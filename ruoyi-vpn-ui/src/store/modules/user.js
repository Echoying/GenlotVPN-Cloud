import { login } from '@/api/login'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { getPendingLine, setPendingLine, clearPendingLine } from '@/utils/pendingLine'

const user = {
  state: {
    token: getToken(),
    pendingLine: getPendingLine(),
    selectedLine: null
  },
  mutations: {
    SET_TOKEN: (state, token) => {
      state.token = token
    },
    SET_PENDING_LINE: (state, line) => {
      state.pendingLine = line
      setPendingLine(line)
    },
    SET_LINE: (state, line) => {
      state.selectedLine = line
    },
    CLEAR_LINE_STATE: (state) => {
      state.pendingLine = null
      state.selectedLine = null
      clearPendingLine()
    }
  },
  actions: {
    // 登录
    Login({ commit, state }, userInfo) {
      const username = userInfo.username.trim()
      const password = userInfo.password
      const code = userInfo.code
      const uuid = userInfo.uuid
      const appId = state.pendingLine && state.pendingLine.appId
      return new Promise((resolve, reject) => {
        login(username, password, code, uuid, appId).then(res => {
          const token = res.data.access_token
          setToken(token)
          commit('SET_TOKEN', token)
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    },
    // 选择线路
    SelectLine({ commit }, line) {
      return new Promise(resolve => {
        commit('SET_LINE', line)
        resolve()
      })
    },
    // 退出
    LogOut({ commit }) {
      return new Promise(resolve => {
        commit('SET_TOKEN', '')
        commit('CLEAR_LINE_STATE')
        removeToken()
        resolve()
      })
    }
  }
}

export default user
