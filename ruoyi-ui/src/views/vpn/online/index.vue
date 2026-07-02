<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="80px">
      <el-form-item label="线路" prop="appIds">
        <el-select
          v-model="selectedAppIds"
          multiple
          collapse-tags
          clearable
          filterable
          placeholder="请选择线路"
          style="width: 280px"
        >
          <el-option
            v-for="line in lineOptions"
            :key="line.appId"
            :label="line.appName"
            :value="line.appId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="用户名称" prop="userName">
        <el-input
          v-model="queryParams.userName"
          placeholder="请输入用户名称"
          clearable
          style="width: 240px;"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="登录地址" prop="ipaddr">
        <el-input
          v-model="queryParams.ipaddr"
          placeholder="请输入登录地址"
          clearable
          style="width: 240px;"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table
      v-loading="loading"
      :data="list.slice((pageNum-1)*pageSize,pageNum*pageSize)"
      style="width: 100%;"
    >
      <el-table-column label="序号" type="index" align="center" width="60">
        <template slot-scope="scope">
          <span>{{ (pageNum - 1) * pageSize + scope.$index + 1 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="80">
        <template slot-scope="_">
          <el-tag type="success" size="mini">在线</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="会话编号" align="center" prop="tokenId" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="登录账号" align="center" prop="userName" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="昵称" align="center" prop="nickName" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="线路名称" align="center" prop="appName" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="登录 IP" align="center" prop="ipaddr" width="130" :show-overflow-tooltip="true" />
      <el-table-column label="操作系统" align="center" prop="clientOs" width="160" :show-overflow-tooltip="true" />
      <el-table-column label="MAC 地址" align="center" prop="clientMac" width="140" :show-overflow-tooltip="true" />
      <el-table-column label="登录用途" align="center" prop="loginPurpose" min-width="160" :show-overflow-tooltip="true" />
      <el-table-column label="登录时间" align="center" prop="loginTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.loginTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="100">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleForceLogout(scope.row)"
            v-hasPermi="['vpn:online:forceLogout']"
          >强退</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="pageNum" :limit.sync="pageSize" />
  </div>
</template>

<script>
import { list, forceLogout } from '@/api/vpn/online'
import { listLineApp } from '@/api/vpn/line'

export default {
  name: 'VpnOnline',
  data() {
    return {
      loading: true,
      total: 0,
      list: [],
      lineOptions: [],
      selectedAppIds: [],
      pageNum: 1,
      pageSize: 10,
      queryParams: {
        ipaddr: undefined,
        userName: undefined,
        appIds: undefined
      }
    }
  },
  created() {
    this.loadLineOptions()
    this.getList()
  },
  methods: {
    loadLineOptions() {
      listLineApp({ pageNum: 1, pageSize: 1000 }).then(response => {
        this.lineOptions = response.rows || []
      })
    },
    getList() {
      this.loading = true
      this.queryParams.appIds = this.selectedAppIds.length ? this.selectedAppIds.join(',') : undefined
      list(this.queryParams).then(response => {
        this.list = response.rows || []
        this.total = response.total != null ? response.total : this.list.length
        this.loading = false
      })
    },
    handleQuery() {
      this.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.selectedAppIds = []
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleForceLogout(row) {
      const tip = '将清除该用户的云端会话；若客户端已连接 VPN，将在下次心跳检测后自动退出。是否确认强退用户「' + row.userName + '」？'
      this.$modal.confirm(tip).then(() => {
        return forceLogout(row.tokenId)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('强退成功')
      }).catch(() => {})
    }
  }
}
</script>
