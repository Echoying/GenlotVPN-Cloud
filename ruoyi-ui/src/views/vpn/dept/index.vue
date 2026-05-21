<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch">
      <el-form-item label="部门名称" prop="deptName">
        <el-input
          v-model="queryParams.deptName"
          placeholder="请输入部门名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="部门状态" clearable>
          <el-option
            v-for="dict in dict.type.sys_normal_disable"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
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
          v-hasPermi="['yianlian:dept:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-check"
          size="mini"
          @click="handleSaveSort"
          v-hasPermi="['yianlian:dept:edit']"
        >保存排序</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="el-icon-sort"
          size="mini"
          @click="toggleExpandAll"
        >展开/折叠</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table
      v-if="refreshTable"
      v-loading="loading"
      :data="deptList"
      row-key="deptId"
      :default-expand-all="isExpandAll"
      :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
    >
      <el-table-column prop="deptName" label="部门名称" width="260"></el-table-column>
      <el-table-column prop="orderNum" label="排序" width="200">
        <template slot-scope="scope">
          <el-input-number v-model="scope.row.orderNum" controls-position="right" :min="0" size="mini" style="width: 88px" />
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="200">
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
            v-hasPermi="['yianlian:dept:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-plus"
            @click="handleAdd(scope.row)"
            v-hasPermi="['yianlian:dept:add']"
          >新增</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-key"
            @click="handleAuth(scope.row)"
            v-hasPermi="['yianlian:dept:edit']"
          >授权</el-button>
          <el-button
            v-if="scope.row.parentId != 0"
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['yianlian:dept:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加或修改部门对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-row>
          <el-col :span="24" v-if="form.parentId !== 0">
            <el-form-item label="上级部门" prop="parentId">
              <treeselect v-model="form.parentId" :options="deptOptions" :normalizer="normalizer" placeholder="选择上级部门" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="部门名称" prop="deptName">
              <el-input v-model="form.deptName" placeholder="请输入部门名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示排序" prop="orderNum">
              <el-input-number v-model="form.orderNum" controls-position="right" :min="0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="负责人" prop="leader">
              <el-input v-model="form.leader" placeholder="请输入负责人" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入联系电话" maxlength="11" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门状态">
              <el-radio-group v-model="form.status">
                <el-radio
                  v-for="dict in dict.type.sys_normal_disable"
                  :key="dict.value"
                  :label="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 部门授权对话框 -->
    <el-dialog
      :title="'部门授权 - ' + authDeptName"
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
import { listDept, getDept, delDept, addDept, updateDept, updateDeptSort, listDeptExcludeChild } from "@/api/vpn/dept"
import { listByDeptId, batchSave, getServiceTree } from "@/api/vpn/deptauth"
import { listLineApp } from "@/api/vpn/line"
import { serviceGroupTreeselect } from "@/api/vpn/serviceGroup"
import Treeselect from "@riophae/vue-treeselect"
import "@riophae/vue-treeselect/dist/vue-treeselect.css"

export default {
  name: "VpnDept",
  dicts: ['sys_normal_disable'],
  components: { Treeselect },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 表格树数据
      deptList: [],
      // 部门树选项
      deptOptions: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 是否展开，默认全部展开
      isExpandAll: true,
      // 重新渲染表格状态
      refreshTable: true,
      // 记录原始排序，用于对比变更
      originalOrders: {},
      // 查询参数
      queryParams: {
        deptName: undefined,
        status: undefined
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        parentId: [
          { required: true, message: "上级部门不能为空", trigger: "blur" }
        ],
        deptName: [
          { required: true, message: "部门名称不能为空", trigger: "blur" }
        ],
        orderNum: [
          { required: true, message: "显示排序不能为空", trigger: "blur" }
        ],
        email: [
          {
            type: "email",
            message: "请输入正确的邮箱地址",
            trigger: ["blur", "change"]
          }
        ],
        phone: [
          {
            pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/,
            message: "请输入正确的手机号码",
            trigger: "blur"
          }
        ]
      },
      // ---- 授权相关 ----
      authDeptId: null,
      authDeptName: "",
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
    // ==================== 部门管理 ====================

    /** 查询部门列表 */
    getList() {
      this.loading = true
      listDept(this.queryParams).then(response => {
        this.deptList = this.handleTree(response.data, "deptId")
        this.recordOriginalOrders(this.deptList)
        this.loading = false
      })
    },

    /** 转换部门树数据结构（treeselect用） */
    normalizer(node) {
      if (node.children && !node.children.length) {
        delete node.children
      }
      return {
        id: node.deptId,
        label: node.deptName,
        children: node.children
      }
    },

    /** 搜索 */
    handleQuery() {
      this.getList()
    },

    /** 重置搜索 */
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },

    /** 展开/折叠表格树 */
    toggleExpandAll() {
      this.refreshTable = false
      this.isExpandAll = !this.isExpandAll
      this.$nextTick(() => {
        this.refreshTable = true
      })
    },

    /** 递归记录原始排序值 */
    recordOriginalOrders(list) {
      list.forEach(item => {
        this.originalOrders[item.deptId] = item.orderNum
        if (item.children && item.children.length) {
          this.recordOriginalOrders(item.children)
        }
      })
    },

    /** 保存排序 */
    handleSaveSort() {
      const changedDeptIds = []
      const changedOrderNums = []
      const collectChanged = (list) => {
        list.forEach(item => {
          if (String(this.originalOrders[item.deptId]) !== String(item.orderNum)) {
            changedDeptIds.push(item.deptId)
            changedOrderNums.push(item.orderNum)
          }
          if (item.children && item.children.length) {
            collectChanged(item.children)
          }
        })
      }
      collectChanged(this.deptList)
      if (changedDeptIds.length === 0) {
        this.$modal.msgWarning("未检测到排序修改")
        return
      }
      updateDeptSort({ deptIds: changedDeptIds.join(","), orderNums: changedOrderNums.join(",") }).then(() => {
        this.$modal.msgSuccess("排序保存成功")
        this.recordOriginalOrders(this.deptList)
      })
    },

    /** 新增按钮操作 */
    handleAdd(row) {
      this.reset()
      if (row != undefined) {
        this.form.parentId = row.deptId
      }
      this.open = true
      this.title = "添加VPN部门"
      listDept().then(response => {
        this.deptOptions = this.handleTree(response.data, "deptId")
      })
    },

    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      getDept(row.deptId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改VPN部门"
        listDeptExcludeChild(row.deptId).then(response => {
          this.deptOptions = this.handleTree(response.data, "deptId")
          if (this.deptOptions.length == 0) {
            const noResultsOptions = { deptId: this.form.parentId, deptName: this.form.parentName, children: [] }
            this.deptOptions.push(noResultsOptions)
          }
        })
      })
    },

    /** 提交新增/修改表单 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.deptId != undefined) {
            updateDept(this.form).then(() => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addDept(this.form).then(() => {
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
        deptId: undefined,
        parentId: undefined,
        deptName: undefined,
        orderNum: undefined,
        leader: undefined,
        phone: undefined,
        email: undefined,
        status: "0"
      }
      this.resetForm("form")
    },

    /** 删除按钮操作 */
    handleDelete(row) {
      this.$modal.confirm('是否确认删除名称为"' + row.deptName + '"的数据项？').then(function() {
        return delDept(row.deptId)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },

    // ==================== 部门授权 ====================

    /** 授权按钮操作：先加载线路列表，再回显授权数据 */
    handleAuth(row) {
      this.authDeptId = row.deptId
      this.authDeptName = row.deptName
      this.authGroups = []
      this.lineOptions = []

      listLineApp({ pageSize: 1000, pageNum: 1 }).then(response => {
        this.lineOptions = response.rows || []

        listByDeptId(row.deptId).then(response => {
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
      this.authDeptId = null
      this.authDeptName = ""
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

      batchSave({
        deptId: this.authDeptId,
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
