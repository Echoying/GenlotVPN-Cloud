<template>
  <div style="text-align:center; padding-top: 100px;">
    <h1>404</h1>
    <p>页面不存在</p>
    <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
  </div>
</template>

<script>
export default { name: 'Page404' }
</script>
