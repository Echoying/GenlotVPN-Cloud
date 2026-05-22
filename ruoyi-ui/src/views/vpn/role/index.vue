<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch">
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
            icon="el-icon-key"
            @click="handleAuth(scope.row)"
            v-hasPermi="['yianlian:role:edit']"
          >授权</el-button>
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

    <!-- 添加或修改角色对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
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
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 角色授权对话框 -->
    <el-dialog
      :title="'角色授权 - ' + authRoleName"
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
              <span class="auth-group-title">授权组 {{ index + 1 }}</span>
              <el-button
                type="danger"
                icon="el-icon-delete"
                size="mini"
                circle
                @click="removeAuthGroup(index)"
              ></el-button>
            </div>
            <el-row :gutter="12">
              <!-- 第1列：线路选择 -->
              <el-col :span="8">
                <el-form-item label="线路" label-width="50px">
                  <el-select
                    v-model="group.lineId"
                    placeholder="请选择线路"
                    style="width: 100%"
                    @change="(val) => handleLineChange(val, index)"
                  >
                    <el-option
                      v-for="line in lineOptions"
                      :key="line.appId"
                      :label="line.appName"
                      :value="line.appId"
                      :disabled="isLineUsed(line.appId, index)"
                    />
                  </el-select>
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

          <!-- 添加授权组按钮 -->
          <div class="auth-add-btn">
            <el-button type="primary" plain icon="el-icon-plus" @click="addAuthGroup">添加授权组</el-button>
          </div>
        </div>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitAuth" :loading="authSaving">确 定</el-button>
        <el-button @click="authOpen = false">取 消</el-button>
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
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
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
      // ---- 授权相关 ----
      authRoleId: null,
      authRoleName: "",
      authOpen: false,
      authSaving: false,
      lineOptions: [],
      authGroups: []
    }
  },
  created() {
    this.getList()
  },
  methods: {
    // ==================== 角色管理 ====================

    /** 查询角色列表 */
    getList() {
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
      this.reset()
      this.open = true
      this.title = "添加VPN角色"
    },

    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.roleId || this.ids[0]
      getRole(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改VPN角色"
      })
    },

    /** 提交表单 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.roleId != undefined) {
            updateRole(this.form).then(() => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addRole(this.form).then(() => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },

    /** 取消按钮 */
    cancel() {
      this.open = false
      this.reset()
    },

    /** 表单重置 */
    reset() {
      this.form = {
        roleId: undefined,
        roleName: undefined,
        roleSort: 0,
        remark: undefined,
        status: "0"
      }
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

    // ==================== 角色授权 ====================

    /** 授权按钮操作：先加载线路列表，再回显授权数据 */
    handleAuth(row) {
      this.authRoleId = row.roleId
      this.authRoleName = row.roleName
      this.authGroups = []
      this.lineOptions = []

      listLineApp({ pageSize: 1000, pageNum: 1 }).then(response => {
        this.lineOptions = response.rows || []

        listByRoleId(row.roleId).then(response => {
          const list = response.data || []
          if (list.length === 0) {
            this.addAuthGroup()
            this.authOpen = true
          } else {
            const promises = list.map(auth => {
              const group = this.createEmptyGroup()
              group.lineId = auth.lineId
              // 应用组ID回显：DB存数字字符串，treeselect期望数字
              group.appGroupIds = auth.appGroupIds
                ? auth.appGroupIds.split(",").filter(Boolean).map(id => Number(id))
                : []
              // 应用ID回显：DB存数字字符串，treeselect期望 "service_xxx" 格式
              group.appIds = auth.appIds
                ? auth.appIds.split(",").filter(Boolean).map(id => 'service_' + id)
                : []
              this.authGroups.push(group)
              return this.loadGroupOptions(group, auth.lineId)
            })
            Promise.all(promises).then(() => {
              this.authOpen = true
            })
          }
        }).catch(() => {
          this.$modal.msgError('加载授权数据失败')
        })
      }).catch(() => {
        this.$modal.msgError('加载线路列表失败')
      })
    },

    /** 关闭授权弹窗时清理 */
    handleAuthClose() {
      this.authGroups = []
      this.authRoleId = null
      this.authRoleName = ""
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
      for (let i = 0; i < this.authGroups.length; i++) {
        if (!this.authGroups[i].lineId) {
          this.$modal.msgWarning(`授权组 ${i + 1} 未选择线路`)
          return
        }
      }

      this.authSaving = true
      const authList = this.authGroups.map(group => {
        // 应用组：只取最底层叶子节点，去掉 "group_" 前缀
        let finalGroupIds = []
        if (group.appGroupIds && group.appGroupIds.length > 0) {
          finalGroupIds = this.getLeafIds(group.appGroupOptions, group.appGroupIds)
          if (finalGroupIds.length === 0) {
            finalGroupIds = group.appGroupIds
          }
        }
        const cleanGroupIds = finalGroupIds.map(id => String(id).replace('group_', ''))

        // 应用：去掉 "service_" 前缀
        let finalAppIds = []
        if (group.appIds && group.appIds.length > 0) {
          finalAppIds = group.appIds.map(id => String(id).replace('service_', ''))
        }

        return {
          lineId: group.lineId,
          appGroupIds: cleanGroupIds.join(","),
          appIds: finalAppIds.join(",")
        }
      })

      batchSaveRoleAuth({
        roleId: this.authRoleId,
        authList: authList
      }).then(() => {
        this.$modal.msgSuccess("授权保存成功")
        this.authOpen = false
      }).catch(() => {
        this.$modal.msgError("授权保存失败")
      }).finally(() => {
        this.authSaving = false
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
