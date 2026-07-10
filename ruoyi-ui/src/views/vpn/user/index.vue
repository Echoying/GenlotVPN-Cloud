<template>
  <div class="app-container tree-sidebar-manage-wrap">
    <tree-panel title="组织机构" :tree-data="deptOptions" search-placeholder="请输入部门名称" storage-key="dept-sidebar-width" :defaultExpandAll="true" @node-click="handleNodeClick" @refresh="getDeptTree" ref="deptTreeRef" />
    <div class="tree-sidebar-content">
      <div class="content-inner">
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
          <el-form-item label="线路" prop="appId">
            <el-select v-model="currentAppId" placeholder="请选择线路" style="width: 200px" @change="onQueryLineChange">
              <el-option
                v-for="line in lineOptions"
                :key="line.appId"
                :label="line.appName"
                :value="line.appId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="用户名称" prop="userName">
            <el-input v-model="queryParams.userName" placeholder="请输入用户名称" clearable style="width: 240px" @keyup.enter.native="handleQuery" />
          </el-form-item>
          <el-form-item label="手机号码" prop="phonenumber">
            <el-input v-model="queryParams.phonenumber" placeholder="请输入手机号码" clearable style="width: 240px" @keyup.enter.native="handleQuery" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="用户状态" clearable style="width: 240px">
              <el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="创建时间">
            <el-date-picker v-model="dateRange" style="width: 240px" value-format="yyyy-MM-dd" type="daterange" range-separator="-" start-placeholder="开始日期" end-placeholder="结束日期"></el-date-picker>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['yianlian:user:edit']">修改</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['yianlian:user:remove']">删除</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['yianlian:user:export']">导出</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="primary" plain icon="el-icon-upload2" size="mini" :disabled="!currentAppId" @click="handleSyncLocal" v-hasPermi="['yianlian:user:syncLocal']">同步</el-button>
          </el-col>
          <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
        </el-row>

        <el-table v-loading="loading" :data="userList" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column label="用户编号" align="center" key="userId" prop="userId" v-if="columns.userId.visible" />
          <el-table-column label="用户名称" align="center" key="userName" v-if="columns.userName.visible" :show-overflow-tooltip="true">
            <template slot-scope="scope">
              <span>{{ scope.row.userName }}</span>
            </template>
          </el-table-column>
          <el-table-column label="用户昵称" align="center" key="nickName" prop="nickName" v-if="columns.nickName.visible" :show-overflow-tooltip="true" />
          <el-table-column label="部门" align="center" key="deptName" prop="dept.deptName" v-if="columns.deptName.visible" :show-overflow-tooltip="true" />
          <el-table-column label="手机号码" align="center" key="phonenumber" prop="phonenumber" v-if="columns.phonenumber.visible" width="120" />
          <el-table-column label="状态" align="center" key="status" v-if="columns.status.visible">
            <template slot-scope="scope">
              <el-switch v-model="scope.row.status" active-value="0" inactive-value="1" @change="handleStatusChange(scope.row)"></el-switch>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" prop="createTime" v-if="columns.createTime.visible" width="160">
            <template slot-scope="scope">
              <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
            <template slot-scope="scope">
              <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['yianlian:user:edit']">修改</el-button>
              <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['yianlian:user:remove']">删除</el-button>
              <el-dropdown size="mini" @command="(command) => handleCommand(command, scope.row)" v-hasPermi="['yianlian:user:resetPwd', 'yianlian:user:edit']">
                <el-button size="mini" type="text" icon="el-icon-d-arrow-right">更多</el-button>
                <el-dropdown-menu slot="dropdown">
                  <el-dropdown-item command="handleResetPwd" icon="el-icon-key" v-hasPermi="['yianlian:user:resetPwd']">重置密码</el-dropdown-item>
                  <el-dropdown-item command="handleAuth" icon="el-icon-setting" v-hasPermi="['yianlian:user:edit']">授权</el-dropdown-item>
                </el-dropdown-menu>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
      </div>
    </div>

    <!-- 添加或修改用户配置对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户昵称" prop="nickName">
              <el-input v-model="form.nickName" placeholder="请输入用户昵称" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="归属部门" prop="deptId">
              <treeselect v-model="form.deptId" :options="enabledDeptOptions" :show-count="true" placeholder="请选择归属部门" />
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
        <el-row>
          <el-col :span="12">
            <el-form-item v-if="form.userId == undefined" label="用户名称" prop="userName">
              <el-input v-model="form.userName" placeholder="请输入用户名称" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.userId == undefined" label="用户密码" prop="password">
              <el-input v-model="form.password" placeholder="请输入用户密码" type="password" maxlength="20" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户性别">
              <el-select v-model="form.sex" placeholder="请选择性别">
                <el-option v-for="dict in dict.type.sys_user_sex" :key="dict.value" :label="dict.label" :value="dict.value"></el-option>
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
            <el-form-item label="角色">
              <el-select v-model="form.roleIds" multiple placeholder="请选择本线路角色">
                <el-option v-for="item in roleOptions" :key="item.roleId" :label="item.roleName" :value="item.roleId" :disabled="item.status == 1"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 用户导入对话框 -->
    <excel-import-dialog ref="importUserRef" title="用户导入" action="/yianlian/vpn/user/importData" template-action="/yianlian/vpn/user/importTemplate" template-file-name="user_template" update-support-label="是否更新已经存在的用户数据" @success="getList" />

    <!-- 用户授权对话框 -->
    <el-dialog
      :title="'用户授权 - ' + authUserName"
      :visible.sync="authOpen"
      width="900px"
      append-to-body
      @close="handleAuthClose"
    >
      <el-form>
        <div class="auth-container">
          <!-- 授权组列表 -->
          <div v-for="(group, index) in authGroups" :key="index" class="auth-group">
            <div class="auth-group-header">
              <span class="auth-group-title">用户授权配置</span>
            </div>
            <el-row :gutter="12">
              <!-- 第1列：线路（与列表当前线路一致，不可修改） -->
              <el-col :span="8">
                <el-form-item label="线路" label-width="50px">
                  <el-input :value="currentLineName" disabled />
                </el-form-item>
              </el-col>
              <!-- 第2列：应用组选择（多选树） -->
              <el-col :span="8">
                <el-form-item label="应用组" label-width="60px">
                  <div style="display:flex;align-items:center;gap:4px">
                    <treeselect
                      v-model="group.appGroupIds"
                      :options="group.appGroupOptions"
                      :multiple="true"
                      :flat="true"
                      :normalizer="normalizerGroup"
                      placeholder="请选择应用组"
                      :disabled="!group.lineId"
                      :loading="group.groupLoading"
                      :default-expand-level="group.groupExpanded ? Infinity : 0"
                      no-options-text="暂无数据"
                      no-children-text="暂无子节点"
                      style="flex:1"
                    />
                    <el-button
                      v-if="group.lineId"
                      size="mini"
                      type="text"
                      :icon="group.groupExpanded ? 'el-icon-arrow-up' : 'el-icon-arrow-down'"
                      :title="group.groupExpanded ? '折叠' : '展开全部'"
                      style="padding:2px 4px;font-size:12px"
                      @click="group.groupExpanded = !group.groupExpanded"
                    />
                  </div>
                </el-form-item>
              </el-col>
              <!-- 第3列：应用服务选择（多选树，只能选叶子应用） -->
              <el-col :span="8">
                <el-form-item label="应用服务" label-width="70px">
                  <div style="display:flex;align-items:center;gap:4px">
                    <treeselect
                      v-model="group.appIds"
                      :options="group.serviceTreeOptions"
                      :multiple="true"
                      :flat="true"
                      :normalizer="normalizerService"
                      placeholder="请选择应用服务"
                      :disabled="!group.lineId"
                      :loading="group.serviceLoading"
                      :default-expand-level="group.serviceExpanded ? Infinity : 0"
                      no-options-text="暂无数据"
                      no-children-text="暂无子节点"
                      style="flex:1"
                    />
                    <el-button
                      v-if="group.lineId"
                      size="mini"
                      type="text"
                      :icon="group.serviceExpanded ? 'el-icon-arrow-up' : 'el-icon-arrow-down'"
                      :title="group.serviceExpanded ? '折叠' : '展开全部'"
                      style="padding:2px 4px;font-size:12px"
                      @click="group.serviceExpanded = !group.serviceExpanded"
                    />
                  </div>
                </el-form-item>
              </el-col>
            </el-row>
          </div>
        </div>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitAuth" :loading="authSaving">确 定</el-button>
        <el-button @click="authOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="syncTitle" :visible.sync="syncOpen" width="860px" append-to-body>
      <el-alert
        :title="'当前线路：' + (currentLineName || currentAppId)"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
      />
      <el-alert
        v-if="!unsyncedUsers.length && !hasSyncedGroups"
        title="暂无可同步的本地用户，且本线路尚无已同步用户"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
      />
      <el-alert
        v-else-if="!unsyncedUsers.length"
        title="该线路已同步全部本地用户，下方仅展示各部门已同步用户"
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
      <el-table v-if="syncResults.length" :data="syncResults" size="small" style="margin-bottom: 12px" max-height="180">
        <el-table-column label="用户" min-width="120">
          <template slot-scope="scope">{{ scope.row.nickName }} ({{ scope.row.userName }})</template>
        </el-table-column>
        <el-table-column label="部门" prop="deptName" min-width="100" show-overflow-tooltip />
        <el-table-column label="结果" width="80" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.success ? 'success' : 'danger'" size="mini">{{ scope.row.success ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="说明" prop="message" min-width="200" show-overflow-tooltip />
      </el-table>
      <el-form ref="syncForm" :model="syncForm" label-width="0">
        <div v-for="(group, index) in syncGroups" :key="group.key" class="sync-group-card">
          <div class="sync-group-header">
            <span>部门分组 {{ index + 1 }}</span>
            <el-button v-if="syncGroups.length > 1" type="text" size="mini" @click="removeSyncGroup(index)">删除</el-button>
          </div>
          <el-form-item label="归属部门" label-width="90px">
            <treeselect
              v-model="group.deptId"
              :options="syncDeptOptions()"
              :show-count="true"
              placeholder="请选择本线路下的部门"
              @input="onSyncGroupDeptChange(group)"
            />
          </el-form-item>
          <el-form-item label="用户" label-width="90px">
            <el-select
              v-model="group.userIds"
              multiple
              filterable
              placeholder="请先选择部门，再选择用户"
              style="width: 100%"
              :disabled="!group.deptId"
              @change="val => onGroupUserChange(group, val)"
            >
              <el-option
                v-for="user in userOptionsForGroup(group)"
                :key="user.localUserId"
                :label="formatSyncUserOption(user)"
                :value="user.localUserId"
                :disabled="user.locked"
              />
            </el-select>
            <div v-if="group.deptId && getLockedUserIds(group).length" class="sync-user-hint">
              标注「已同步」的用户不可移除；仅新增选择的用户会提交同步
            </div>
          </el-form-item>
        </div>
      </el-form>
      <el-button
        type="text"
        icon="el-icon-plus"
        @click="addSyncGroup"
        :disabled="syncGroups.length >= maxSyncGroupCount"
      >添加分组</el-button>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="syncSubmitting" :disabled="!canSubmitSync" @click="submitSyncLocal">确 定</el-button>
        <el-button @click="syncOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listUser, getUser, delUser, addUser, updateUser, resetUserPwd, changeUserStatus, deptTreeSelect, getLineSyncContext, syncLocalUsersToLine } from "@/api/vpn/user"
