import router from './router'
import store from './store'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { getPendingLine } from '@/utils/pendingLine'

NProgress.configure({ showSpinner: false })

const whiteList = ['/choose-line', '/login']

router.beforeEach((to, from, next) => {
  NProgress.start()
  const hasToken = getToken()
  const pendingLine = store.state.user.pendingLine || getPendingLine()

  if (hasToken) {
    if (to.path === '/login' || to.path === '/choose-line' || to.path === '/') {
      const target = store.state.user.selectedLine ? '/app-list' : '/select-line'
      next({ path: target, replace: true })
      NProgress.done()
    } else if (to.path === '/select-line' && !pendingLine) {
      next({ path: '/choose-line', replace: true })
      NProgress.done()
    } else {
      next()
    }
  } else {
    if (to.path === '/') {
      next({ path: '/choose-line', replace: true })
      NProgress.done()
    } else if (whiteList.indexOf(to.path) !== -1) {
      if (to.path === '/login' && !pendingLine) {
        next({ path: '/choose-line', replace: true })
        NProgress.done()
      } else {
        next()
      }
    } else {
      next(`/choose-line?redirect=${encodeURIComponent(to.fullPath)}`)
      NProgress.done()
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})
