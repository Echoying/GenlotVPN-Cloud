<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="用户名称" prop="userName">
        <el-input v-model="queryParams.userName" placeholder="请输入用户名称" clearable style="width: 200px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="手机号码" prop="phonenumber">
        <el-input v-model="queryParams.phonenumber" placeholder="请输入手机号码" clearable style="width: 200px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="用户状态" clearable style="width: 200px">
          <el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['vpn:localUser:add']">新增</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="userList">
      <el-table-column label="用户编号" align="center" prop="localUserId" width="90" />
      <el-table-column label="登录账号" align="center" prop="userName" :show-overflow-tooltip="true" />
      <el-table-column label="用户昵称" align="center" prop="nickName" :show-overflow-tooltip="true" />
      <el-table-column label="手机号码" align="center" prop="phonenumber" width="120" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.status" active-value="0" inactive-value="1" @change="handleStatusChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="300" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['vpn:localUser:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['vpn:localUser:remove']">删除</el-button>
          <el-button size="mini" type="text" icon="el-icon-upload2" @click="handleSync(scope.row)" v-hasPermi="['vpn:localUser:sync']">同步</el-button>
          <el-button size="mini" type="text" icon="el-icon-download" @click="handleOfflineLogin(scope.row)" v-hasPermi="['vpn:localUser:offlineLogin']">离线登录</el-button>
          <el-dropdown size="mini" @command="(command) => handleCommand(command, scope.row)" v-hasPermi="['vpn:localUser:resetPwd']">
            <el-button size="mini" type="text" icon="el-icon-d-arrow-right">更多</el-button>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="handleResetPwd" icon="el-icon-key" v-hasPermi="['vpn:localUser:resetPwd']">重置密码</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户昵称" prop="nickName">
              <el-input v-model="form.nickName" placeholder="请输入用户昵称" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="登录账号" prop="userName">
              <el-input v-model="form.userName" placeholder="请输入登录账号" maxlength="30" :disabled="form.localUserId != undefined" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="手机号码" prop="phonenumber">
              <el-input v-model="form.phonenumber" placeholder="请输入手机号码" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="form.localUserId == undefined">
          <el-col :span="12">
            <el-form-item label="用户密码" prop="password">
              <el-input v-model="form.password" placeholder="请输入用户密码" type="password" maxlength="20" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户性别">
              <el-select v-model="form.sex" placeholder="请选择">
                <el-option v-for="dict in dict.type.sys_user_sex" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="offlineTitle" :visible.sync="offlineOpen" width="560px" append-to-body>
      <el-alert
        v-if="!offlineLineOptions.length"
        title="暂无可导出线路，请先同步用户到线路并配置部门/角色/用户授权"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
      />
      <el-form ref="offlineForm" :model="offlineForm" :rules="offlineRules" label-width="90px">
        <el-form-item label="线路选择" prop="appIds">
          <el-checkbox-group v-model="offlineForm.appIds">
            <el-checkbox
              v-for="line in offlineLineOptions"
              :key="line.appId"
              :label="line.appId"
              style="display: block; margin-bottom: 8px;"
            >
              {{ line.appName }}
              <span v-if="line.host" style="color: #909399; margin-left: 6px;">
                ({{ line.host }}{{ line.srvPort ? (':' + line.srvPort) : '' }})
              </span>
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="有效时间" prop="validity">
          <el-select v-model="offlineForm.validity" placeholder="请选择有效时间" style="width: 100%">
            <el-option v-for="item in validityOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="offlineExporting" :disabled="!offlineLineOptions.length" @click="submitOfflineExport">导 出</el-button>
        <el-button @click="offlineOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="syncTitle" :visible.sync="syncOpen" width="720px" append-to-body>
      <el-alert
        v-if="allLinesSynced"
        title="该用户已同步到所有线路，如需修改请在线路用户管理中操作"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
      />
      <el-alert
        v-else-if="syncedLineUsers.length"
        title="以下线路已同步，不可再次同步；请在下方为未同步线路添加分组"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
      />
      <el-alert
        v-if="syncResults.length"
        :title="syncResultSummary"
        :type="syncResultAlertType"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
      />
      <el-table v-if="syncResults.length" :data="syncResults" size="small" style="margin-bottom: 12px" max-height="160">
        <el-table-column label="线路" prop="appName" min-width="120" />
        <el-table-column label="结果" width="80" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.success ? 'success' : 'danger'" size="mini">{{ scope.row.success ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="说明" prop="message" min-width="200" show-overflow-tooltip />
      </el-table>
      <div v-if="syncedLineUsers.length" class="sync-synced-section">
        <div class="sync-group-header">已同步线路</div>
        <el-table :data="syncedLineUsers" size="small" style="margin-bottom: 12px">
          <el-table-column label="线路" min-width="120" show-overflow-tooltip>
            <template slot-scope="scope">{{ formatLineUserAppName(scope.row) }}</template>
          </el-table-column>
          <el-table-column label="归属部门" min-width="120" show-overflow-tooltip>
            <template slot-scope="scope">{{ formatLineUserDept(scope.row) }}</template>
          </el-table-column>
          <el-table-column label="角色" min-width="160" show-overflow-tooltip>
            <template slot-scope="scope">{{ formatLineUserRoles(scope.row) }}</template>
          </el-table-column>
        </el-table>
      </div>
      <template v-if="!allLinesSynced">
        <el-form ref="syncForm" :model="syncForm" label-width="0">
          <div v-for="(group, index) in syncGroups" :key="group.key" class="sync-group-card">
            <div class="sync-group-header">
              <span>待同步分组 {{ index + 1 }}</span>
              <el-button v-if="syncGroups.length > 1" type="text" size="mini" @click="removeSyncGroup(index)">删除</el-button>
            </div>
            <el-form-item label="目标线路" label-width="90px">
              <el-select v-model="group.appId" placeholder="请选择线路" style="width: 100%" @change="onGroupLineChange(group)">
                <el-option
                  v-for="line in availableLinesForGroup(group)"
                  :key="line.appId"
                  :label="line.appName"
                  :value="line.appId"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="归属部门" label-width="90px">
              <treeselect v-model="group.deptId" :options="group.deptOptions" :show-count="true" placeholder="请选择归属部门" :disabled="!group.appId" />
            </el-form-item>
            <el-form-item label="角色" label-width="90px">
              <el-select v-model="group.roleIds" multiple placeholder="请选择角色" style="width: 100%" :disabled="!group.appId">
                <el-option v-for="role in group.roleOptions" :key="role.roleId" :label="role.roleName" :value="role.roleId" />
              </el-select>
            </el-form-item>
          </div>
        </el-form>
        <el-button type="text" icon="el-icon-plus" @click="addSyncGroup" :disabled="syncGroups.length >= unsyncedLineCount">添加分组</el-button>
      </template>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="syncSubmitting" :disabled="allLinesSynced || !syncGroups.length" @click="submitSync">确 定</el-button>
        <el-button @click="syncOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listLocalUser, getLocalUser, addLocalUser, updateLocalUser, delLocalUser, resetLocalUserPwd, changeLocalUserStatus, syncLocalUserToLine, listLineUsers, listOfflineLoginLines, exportOfflineLogin } from '@/api/vpn/localUser'
import { listLineApp } from '@/api/vpn/line'
import { listRole } from '@/api/vpn/role'
import { deptTreeSelect } from '@/api/vpn/user'
import { blobValidate } from '@/utils/ruoyi'
import { saveAs } from 'file-saver'
import Treeselect from '@riophae/vue-treeselect'
import '@riophae/vue-treeselect/dist/vue-treeselect.css'

export default {
  name: 'VpnLocalUser',
  dicts: ['sys_normal_disable', 'sys_user_sex'],
  components: { Treeselect },
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      userList: [],
      title: '',
      open: false,
      syncOpen: false,
      syncTitle: '',
      syncUser: null,
      offlineOpen: false,
      offlineTitle: '',
      offlineExporting: false,
      offlineUser: null,
      offlineLineOptions: [],
      validityOptions: [
        { value: 'H6', label: '6个小时' },
        { value: 'D1', label: '一天' },
        { value: 'D3', label: '三天' },
        { value: 'W1', label: '一个星期' },
        { value: 'M1', label: '一个月' },
        { value: 'Y1', label: '一年' }
      ],
      syncedAppIds: [],
      syncedLineUsers: [],
      lineOptions: [],
      syncGroups: [],
      syncGroupKeySeq: 1,
      syncResults: [],
      syncSubmitting: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userName: undefined,
        phonenumber: undefined,
        status: undefined
      },
      form: {},
      syncForm: {
        localUserId: undefined
      },
      rules: {
        userName: [{ required: true, message: '用户账号不能为空', trigger: 'blur' }],
        nickName: [{ required: true, message: '用户昵称不能为空', trigger: 'blur' }],
        password: [{ required: true, message: '用户密码不能为空', trigger: 'blur' }, { min: 5, max: 20, message: '长度在 5 到 20 个字符', trigger: 'blur' }],
        email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }],
        phonenumber: [{ pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: '请输入正确的手机号码', trigger: 'blur' }]
      },
      syncGroupRules: {
        appId: [{ required: true, message: '请选择线路', trigger: 'change' }],
        deptId: [{ required: true, message: '请选择部门', trigger: 'change' }]
      },
      offlineForm: {
        localUserId: undefined,
        appIds: [],
        validity: 'D1'
      },
      offlineRules: {
        appIds: [{ type: 'array', required: true, message: '请至少选择一条线路', trigger: 'change' }],
        validity: [{ required: true, message: '请选择有效时间', trigger: 'change' }]
      }
    }
  },
  computed: {
    allLinesSynced() {
      if (!this.lineOptions.length) return false
      return this.lineOptions.every(line => this.isLineSynced(line.appId))
    },
    unsyncedLineCount() {
      return this.lineOptions.filter(line => !this.isLineSynced(line.appId)).length
    },
    syncResultSummary() {
      const ok = this.syncResults.filter(r => r.success).length
      const fail = this.syncResults.length - ok
      if (fail === 0) return `已全部同步成功（${ok} 条线路）`
      if (ok === 0) return `全部失败（${fail} 条线路）`
      return `部分成功：成功 ${ok} 条，失败 ${fail} 条`
    },
    syncResultAlertType() {
      const ok = this.syncResults.filter(r => r.success).length
      if (ok === this.syncResults.length) return 'success'
      if (ok === 0) return 'error'
      return 'warning'
    }
  },
  created() {
    this.getList()
    this.loadLineOptions()
  },
  methods: {
    getList() {
      this.loading = true
      listLocalUser(this.queryParams).then(response => {
        this.userList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    loadLineOptions() {
      listLineApp({ status: '0' }).then(res => {
        this.lineOptions = res.rows || []
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = { status: '0', sex: '0' }
      this.resetForm('form')
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '添加本地用户'
    },
    handleUpdate(row) {
      this.reset()
      getLocalUser(row.localUserId).then(response => {
        this.form = response.data
        this.open = true
        this.title = '修改本地用户'
      })
    },
    handleCommand(command, row) {
      if (command === 'handleResetPwd') {
        this.$prompt('请输入"' + row.userName + '"的新密码', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          inputPattern: /^.{5,20}$/,
          inputErrorMessage: '用户密码长度必须介于 5 和 20 之间'
        }).then(({ value }) => {
          resetLocalUserPwd(row.localUserId, value).then(() => {
            this.$modal.msgSuccess('重置成功')
          })
        }).catch(() => {})
      }
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) return
        if (this.form.localUserId != undefined) {
          updateLocalUser(this.form).then(() => {
            this.$modal.msgSuccess('修改成功')
            this.open = false
            this.getList()
          })
        } else {
          addLocalUser(this.form).then(() => {
            this.$modal.msgSuccess('新增成功')
            this.open = false
            this.getList()
          })
        }
      })
    },
    handleDelete(row) {
      this.$modal.confirm('是否确认删除本地用户"' + row.userName + '"？').then(() => {
        return delLocalUser(row.localUserId)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    handleStatusChange(row) {
      const text = row.status === '0' ? '启用' : '停用'
      this.$modal.confirm('确认要"' + text + '""' + row.userName + '"用户吗？').then(() => {
        return changeLocalUserStatus(row.localUserId, row.status)
      }).then(() => {
        this.$modal.msgSuccess(text + '成功')
      }).catch(() => {
        row.status = row.status === '0' ? '1' : '0'
      })
    },
    isLineSynced(appId) {
      return this.syncedAppIds.includes(appId)
    },
    createEmptySyncGroup() {
      return {
        key: this.syncGroupKeySeq++,
        appId: undefined,
        deptId: undefined,
        roleIds: [],
        deptOptions: [],
        roleOptions: []
      }
    },
    formatLineUserAppName(lineUser) {
      const line = this.lineOptions.find(l => l.appId === lineUser.appId)
      return line ? line.appName : (lineUser.appId || '-')
    },
    formatLineUserDept(lineUser) {
      if (lineUser.dept && lineUser.dept.deptName) return lineUser.dept.deptName
      return lineUser.deptName || '-'
    },
    formatLineUserRoles(lineUser) {
      const names = (lineUser.roles || []).map(r => r.roleName).filter(Boolean)
      return names.length ? names.join('、') : '-'
    },
    addSyncGroup() {
      this.syncGroups.push(this.createEmptySyncGroup())
    },
    removeSyncGroup(index) {
      if (this.syncGroups.length <= 1) return
      this.syncGroups.splice(index, 1)
    },
    availableLinesForGroup(group) {
      const selected = this.syncGroups
        .filter(g => g !== group && g.appId)
        .map(g => g.appId)
      return this.lineOptions.filter(line =>
        !selected.includes(line.appId) && !this.isLineSynced(line.appId)
      )
    },
    handleSync(row) {
      this.syncUser = row
      this.syncTitle = '同步到线路 - ' + row.userName
      this.syncForm = { localUserId: row.localUserId }
      this.syncResults = []
      this.syncedLineUsers = []
      listLineUsers(row.localUserId).then(res => {
        const users = res.data || []
        this.syncedLineUsers = users
        this.syncedAppIds = users.map(u => u.appId).filter(Boolean)
        const hasUnsynced = this.lineOptions.some(line => !this.syncedAppIds.includes(line.appId))
        this.syncGroups = hasUnsynced ? [this.createEmptySyncGroup()] : []
        this.syncOpen = true
        this.$nextTick(() => {
          if (this.$refs.syncForm) this.$refs.syncForm.clearValidate()
        })
      })
    },
    onGroupLineChange(group) {
      group.deptId = undefined
      group.roleIds = []
      group.deptOptions = []
      group.roleOptions = []
      if (!group.appId) return
      deptTreeSelect({ appId: group.appId }).then(res => {
        group.deptOptions = res.data
      })
      listRole({ appId: group.appId, pageNum: 1, pageSize: 100 }).then(res => {
        group.roleOptions = res.rows || []
      })
    },
    validateSyncGroups() {
      if (!this.syncGroups.length) {
        this.$modal.msgError('请至少添加一个线路分组')
        return false
      }
      const appIds = []
      for (const group of this.syncGroups) {
        if (!group.appId) {
          this.$modal.msgError('请为每个分组选择线路')
          return false
        }
        if (this.isLineSynced(group.appId)) {
          this.$modal.msgError('已同步线路不可再次同步')
          return false
        }
        if (!group.deptId) {
          this.$modal.msgError('请为每个分组选择归属部门')
          return false
        }
        if (appIds.includes(group.appId)) {
          this.$modal.msgError('线路不能重复选择')
          return false
        }
        appIds.push(group.appId)
      }
      return true
    },
    refreshSyncedLineUsers() {
      return listLineUsers(this.syncForm.localUserId).then(res => {
        this.syncedLineUsers = res.data || []
        this.syncedAppIds = this.syncedLineUsers.map(u => u.appId).filter(Boolean)
      })
    },
    rebuildSyncGroupsAfterSubmit() {
      this.syncGroups = this.syncGroups.filter(g => g.appId && !this.isLineSynced(g.appId))
      if (!this.allLinesSynced && !this.syncGroups.length) {
        this.syncGroups = [this.createEmptySyncGroup()]
      }
    },
    submitSync() {
      if (!this.validateSyncGroups()) return
      const payload = {
        localUserId: this.syncForm.localUserId,
        lines: this.syncGroups.map(g => ({
          appId: g.appId,
          deptId: g.deptId,
          roleIds: g.roleIds || []
        }))
      }
      this.syncSubmitting = true
      syncLocalUserToLine(payload).then(res => {
        const results = res.data || []
        this.syncResults = results
        const ok = results.filter(r => r.success).length
        const fail = results.length - ok
        return this.refreshSyncedLineUsers().then(() => {
          this.rebuildSyncGroupsAfterSubmit()
          return { ok, fail, results }
        })
      }).then(({ ok, fail }) => {
        if (fail === 0) {
          this.$modal.msgSuccess(`已成功同步 ${ok} 条线路`)
          if (this.allLinesSynced) {
            this.syncOpen = false
          }
        } else if (ok === 0) {
          this.$modal.msgError('全部线路同步失败，请查看明细后重试')
        } else {
          this.$modal.msgWarning(`部分成功：${ok} 条成功，${fail} 条失败`)
        }
      }).finally(() => {
        this.syncSubmitting = false
      })
    },
    handleOfflineLogin(row) {
      this.offlineUser = row
      this.offlineTitle = '离线登录 - ' + row.userName
      this.offlineForm = {
        localUserId: row.localUserId,
        appIds: [],
        validity: 'D1'
      }
      this.offlineLineOptions = []
      listOfflineLoginLines(row.localUserId).then(res => {
        this.offlineLineOptions = res.data || []
        this.offlineOpen = true
        this.$nextTick(() => {
          if (this.$refs.offlineForm) {
            this.$refs.offlineForm.clearValidate()
          }
        })
      })
    },
    submitOfflineExport() {
      this.$refs['offlineForm'].validate(valid => {
        if (!valid) return
        this.offlineExporting = true
        exportOfflineLogin(this.offlineForm).then(async data => {
          const isBlob = blobValidate(data)
          if (!isBlob) {
            const resText = await data.text()
            const rspObj = JSON.parse(resText)
            this.$modal.msgError(rspObj.msg || '导出失败')
            return
          }
          const zipName = 'offline-login-' + this.offlineUser.userName + '.zip'
          saveAs(new Blob([data], { type: 'application/zip' }), zipName)
          this.$modal.msgSuccess('导出成功')
          this.offlineOpen = false
        }).catch(() => {
          this.$modal.msgError('导出失败')
        }).finally(() => {
          this.offlineExporting = false
        })
      })
    }
  }
}
</script>

<style scoped>
.sync-group-card {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 12px 12px 4px;
  margin-bottom: 12px;
  background: #fafafa;
}
.sync-group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-weight: 600;
  color: #303133;
}
.sync-synced-section {
  margin-bottom: 12px;
}
</style>
