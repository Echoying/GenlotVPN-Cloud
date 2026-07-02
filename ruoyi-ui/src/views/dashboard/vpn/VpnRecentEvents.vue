<template>
  <div class="vpn-recent-events">
    <div class="vpn-recent-events__header">
      <span class="vpn-recent-events__title">最近动态</span>
      <el-button type="text" @click="goLoginLog">查看全部</el-button>
    </div>
    <el-table v-loading="loading" :data="events" size="small" style="width: 100%">
      <el-table-column label="时间" prop="accessTime" width="170">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.accessTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="用户" prop="userName" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="线路" prop="appName" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="状态" prop="status" width="90" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'" size="mini">
            {{ scope.row.status === '0' ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="登录用途" prop="loginPurpose" min-width="220" :show-overflow-tooltip="true" />
      <el-table-column label="说明" prop="msg" min-width="160" :show-overflow-tooltip="true" />
      <el-table-column label="操作" width="90" align="center">
        <template slot-scope="scope">
          <el-button type="text" size="mini" @click="viewEvent(scope.row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !events.length" description="暂无动态" :image-size="80" />
  </div>
</template>

<script>
export default {
  name: 'VpnRecentEvents',
  props: {
    loading: { type: Boolean, default: false },
    events: { type: Array, default: () => [] }
  },
  methods: {
    goLoginLog() {
      this.$router.push({ path: '/yianlian/logininfor' })
    },
    viewEvent(row) {
      const query = { userName: row.userName || undefined }
      if (row.status) {
        query.status = row.status
      }
      this.$router.push({ path: '/yianlian/logininfor', query })
    }
  }
}
</script>

<style lang="scss" scoped>
.vpn-recent-events {
  background: #fff;
  padding: 16px;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
  }

  &__title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }
}
</style>
