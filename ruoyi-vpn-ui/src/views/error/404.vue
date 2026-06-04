<template>
  <div style="text-align:center; padding-top: 100px;">
    <h1>404</h1>
    <p>页面不存在</p>
    <el-button type="primary" @click="goHome">返回首页</el-button>
  </div>
</template>

<script>
import { getToken } from '@/utils/auth'
import store from '@/store'

export default {
  name: 'Page404',
  methods: {
    goHome() {
      if (getToken()) {
        const target = store.state.user.selectedLine ? '/app-list' : '/select-line'
        this.$router.replace(target)
      } else {
        this.$router.replace('/login')
      }
    }
  }
}
</script>
