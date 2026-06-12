<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch">
      <el-form-item label="线路" prop="appId">
        <el-select v-model="queryParams.appId" placeholder="请选择线路" style="width: 200px" @change="handleLineChange">
          <el-option
            v-for="item in lineAppList"
            :key="item.appId"
            :label="item.appName"
            :value="item.appId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="应用组名称" prop="groupName">
        <el-input
          v-model="queryParams.groupName"
          placeholder="请输入应用组名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['yianlian:serviceGroup:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="el-icon-sort" size="mini" @click="toggleExpandAll">展开/折叠</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table
    v-if="refreshTable"
      v-loading="loading"
      :data="serviceGroupList"
      row-key="id"
      :default-expand-all="isExpandAll"
      :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
    >
      <el-table-column prop="groupName" label="应用组名称" width="260"></el-table-column>
      <el-table-column prop="appName" label="线路" width="160"></el-table-column>
      <el-table-column prop="description" label="描述" :show-overflow-tooltip="true"></el-table-column>
      <el-table-column prop="orderNum" label="排序" width="100"></el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['yianlian:serviceGroup:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-plus" @click="handleAdd(scope.row)" v-hasPermi="['yianlian:serviceGroup:add']">新增</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['yianlian:serviceGroup:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加或修改对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="上级应用组" prop="parentId">
              <treeselect v-model="form.parentId" :options="groupOptions" :normalizer="normalizer" :placeholder="parentPlaceholder" @input="handleParentChange" />
            </el-form-item>
          </el-col>
      </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="线路" prop="appId">
              <el-input :value="currentLineName" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="应用组名称" prop="groupName">
              <el-input v-model="form.groupName" placeholder="请输入应用组名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="显示排序" prop="orderNum">
         <el-input-number v-model="form.orderNum" controls-position="right" :min="0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="描述" prop="description">
              <el-input v-model="form.description" type="textarea" placeholder="请输入描述" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listServiceGroup, getServiceGroup, addServiceGroup, updateServiceGroup, delServiceGroup } from "@/api/vpn/serviceGroup"
import { listLineApp } from "@/api/vpn/line"
import Treeselect from "@riophae/vue-treeselect"
import "@riophae/vue-treeselect/dist/vue-treeselect.css"

export default {
  name: "ServiceGroup",
  components: { Treeselect },
  data() {
    return {
      loading: true,
      showSearch: true,
      serviceGroupList: [],
      serviceGroupAllList: [],
      lineAppList: [],
      currentLineName: "",
      parentPlaceholder: "根应用组",
      groupOptions: [],
      title: "",
      open: false,
      isExpandAll: true,
      refreshTable: true,
      queryParams: {
        appId: undefined,
        groupName: undefined
      },
      form: {},
      rules: {
        parentId: [{ required: true, message: "上级应用组不能为空", trigger: "change" }],
        groupName: [{ required: true, message: "应用组名称不能为空", trigger: "blur" }]
      }
    }
  },
  created() {
    this.loadLineAppList()
  },
  methods: {
    /** 加载线路列表，默认选中第一条 */
    loadLineAppList() {
      listLineApp({ pageSize: 1000, pageNum: 1 }).then(response => {
        this.lineAppList = response.rows || []
        if (this.lineAppList.length > 0) {
          this.queryParams.appId = this.lineAppList[0].appId
          this.currentLineName = this.lineAppList[0].appName
          this.getList()
        } else {
          this.serviceGroupList = []
          this.loading = false
        }
      })
    },
    handleLineChange() {
      const line = this.lineAppList.find(item => item.appId === this.queryParams.appId)
      this.currentLineName = line ? line.appName : ""
      this.getList()
    },
    getList() {
      if (!this.queryParams.appId) {
        this.serviceGroupList = []
        this.loading = false
        return
      }
      this.loading = true
      listServiceGroup(this.queryParams).then(response => {
        this.serviceGroupAllList = response.data
        this.serviceGroupList = this.handleTree(response.data, "id")
        this.loading = false
      })
    },
    /** 判断同线路下是否已存在根应用组 */
    hasRootGroup(data, excludeId) {
      return (data || []).some(item =>
        (item.parentId == null || item.parentId === 0) && item.id !== excludeId
      )
    },
    /** 构建上级应用组下拉选项（无根组时包含虚拟根节点） */
    buildParentGroupOptions(data, excludeId) {
      const tree = this.handleTree(data, "id")
      if (this.hasRootGroup(data, excludeId)) {
        return tree
      }
      return [{ id: 0, groupName: "根应用组", children: tree }]
    },
    updateParentPlaceholder(data, excludeId) {
      this.parentPlaceholder = this.hasRootGroup(data, excludeId) ? "请选择上级应用组" : "根应用组"
    },
    normalizer(node) {
      if (node.children && !node.children.length) {
        delete node.children
      }
      return { id: node.id, label: node.groupName, children: node.children }
    },
    /** 选择上级应用组时同步线路（与列表当前线路一致） */
    handleParentChange() {
      this.form.appId = this.queryParams.appId
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = { id: undefined, parentId: 0, appId: undefined, groupName: undefined, orderNum: 0, description: undefined, status: "0" }
      this.resetForm("form")
    },
    handleQuery() { this.getList() },
    resetQuery() {
      const appId = this.queryParams.appId
      this.resetForm("queryForm")
      this.queryParams.appId = appId
      this.handleQuery()
    },
    handleAdd(row) {
      if (!this.queryParams.appId) {
        this.$modal.msgWarning("请先选择线路")
        return
      }
      this.reset()
      this.form.appId = this.queryParams.appId
      if (row != undefined && row.id) {
        this.form.parentId = row.id
      }
      this.open = true
      this.title = "新增应用组"
      listServiceGroup({ appId: this.queryParams.appId }).then(response => {
        this.serviceGroupAllList = response.data
        this.groupOptions = this.buildParentGroupOptions(response.data)
        this.updateParentPlaceholder(response.data)
        if (!(row != undefined && row.id)) {
          if (this.hasRootGroup(response.data)) {
            this.form.parentId = undefined
          } else {
            this.form.parentId = 0
          }
        }
      })
    },
    toggleExpandAll() {
      this.refreshTable = false
      this.isExpandAll = !this.isExpandAll
      this.$nextTick(() => { this.refreshTable = true })
    },
    handleUpdate(row) {
      this.reset()
      getServiceGroup(row.id).then(response => {
        this.form = response.data
        if (this.form.parentId == null) {
          this.form.parentId = 0
        }
        this.open = true
        this.title = "修改应用组"
        const appId = this.form.appId || this.queryParams.appId
        listServiceGroup({ appId }).then(resp => {
          const filteredData = resp.data.filter(d => d.id !== row.id)
          this.serviceGroupAllList = resp.data
          this.groupOptions = this.buildParentGroupOptions(filteredData, row.id)
          this.updateParentPlaceholder(filteredData, row.id)
        })
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.form.appId = this.queryParams.appId
          if (this.form.id != undefined) {
            updateServiceGroup(this.form).then(() => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() })
          } else {
            addServiceGroup(this.form).then(() => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() })
          }
        }
      })
    },
    handleDelete(row) {
      this.$modal.confirm('是否确认删除应用组"' + row.groupName + '"？').then(() => {
        return delServiceGroup(row.id)
      }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {})
    }
  }
}
</script>