import { listByUserId, batchSaveUserAuth, getServiceTree } from "@/api/vpn/userauth"
import { listLineApp } from "@/api/vpn/line"
import { serviceGroupTreeselect } from "@/api/vpn/serviceGroup"
import { getConfigKey } from "@/api/system/config"
import Treeselect from "@riophae/vue-treeselect"
import "@riophae/vue-treeselect/dist/vue-treeselect.css"
import TreePanel from "@/components/TreePanel"
import ExcelImportDialog from "@/components/ExcelImportDialog"

export default {
  name: "VpnUser",
  dicts: ['sys_normal_disable', 'sys_user_sex'],
  components: { Treeselect, TreePanel, ExcelImportDialog },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 用户表格数据
      userList: null,
      // 弹出层标题
      title: "",
      // 所有部门树选项
      deptOptions: undefined,
      // 过滤掉已禁用部门树选项
      enabledDeptOptions: undefined,
      // 是否显示弹出层
      open: false,
      // 默认密码
      initPassword: undefined,
      // 日期范围
      dateRange: [],
      // 角色选项
      roleOptions: [],
      // 表单参数
      form: {},
      currentAppId: null,
      currentLineName: '',
      lineOptions: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        appId: undefined,
        userName: undefined,
        phonenumber: undefined,
        status: undefined,
        deptId: undefined
      },
      // 列信息
      columns: {
        userId: { label: '用户编号', visible: true },
        userName: { label: '用户名称', visible: true },
        nickName: { label: '用户昵称', visible: true },
        deptName: { label: '部门', visible: true },
        phonenumber: { label: '手机号码', visible: true },
        status: { label: '状态', visible: true },
        createTime: { label: '创建时间', visible: true }
      },
      // 表单校验
      rules: {
        userName: [
          { required: true, message: "用户名称不能为空", trigger: "blur" },
          { min: 2, max: 20, message: '用户名称长度必须介于 2 和 20 之间', trigger: 'blur' }
        ],
        nickName: [
          { required: true, message: "用户昵称不能为空", trigger: "blur" }
        ],
        password: [
          { required: true, message: "用户密码不能为空", trigger: "blur" },
          { min: 5, max: 20, message: '用户密码长度必须介于 5 和 20 之间', trigger: 'blur' },
          { pattern: /^[^<>"'|\\]+$/, message: "不能包含非法字符：< > \" ' \\\ |", trigger: "blur" }
        ],
        email: [
          {
            type: "email",
            message: "请输入正确的邮箱地址",
            trigger: ["blur", "change"]
          }
        ],
        phonenumber: [
          {
            pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/,
            message: "请输入正确的手机号码",
            trigger: "blur"
          }
        ]
      },
      // ---- 授权相关 ----
      authUserId: null,
      authUserName: "",
      authOpen: false,
      authSaving: false,
      authGroups: [],
      syncOpen: false,
      syncTitle: '',
      syncForm: {},
      syncSubmitting: false,
      syncResults: [],
      unsyncedUsers: [],
      syncedGroupsCache: [],
      syncGroups: [],
      syncGroupKeySeq: 1
    }
  },
  computed: {
    hasSyncedGroups() {
      return this.syncedGroupsCache && this.syncedGroupsCache.length > 0
    },
    pendingUserCount() {
      return this.syncGroups.reduce((sum, g) => sum + this.getPendingUserIds(g).length, 0)
    },
    canSubmitSync() {
      return this.pendingUserCount > 0
    },
    maxSyncGroupCount() {
      return this.countDeptNodes(this.enabledDeptOptions)
    },
    syncResultSummary() {
      const ok = this.syncResults.filter(r => r.success).length
      const fail = this.syncResults.length - ok
      if (fail === 0) return `已全部同步成功（${ok} 人）`
      if (ok === 0) return `全部失败（${fail} 人）`
      return `部分成功：成功 ${ok} 人，失败 ${fail} 人`
    },
    syncResultAlertType() {
      const ok = this.syncResults.filter(r => r.success).length
      if (ok === this.syncResults.length) return 'success'
      if (ok === 0) return 'error'
      return 'warning'
    }
  },
  created() {
    this.loadLineOptions()
    this.getConfigKey("sys.user.initPassword").then(response => {
      this.initPassword = response.msg
    })
  },
  methods: {
    /** 加载线路列表，默认选中第一条 */
    loadLineOptions() {
      listLineApp({ pageSize: 1000, pageNum: 1 }).then(response => {
        this.lineOptions = response.rows || []
        if (this.lineOptions.length > 0) {
          this.currentAppId = this.lineOptions[0].appId
          this.currentLineName = this.lineOptions[0].appName
          this.queryParams.appId = this.currentAppId
          this.getDeptTree()
          this.getList()
        } else {
          this.userList = []
          this.total = 0
          this.loading = false
        }
      })
    },

    /** 切换列表线路 */
    onQueryLineChange() {
      const line = this.lineOptions.find(l => l.appId === this.currentAppId)
      this.currentLineName = line ? line.appName : ''
      this.queryParams.appId = this.currentAppId
      this.queryParams.pageNum = 1
      this.queryParams.deptId = undefined
      if (this.$refs.deptTreeRef) {
        this.$refs.deptTreeRef.setCurrentKey(null)
      }
      this.getDeptTree()
      this.getList()
    },

    /** 查询用户列表 */
    getList() {
      if (!this.currentAppId) {
        this.userList = []
        this.total = 0
        return
      }
      this.loading = true
      listUser(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
        this.userList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    /** 查询部门下拉树结构 */
    getDeptTree() {
      if (!this.currentAppId) {
        this.deptOptions = []
        this.enabledDeptOptions = []
        return
      }
      deptTreeSelect({ appId: this.currentAppId }).then(response => {
        this.deptOptions = response.data
        this.enabledDeptOptions = this.filterDisabledDept(JSON.parse(JSON.stringify(response.data)))
      })
    },
    // 过滤禁用的部门
    filterDisabledDept(deptList) {
      return deptList.filter(dept => {
        if (dept.disabled) {
          return false
        }
        if (dept.children && dept.children.length) {
          dept.children = this.filterDisabledDept(dept.children)
        }
        return true
      })
    },
    // 节点单击事件
    handleNodeClick(data) {
      this.queryParams.deptId = data.id
      this.handleQuery()
    },
    // 用户状态修改
    handleStatusChange(row) {
      let text = row.status === "0" ? "启用" : "停用"
      this.$modal.confirm('确认要"' + text + '""' + row.userName + '"用户吗？').then(function() {
        return changeUserStatus(row.userId, row.status, row.appId || this.currentAppId)
      }).then(() => {
        this.$modal.msgSuccess(text + "成功")
      }).catch(function() {
        row.status = row.status === "0" ? "1" : "0"
      })
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    // 表单重置
    reset() {
      this.form = {
        userId: undefined,
        deptId: undefined,
        userName: undefined,
        nickName: undefined,
        password: undefined,
        phonenumber: undefined,
        email: undefined,
        sex: undefined,
        status: "0",
        remark: undefined,
        roleIds: []
      }
      this.resetForm("form")
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.dateRange = []
      this.resetForm("queryForm")
      this.queryParams.deptId = undefined
      this.$refs.deptTreeRef.setCurrentKey(null)
      this.handleQuery()
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.userId)
      this.single = selection.length != 1
      this.multiple = !selection.length
    },
    // 更多操作触发
    handleCommand(command, row) {
      switch (command) {
        case "handleResetPwd":
          this.handleResetPwd(row)
          break
        case "handleAuth":
          this.handleAuth(row)
          break
        default:
          break
      }
    },
    /** 新增按钮操作 */
    handleAdd() {
      if (!this.currentAppId) {
        this.$modal.msgWarning('请先选择线路')
        return
      }
      this.reset()
      getUser(undefined, this.currentAppId).then(response => {
        this.roleOptions = response.roles
        this.open = true
        this.title = "添加用户"
        this.form.appId = this.currentAppId
        this.form.password = this.initPassword
        this.$set(this.form, "roleIds", [])
      })
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const userId = row.userId || this.ids
      getUser(userId, this.currentAppId).then(response => {
        this.form = response.data
        this.form.appId = this.currentAppId
        this.roleOptions = response.roles
        this.$set(this.form, "roleIds", response.roleIds || [])
        this.open = true
        this.title = "修改用户"
        this.form.password = ""
      })
    },
    /** 重置密码按钮操作 */
    handleResetPwd(row) {
      this.$prompt('请输入"' + row.userName + '"的新密码', "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        closeOnClickModal: false,
        inputPattern: /^.{5,20}$/,
        inputErrorMessage: "用户密码长度必须介于 5 和 20 之间",
        inputValidator: (value) => {
          if (/<|>|"|'|\||\\/.test(value)) {
            return "不能包含非法字符：< > \" ' \\\ |"
          }
        },
      }).then(({ value }) => {
        resetUserPwd(row.userId, value, row.appId || this.currentAppId).then(() => {
          this.$modal.msgSuccess("修改成功，新密码是：" + value)
        })
      }).catch(() => {})
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          const roleIds = (this.form.roleIds || []).map(id => Number(id))
          const basePayload = {
            userName: this.form.userName,
            deptId: this.form.deptId,
            nickName: this.form.nickName,
            phonenumber: this.form.phonenumber,
            email: this.form.email,
            sex: this.form.sex,
            status: this.form.status,
            remark: this.form.remark,
            appId: this.currentAppId,
            roleIds
          }
          if (this.form.userId != undefined) {
            updateUser({ ...basePayload, userId: this.form.userId }).then(() => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addUser({ ...basePayload, password: this.form.password }).then(() => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const userIds = row.userId || this.ids
      this.$modal.confirm('是否确认删除用户编号为"' + userIds + '"的数据项？').then(function() {
        return delUser(userIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/user/export', {
        ...this.queryParams
      }, `user_${new Date().getTime()}.xlsx`)
    },
    /** 导入按钮操作 */
    handleImport() {
      this.$refs.importUserRef.open()
    },

    // ==================== 用户授权 ====================

    /** 授权按钮操作：线路固定为列表当前线路 */
    handleAuth(row) {
      if (!this.currentAppId) {
        this.$modal.msgWarning('请先选择线路')
        return
      }
      this.authUserId = row.userId
      this.authUserName = row.userName
      this.authGroups = []

      listByUserId(row.userId, this.currentAppId).then(response => {
        const list = response.data || []
        const group = this.createEmptyGroup()
        group.lineId = this.currentAppId
        if (list.length > 0) {
          const auth = list[0]
          group.appGroupIds = auth.appGroupIds
            ? auth.appGroupIds.split(",").filter(Boolean).map(id => Number(id))
            : []
          group.appIds = auth.appIds
            ? auth.appIds.split(",").filter(Boolean).map(id => 'service_' + id)
            : []
        }
        this.authGroups = [group]
        this.loadGroupOptions(group, this.currentAppId).then(() => {
          this.authOpen = true
        })
      }).catch(() => {
        this.$modal.msgError('加载授权数据失败')
      })
    },

    /** 关闭授权弹窗时清理 */
    handleAuthClose() {
      this.authGroups = []
      this.authUserId = null
      this.authUserName = ""
    },

    /** 创建空授权组对象 */
    createEmptyGroup() {
      return {
        lineId: null,
        appGroupIds: [],
        appIds: [],
        appGroupOptions: [],
        serviceTreeOptions: [],
        groupLoading: false,
        serviceLoading: false,
        groupExpanded: false,
        serviceExpanded: false
      }
    },

    /** 添加授权组 */
    addAuthGroup() {
      this.authGroups.push(this.createEmptyGroup())
    },

    /** 移除授权组 */
    removeAuthGroup(index) {
      this.authGroups.splice(index, 1)
    },

    /** 线路变更时重新加载应用组和服务树 */
    handleLineChange(lineId, index) {
      const group = this.authGroups[index]
      group.appGroupIds = []
      group.appIds = []
      group.appGroupOptions = []
      group.serviceTreeOptions = []
      if (lineId) {
        this.loadGroupOptions(group, lineId)
      }
    },

    /** 加载指定线路的应用组树和服务树 */
    loadGroupOptions(group, lineId) {
      group.groupLoading = true
      group.serviceLoading = true

      const p1 = serviceGroupTreeselect({ appId: lineId }).then(response => {
        group.appGroupOptions = response.data || []
      }).catch(() => {
        group.appGroupOptions = []
      }).finally(() => {
        group.groupLoading = false
      })

      const p2 = getServiceTree(lineId).then(response => {
        group.serviceTreeOptions = response.data || []
      }).catch(() => {
        group.serviceTreeOptions = []
      }).finally(() => {
        group.serviceLoading = false
      })

      return Promise.all([p1, p2])
    },

    /** 判断线路是否已被其他授权组占用 */
    isLineUsed(lineId, currentIndex) {
      return this.authGroups.some((g, i) => i !== currentIndex && g.lineId === lineId)
    },

    /** 应用组 treeselect normalizer */
    normalizerGroup(node) {
      return {
        id: node.id,
        label: node.label,
        children: node.children && node.children.length ? node.children : undefined
      }
    },

    /** 服务树 treeselect normalizer（应用组节点禁止选择） */
    normalizerService(node) {
      return {
        id: node.id,
        label: node.label,
        children: node.children && node.children.length ? node.children : undefined,
        isDisabled: node.type === 'group'
      }
    },

    /** 递归提取叶子节点ID */
    getLeafIds(options, selectedIds) {
      const leafIds = []
      const collectLeaf = (nodes) => {
        nodes.forEach(node => {
          if (!node.children || node.children.length === 0) {
            if (selectedIds.includes(node.id)) {
              leafIds.push(node.id)
            }
          } else {
            if (selectedIds.includes(node.id)) {
              const childLeafs = []
              const findLeafs = (n) => {
                if (!n.children || n.children.length === 0) {
                  childLeafs.push(n.id)
                } else {
                  n.children.forEach(c => findLeafs(c))
                }
              }
              findLeafs(node)
              if (childLeafs.length === 0) {
                leafIds.push(node.id)
              } else {
                childLeafs.forEach(id => leafIds.push(id))
              }
            } else {
              collectLeaf(node.children)
            }
          }
        })
      }
      collectLeaf(options)
      return [...new Set(leafIds)]
    },

    /** 提交授权保存 */
    submitAuth() {
      if (!this.currentAppId) {
        this.$modal.msgWarning('请先选择线路')
        return
      }

      this.authSaving = true
      const authList = this.authGroups.map(group => {
        let finalGroupIds = []
        if (group.appGroupIds && group.appGroupIds.length > 0) {
          finalGroupIds = this.getLeafIds(group.appGroupOptions, group.appGroupIds)
          if (finalGroupIds.length === 0) {
            finalGroupIds = group.appGroupIds
          }
        }
        const cleanGroupIds = finalGroupIds.map(id => String(id).replace('group_', ''))

        let finalAppIds = []
        if (group.appIds && group.appIds.length > 0) {
          finalAppIds = group.appIds.map(id => String(id).replace('service_', ''))
        }

        return {
          lineId: this.currentAppId,
          appGroupIds: cleanGroupIds.join(","),
          appIds: finalAppIds.join(",")
        }
      })

      batchSaveUserAuth({
        userId: this.authUserId,
        lineId: this.currentAppId,
        authList: authList
      }).then(() => {
        this.$modal.msgSuccess("授权保存成功")
        this.authOpen = false
      }).catch(() => {
        this.$modal.msgError("授权保存失败")
      }).finally(() => {
        this.authSaving = false
      })
    },
    handleSyncLocal() {
      if (!this.currentAppId) {
        this.$modal.msgWarning('请先选择线路')
        return
      }
      this.syncTitle = '同步本地用户 - ' + (this.currentLineName || this.currentAppId)
      this.syncResults = []
      getLineSyncContext(this.currentAppId).then(res => {
        const data = res.data || {}
        this.unsyncedUsers = data.unsyncedUsers || []
        this.syncedGroupsCache = data.syncedGroups || []
        this.syncGroups = [this.createEmptySyncGroup()]
        this.syncOpen = true
      })
    },
    createEmptySyncGroup() {
      return {
        key: this.syncGroupKeySeq++,
        deptId: undefined,
        userIds: []
      }
    },
    getSyncedUsersForDept(deptId) {
      if (!deptId) return []
      const matched = (this.syncedGroupsCache || []).find(g => g.deptId === deptId)
      return matched && matched.users ? matched.users : []
    },
    getLockedUserIds(group) {
      return this.getSyncedUsersForDept(group.deptId).map(u => u.localUserId).filter(Boolean)
    },
    getPendingUserIds(group) {
      const locked = this.getLockedUserIds(group)
      return (group.userIds || []).filter(id => !locked.includes(id))
    },
    formatSyncUserOption(user) {
      const name = (user.nickName || '') + ' (' + (user.userName || '') + ')'
      return user.locked ? name + ' [已同步]' : name
    },
    userOptionsForGroup(group) {
      if (!group.deptId) return []
      const lockedUsers = this.getSyncedUsersForDept(group.deptId).map(u => ({
        localUserId: u.localUserId,
        userName: u.userName,
        nickName: u.nickName,
        locked: true
      }))
      const takenPending = new Set()
      this.syncGroups.forEach(g => {
        if (g === group) return
        this.getPendingUserIds(g).forEach(id => takenPending.add(id))
      })
      const pendingUsers = (this.unsyncedUsers || [])
        .filter(u => !takenPending.has(u.localUserId))
        .map(u => ({ ...u, locked: false }))
      const merged = new Map()
      lockedUsers.forEach(u => merged.set(u.localUserId, u))
      pendingUsers.forEach(u => {
        if (!merged.has(u.localUserId)) merged.set(u.localUserId, u)
      })
      return Array.from(merged.values())
    },
    addSyncGroup() {
      this.syncGroups.push(this.createEmptySyncGroup())
    },
    removeSyncGroup(index) {
      if (this.syncGroups.length <= 1) return
      this.syncGroups.splice(index, 1)
    },
    onSyncGroupDeptChange(group) {
      if (group.deptId) {
        const duplicated = this.syncGroups.some(g => g !== group && g.deptId === group.deptId)
        if (duplicated) {
          this.$modal.msgError('该部门已在其他分组中选择')
          this.$nextTick(() => {
            group.deptId = undefined
            group.userIds = []
          })
          return
        }
      }
      group.userIds = [...this.getLockedUserIds(group)]
    },
    onGroupUserChange(group, val) {
      const locked = this.getLockedUserIds(group)
      const pending = (val || []).filter(id => !locked.includes(id))
      group.userIds = [...locked, ...pending]
    },
    syncDeptOptions() {
      // 不在树里禁用已选部门：父节点 isDisabled 会导致 treeselect 无法展开选子部门
      return JSON.parse(JSON.stringify(this.enabledDeptOptions || []))
    },
    countDeptNodes(nodes) {
      if (!nodes || !nodes.length) return 0
      let count = 0
      nodes.forEach(node => {
        count += 1
        if (node.children && node.children.length) {
          count += this.countDeptNodes(node.children)
        }
      })
      return count
    },
    validateSyncLocalGroups() {
      const deptIds = []
      const pendingIds = []
      for (const group of this.syncGroups) {
        if (!group.deptId) {
          this.$modal.msgError('请为每个分组选择部门')
          return false
        }
        if (deptIds.includes(group.deptId)) {
          this.$modal.msgError('部门不能重复选择')
          return false
        }
        deptIds.push(group.deptId)
        for (const localUserId of this.getPendingUserIds(group)) {
          if (pendingIds.includes(localUserId)) {
            this.$modal.msgError('用户不能重复选择')
            return false
          }
          pendingIds.push(localUserId)
        }
      }
      if (!pendingIds.length) {
        this.$modal.msgError('请至少选择一名待同步用户')
        return false
      }
      return true
    },
    refreshSyncContext() {
      return getLineSyncContext(this.currentAppId).then(res => {
        const data = res.data || {}
        this.unsyncedUsers = data.unsyncedUsers || []
        this.syncedGroupsCache = data.syncedGroups || []
      })
    },
    rebuildSyncGroupsAfterSubmit() {
      const nextGroups = this.syncGroups
        .filter(g => g.deptId)
        .map(g => {
          const locked = this.getLockedUserIds(g)
          const failedPending = this.getPendingUserIds(g).filter(id => {
            const result = this.syncResults.find(r => r.localUserId === id)
            return result && !result.success
          })
          return {
            ...g,
            userIds: [...locked, ...failedPending]
          }
        })
        .filter(g => this.getPendingUserIds(g).length > 0 || this.getLockedUserIds(g).length > 0)
      this.syncGroups = nextGroups.length ? nextGroups : [this.createEmptySyncGroup()]
    },
    submitSyncLocal() {
      if (!this.validateSyncLocalGroups()) return
      const payload = {
        appId: this.currentAppId,
        groups: this.syncGroups
          .filter(g => g.deptId && this.getPendingUserIds(g).length)
          .map(g => ({
            deptId: g.deptId,
            localUserIds: this.getPendingUserIds(g)
          }))
      }
      this.syncSubmitting = true
      syncLocalUsersToLine(payload).then(res => {
        const results = res.data || []
        this.syncResults = results
        const ok = results.filter(r => r.success).length
        const fail = results.length - ok
        return this.refreshSyncContext().then(() => ({ ok, fail }))
      }).then(({ ok, fail }) => {
        this.rebuildSyncGroupsAfterSubmit()
        if (fail === 0) {
          this.$modal.msgSuccess(`已成功同步 ${ok} 人`)
          this.syncOpen = false
          this.getList()
        } else if (ok === 0) {
          this.$modal.msgError('全部同步失败，请查看明细后重试')
        } else {
          this.$modal.msgWarning(`部分成功：${ok} 人成功，${fail} 人失败`)
          this.getList()
        }
      }).finally(() => {
        this.syncSubmitting = false
      })
    }
  }
}
</script>

<style scoped>
.auth-container {
  padding: 0 10px;
}
.auth-group {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 16px 16px 8px;
  margin-bottom: 12px;
  background: #fafafa;
}
.auth-group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.auth-group-title {
  font-weight: bold;
  color: #303133;
}
.auth-add-btn {
  text-align: center;
  padding: 8px 0;
}
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
  margin: 0 0 12px 90px;
}
.sync-section-label {
  font-size: 13px;
  color: #606266;
  margin-bottom: 6px;
}
.sync-user-hint {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin-top: 4px;
}
</style>
