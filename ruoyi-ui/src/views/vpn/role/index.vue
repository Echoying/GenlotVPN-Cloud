<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch">
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
      <el-form-item label="角色名称" prop="roleName">
        <el-input
          v-model="queryParams.roleName"
          placeholder="请输入角色名称"
          clearable
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="角色状态"
          clearable
          style="width: 240px"
        >
          <el-option
            v-for="dict in dict.type.sys_normal_disable"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="创建时间">
        <el-date-picker
          v-model="dateRange"
          style="width: 240px"
          value-format="yyyy-MM-dd"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        ></el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['yianlian:role:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-connection"
          size="mini"
          @click="handleAddSyncProxyRole"
          v-hasPermi="['yianlian:role:add']"
        >同步代理角色</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['yianlian:role:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['yianlian:role:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="roleList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="角色编号" prop="roleId" width="120" />
      <el-table-column label="角色名称" prop="roleName" :show-overflow-tooltip="true" width="150" />
      <el-table-column label="权限字符" prop="roleKey" :show-overflow-tooltip="true" width="120" />
      <el-table-column label="角色描述" prop="remark" :show-overflow-tooltip="true" />
      <el-table-column label="状态" width="100">
        <template slot-scope="scope">
          <el-switch
            v-model="scope.row.status"
            active-value="0"
            inactive-value="1"
            @change="handleStatusChange(scope.row)"
          ></el-switch>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['yianlian:role:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['yianlian:role:remove']"
          >删除</el-button>
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

    <!-- 添加或修改角色对话框（含应用授权） -->
    <el-dialog :title="title" :visible.sync="open" width="900px" append-to-body @close="handleDialogClose">
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="权限字符" prop="roleKey">
          <el-input v-model="form.roleKey" placeholder="同步代理填 sync_proxy，其它角色可留空" />
        </el-form-item>
        <el-form-item label="角色描述" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入角色描述" />
        </el-form-item>
        <el-form-item label="角色状态">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in dict.type.sys_normal_disable"
              :key="dict.value"
              :label="dict.value"
            >{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-divider content-position="left">应用授权</el-divider>
        <div class="auth-container">
          <div v-for="(group, index) in authGroups" :key="index" class="auth-group">
            <el-row :gutter="12">
              <el-col :span="8">
                <el-form-item label="线路" label-width="50px">
                  <el-input :value="currentLineName" disabled />
                </el-form-item>
              </el-col>
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
        <el-button type="primary" @click="submitForm" :loading="formSubmitting">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listRole, getRole, delRole, addRole, updateRole, changeRoleStatus } from "@/api/vpn/role"
import { listByRoleId, batchSaveRoleAuth, getServiceTree } from "@/api/vpn/roleauth"
import { listLineApp } from "@/api/vpn/line"
import { serviceGroupTreeselect } from "@/api/vpn/serviceGroup"
import Treeselect from "@riophae/vue-treeselect"
import "@riophae/vue-treeselect/dist/vue-treeselect.css"

export default {
  name: "VpnRole",
  dicts: ['sys_normal_disable'],
  components: { Treeselect },
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
      // 角色表格数据
      roleList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 日期范围
      dateRange: [],
      currentAppId: null,
      currentLineName: '',
      lineOptions: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        appId: undefined,
        roleName: undefined,
        status: undefined
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        roleName: [
          { required: true, message: "角色名称不能为空", trigger: "blur" }
        ]
      },
      // ---- 应用授权（并入新增/修改对话框） ----
      formSubmitting: false,
      authGroups: []
    }
  },
  created() {
    this.loadLineOptions()
  },
  methods: {
    // ==================== 角色管理 ====================

    /** 加载线路列表，默认选中第一条 */
    loadLineOptions() {
      listLineApp({ pageSize: 1000, pageNum: 1 }).then(response => {
        this.lineOptions = response.rows || []
        if (this.lineOptions.length > 0) {
          this.currentAppId = this.lineOptions[0].appId
          this.currentLineName = this.lineOptions[0].appName
          this.queryParams.appId = this.currentAppId
          this.getList()
        } else {
          this.roleList = []
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
      this.getList()
    },

    /** 查询角色列表 */
    getList() {
      if (!this.currentAppId) {
        this.roleList = []
        this.total = 0
        return
      }
      this.loading = true
      listRole(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
        this.roleList = response.rows
        this.total = response.total
        this.loading = false
      })
    },

    /** 搜索 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },

    /** 重置搜索 */
    resetQuery() {
      this.dateRange = []
      this.resetForm("queryForm")
      this.handleQuery()
    },

    /** 多选框选中数据 */
  handleSelectionChange(selection) {
      this.ids = selection.map(item => item.roleId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },

    /** 角色状态变更 */
    handleStatusChange(row) {
      let text = row.status === "0" ? "启用" : "停用"
      this.$modal.confirm('确认要"' + text + '""' + row.roleName + '"角色吗？').then(() => {
        return changeRoleStatus(row.roleId, row.status)
      }).then(() => {
        this.$modal.msgSuccess(text + "成功")
      }).catch(() => {
        row.status = row.status === "0" ? "1" : "0"
      })
    },

    /** 新增按钮操作 */
    handleAdd() {
      if (!this.currentAppId) {
        this.$modal.msgWarning('请先选择线路')
        return
      }
      this.reset()
      this.form.appId = this.currentAppId
      this.initAuthGroupsForCurrentLine().then(() => {
        this.open = true
        this.title = "添加VPN角色"
      })
    },

    /** 新增同步代理管理员角色 */
    handleAddSyncProxyRole() {
      if (!this.currentAppId) {
        this.$modal.msgWarning('请先选择线路')
        return
      }
      this.reset()
      this.form.appId = this.currentAppId
      this.form.roleName = '同步代理管理员'
      this.form.roleKey = 'sync_proxy'
      this.form.remark = 'GenlotVPN 内置 OpenAPI 同步代理，每线路单独配置'
      this.initAuthGroupsForCurrentLine().then(() => {
        this.open = true
        this.title = '添加同步代理管理员角色'
      })
    },

    /** 修改按钮操作 */
    handleUpdate(row) {
      if (!this.currentAppId) {
        this.$modal.msgWarning('请先选择线路')
        return
      }
      this.reset()
      const id = row.roleId || this.ids[0]
      getRole(id).then(response => {
        this.form = response.data
        return this.loadRoleAuthGroups(id)
      }).then(() => {
        this.open = true
        this.title = "修改VPN角色"
      }).catch(() => {
        this.$modal.msgError('加载角色数据失败')
      })
    },

    /** 提交表单：保存角色 + 应用授权 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (!valid) {
          return
        }
        if (!this.currentAppId) {
          this.$modal.msgWarning('请先选择线路')
          return
        }
        this.formSubmitting = true
        const isUpdate = this.form.roleId != undefined
        const saveRolePromise = isUpdate
          ? updateRole(this.form).then(() => this.form.roleId)
          : addRole(this.form).then(res => res.data)

        saveRolePromise.then(roleId => {
          return batchSaveRoleAuth({
            roleId: roleId,
            lineId: this.currentAppId,
            authList: this.buildAuthList()
          })
        }).then(() => {
          this.$modal.msgSuccess(isUpdate ? "修改成功" : "新增成功")
          this.open = false
          this.getList()
        }).catch(() => {
          this.$modal.msgError("保存失败")
        }).finally(() => {
          this.formSubmitting = false
        })
      })
    },

    /** 取消按钮 */
    cancel() {
      this.open = false
      this.reset()
    },

    /** 对话框关闭时清理授权数据 */
    handleDialogClose() {
      this.authGroups = []
    },

    /** 表单重置 */
    reset() {
      this.form = {
        roleId: undefined,
        appId: this.currentAppId,
        roleName: undefined,
        roleKey: undefined,
        roleSort: 0,
        remark: undefined,
        status: "0"
      }
      this.authGroups = []
      this.resetForm("form")
    },

    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.roleId || this.ids
      this.$modal.confirm('是否确认删除角色编号为"' + ids + '"的数据项？').then(() => {
        return delRole(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },

    // ==================== 应用授权 ====================

    /** 初始化当前线路的授权组（新增或编辑回显） */
    initAuthGroupsForCurrentLine(authRecord) {
      const group = this.createEmptyGroup()
      group.lineId = this.currentAppId
      if (authRecord) {
        group.appGroupIds = authRecord.appGroupIds
          ? authRecord.appGroupIds.split(",").filter(Boolean).map(id => Number(id))
          : []
        group.appIds = authRecord.appIds
          ? authRecord.appIds.split(",").filter(Boolean).map(id => 'service_' + id)
          : []
      }
      this.authGroups = [group]
      return this.loadGroupOptions(group, this.currentAppId)
    },

    /** 加载角色已有授权 */
    loadRoleAuthGroups(roleId) {
      return listByRoleId(roleId, this.currentAppId).then(response => {
        const list = response.data || []
        const auth = list.length > 0 ? list[0] : null
        return this.initAuthGroupsForCurrentLine(auth)
      })
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

    /** 构造授权保存列表 */
    buildAuthList() {
      return this.authGroups.map(group => {
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
</style>
