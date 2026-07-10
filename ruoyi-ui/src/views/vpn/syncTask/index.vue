<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
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
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="任务状态" clearable style="width: 140px">
          <el-option
            v-for="item in statusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="业务类型" prop="bizType">
        <el-select v-model="queryParams.bizType" placeholder="业务类型" clearable style="width: 140px">
          <el-option
            v-for="item in bizTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="操作" prop="operation">
        <el-select v-model="queryParams.operation" placeholder="操作类型" clearable style="width: 140px">
          <el-option
            v-for="item in operationOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="业务ID" prop="bizId">
        <el-input
          v-model="queryParams.bizId"
          placeholder="请输入业务ID"
          clearable
          style="width: 160px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="创建时间">
        <el-date-picker
          v-model="dateRange"
          style="width: 240px"
          value-format="yyyy-MM-dd HH:mm:ss"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :default-time="['00:00:00', '23:59:59']"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="任务" align="left" min-width="220" :show-overflow-tooltip="true">
        <template slot-scope="scope">
          <span>{{ formatTaskLabel(scope.row) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="线路" align="center" min-width="160" :show-overflow-tooltip="true">
        <template slot-scope="scope">
          <span>{{ formatLine(scope.row.appId) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="业务类型" align="center" prop="bizType" width="110">
        <template slot-scope="scope">
          <span>{{ bizTypeLabel(scope.row.bizType) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" prop="operation" width="100">
        <template slot-scope="scope">
          <span>{{ operationLabel(scope.row.operation) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="业务对象" align="center" prop="bizName" min-width="120" :show-overflow-tooltip="true">
        <template slot-scope="scope">
          <span>{{ scope.row.bizName || scope.row.bizId || '新建' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template slot-scope="scope">
          <el-tag :type="statusTagType(scope.row.status)" size="small">
            {{ statusLabel(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="重试次数" align="center" prop="retryCount" width="80" />
      <el-table-column label="优先级" align="center" prop="priority" width="70" />
      <el-table-column label="下次执行" align="center" prop="nextRetryTime" width="160">
        <template slot-scope="scope">
          <span>{{ scope.row.nextRetryTime ? parseTime(scope.row.nextRetryTime) : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="最近错误" align="center" prop="lastError" min-width="180" :show-overflow-tooltip="true" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="80" fixed="right">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleDetail(scope.row)"
            v-hasPermi="['yianlian:synctask:query']"
          >详细</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog title="同步任务详细" :visible.sync="detailOpen" width="780px" append-to-body>
      <el-descriptions :column="2" border size="small" v-if="detail">
        <el-descriptions-item label="记录号">{{ detail.taskId }}</el-descriptions-item>
        <el-descriptions-item label="任务">{{ formatTaskLabel(detail) }}</el-descriptions-item>
        <el-descriptions-item label="线路">{{ formatLine(detail.appId) }}</el-descriptions-item>
        <el-descriptions-item label="业务类型">{{ bizTypeLabel(detail.bizType) }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ operationLabel(detail.operation) }}</el-descriptions-item>
        <el-descriptions-item label="业务对象">{{ detail.bizName || detail.bizId || '新建' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detail.status)" size="small">{{ statusLabel(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="重试次数">{{ detail.retryCount }}</el-descriptions-item>
        <el-descriptions-item label="优先级">{{ detail.priority }}</el-descriptions-item>
        <el-descriptions-item label="创建者">{{ detail.createBy || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ parseTime(detail.updateTime) }}</el-descriptions-item>
        <el-descriptions-item label="下次执行">{{ detail.nextRetryTime ? parseTime(detail.nextRetryTime) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近错误" :span="2">{{ detail.lastError || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div class="detail-payload" v-if="detail">
        <div class="detail-payload-title">命令快照（payload）</div>
        <pre class="detail-payload-body">{{ formattedPayload }}</pre>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="detailOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listSyncTask, getSyncTask } from '@/api/vpn/syncTask'
import { listLineApp } from '@/api/vpn/line'

const BIZ_TYPE_OPTIONS = [
  { value: 'DEPT', label: '部门' },
  { value: 'ROLE', label: '角色' },
  { value: 'SERVICE', label: '服务' },
  { value: 'SERVICE_GROUP', label: '服务组' },
  { value: 'VPN_USER', label: '线路用户' },
  { value: 'LOCAL_USER', label: '本地用户' },
  { value: 'YAL_DEPT_AUTH', label: '部门授权' },
  { value: 'YAL_ROLE_AUTH', label: '角色授权' },
  { value: 'YAL_USER_AUTH', label: '用户授权' }
]

const OPERATION_OPTIONS = [
  { value: 'CREATE', label: '新增' },
  { value: 'UPDATE', label: '修改' },
  { value: 'DELETE', label: '删除' },
  { value: 'RESET_PASSWORD', label: '重置密码' },
  { value: 'CHANGE_STATUS', label: '变更状态' },
  { value: 'ASSIGN_ROLES', label: '分配角色' }
]

const STATUS_OPTIONS = [
  { value: '0', label: '待处理' },
  { value: '1', label: '处理中' },
  { value: '2', label: '成功' },
  { value: '3', label: '放弃' }
]

export default {
  name: 'VpnSyncTask',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      list: [],
      lineOptions: [],
      selectedAppIds: [],
      dateRange: [],
      detailOpen: false,
      detail: null,
      bizTypeOptions: BIZ_TYPE_OPTIONS,
      operationOptions: OPERATION_OPTIONS,
      statusOptions: STATUS_OPTIONS,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        status: undefined,
        bizType: undefined,
        operation: undefined,
        bizId: undefined,
        appIds: undefined
      }
    }
  },
  computed: {
    lineNameMap() {
      const map = {}
      this.lineOptions.forEach(line => {
        map[line.appId] = line.appName
      })
      return map
    },
    formattedPayload() {
      if (!this.detail || !this.detail.payload) {
        return '-'
      }
      try {
        return JSON.stringify(JSON.parse(this.detail.payload), null, 2)
      } catch (e) {
        return this.detail.payload
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
      listSyncTask(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
        this.list = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.dateRange = []
      this.selectedAppIds = []
      this.resetForm('queryForm')
      this.queryParams.pageNum = 1
      this.getList()
    },
    handleDetail(row) {
      getSyncTask(row.taskId).then(response => {
        this.detail = response.data
        this.detailOpen = true
      })
    },
    formatLine(appId) {
      if (!appId) {
        return '-'
      }
      const name = this.lineNameMap[appId]
      return name ? `${name}（${appId}）` : appId
    },
    formatTaskLabel(row) {
      if (!row) {
        return '-'
      }
      const type = this.bizTypeLabel(row.bizType)
      const op = this.operationLabel(row.operation)
      const target = row.bizName || row.bizId || '新建'
      return `${type} · ${op} · ${target}`
    },
    bizTypeLabel(value) {
      const item = BIZ_TYPE_OPTIONS.find(o => o.value === value)
      return item ? item.label : (value || '-')
    },
    operationLabel(value) {
      const item = OPERATION_OPTIONS.find(o => o.value === value)
      return item ? item.label : (value || '-')
    },
    statusLabel(value) {
      const normalized = value === undefined || value === null ? '' : String(value)
      const item = STATUS_OPTIONS.find(o => o.value === normalized)
      return item ? item.label : (normalized || '-')
    },
    statusTagType(value) {
      const map = { '0': 'info', '1': 'warning', '2': 'success', '3': 'danger' }
      return map[String(value)] || 'info'
    }
  }
}
</script>

<style scoped>
.detail-payload {
  margin-top: 16px;
}
.detail-payload-title {
  font-weight: 600;
  margin-bottom: 8px;
}
.detail-payload-body {
  margin: 0;
  padding: 12px;
  max-height: 360px;
  overflow: auto;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
