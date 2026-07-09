<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="线路名称" prop="appName">
        <el-input
          v-model="queryParams.appName"
          placeholder="请输入线路名称"
          clearable
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="线路ID" prop="appId">
        <el-input
          v-model="queryParams.appId"
          placeholder="请输入线路ID"
          clearable
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="线路状态" clearable>
          <el-option
            v-for="dict in dict.type.vpn_line_status"
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
          v-hasPermi="['yianlian:line:add']"
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
          v-hasPermi="['yianlian:line:edit']"
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
          v-hasPermi="['yianlian:line:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['yianlian:line:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="lineAppList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="线路名称" align="center" prop="appName" :show-overflow-tooltip="true" />
      <el-table-column label="线路ID" align="center" prop="appId" :show-overflow-tooltip="true" />
      <el-table-column label="线路密钥" align="center" prop="appSecret" :show-overflow-tooltip="true" />
      <el-table-column label="管理系统URL" align="center" prop="url" :show-overflow-tooltip="true" />
      <el-table-column label="同步代理" align="center" prop="proxyEnabled" width="90">
        <template slot-scope="scope">
          <span>{{ scope.row.proxyEnabled === '1' ? '是' : '否' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="服务器" align="center" prop="host" :show-overflow-tooltip="true" />
      <el-table-column label="服务器端口" align="center" prop="srvPort" />
      <el-table-column label="敲门端口" align="center" prop="spaPort" />
      <el-table-column label="预共享秘钥" align="center" prop="spaKey" :show-overflow-tooltip="true" />
      <el-table-column label="探测" align="center" prop="probeStatus" width="90">
        <template slot-scope="scope">
          <el-tooltip v-if="scope.row.probeStatus === '1' || scope.row.probeStatus === '2'"
                      :content="probeTooltip(scope.row)" placement="top">
            <el-tag v-if="scope.row.probeStatus === '1'" type="success" size="mini">成功</el-tag>
            <el-tag v-else-if="scope.row.probeStatus === '2'" type="danger" size="mini">失败</el-tag>
          </el-tooltip>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="线路状态" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.vpn_line_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
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
            v-hasPermi="['yianlian:line:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['yianlian:line:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改参数配置对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="900px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="线路名称" prop="appName">
              <el-input v-model="form.appName" placeholder="请输入线路名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="线路ID" prop="appId">
              <el-input v-model="form.appId" placeholder="请输入线路ID" :disabled="isEdit" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="密钥" prop="appSecret">
              <el-input v-model="form.appSecret" type="password" :placeholder="isEdit ? '留空则不修改' : '请输入密钥'" show-password />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="管理系统URL" prop="url">
              <el-input v-model="form.url" placeholder="易安联 OpenAPI 上游地址（VPN 隧道内可达）" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="24">
            <el-alert
              title="同步代理：开启后管理系统经 GenlotVPN 代理访问易安联；关闭则 ruoyi-yianlian 直连管理系统 URL。需在「VPN角色管理」中为该线路配置 sync_proxy 角色。"
              type="info"
              :closable="false"
              show-icon
              style="margin-bottom: 12px"
            />
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="启用同步代理" prop="proxyEnabled">
              <el-radio-group v-model="form.proxyEnabled">
                <el-radio label="0">否（直连）</el-radio>
                <el-radio label="1">是（经代理）</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16" v-if="form.proxyEnabled === '1'">
          <el-col :span="12">
            <el-form-item label="代理监听IP" prop="proxyHost">
              <el-input v-model="form.proxyHost" placeholder="留空则使用 Nacos 默认管理机 IP" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="代理监听端口" prop="proxyPort">
              <el-input v-model.number="form.proxyPort" type="number" placeholder="留空则使用 Nacos 默认端口" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="服务器" prop="host">
              <el-input v-model="form.host" placeholder="请输入服务器域名或IP" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预共享秘钥" prop="spaKey">
              <el-input v-model="form.spaKey" type="password" :placeholder="isEdit ? '留空则不修改' : '请输入预共享秘钥'" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="服务器端口" prop="srvPort">
              <el-input v-model.number="form.srvPort" type="number" placeholder="范围：1 - 65535" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="敲门端口" prop="spaPort">
              <el-input v-model.number="form.spaPort" type="number" placeholder="范围：1 - 65535" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio
                  v-for="dict in dict.type.vpn_line_status"
                  :key="dict.value"
                  :label="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" :rows="2" />
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
import { listLineApp, getLineApp, addLineApp, updateLineApp, delLineApp } from "@/api/vpn/line"

export default {
  name: "LineApp",
  dicts: ['vpn_line_status'],
  data() {
    return {
      // 遮罩层
      loading: true,
      isEdit: false,
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
      // 参数表格数据
      lineAppList: [],
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
        appName: undefined,
        appId: undefined,
        status: undefined
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        appName: [
          {required: true, message: "线路名称不能为空", trigger: "blur"}
        ],
        appId: [
          {required: true, message: "线路ID不能为空", trigger: "blur"}
        ],
        appSecret: [
          {validator: this.validateAppSecret, trigger: "blur"}
        ],
        url: [
          {required: true, message: "管理系统URL不能为空", trigger: "blur"}
        ],
        host: [
          {required: true, message: "服务器域名或IP不能为空", trigger: "blur"}
        ],
        srvPort: [
          {required: true, message: "服务器端口号不能为空", trigger: "blur"},
          {type: 'number', min: 1, max: 65535, message: "端口范围1-65535", trigger: "blur"}
        ],
        spaPort: [
          {required: true, message: "敲门端口不能为空", trigger: "blur"},
          {type: 'number', min: 1, max: 65535, message: "端口范围1-65535", trigger: "blur"}
        ],
        spaKey: [
          {validator: this.validateSpaKey, trigger: "blur"}
        ]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** appSecret校验：新增时必填，编辑时可选 */
    validateAppSecret(rule, value, callback) {
      if (!this.isEdit && !value) {
        callback(new Error('线路密钥不能为空'))
      } else {
        callback()
      }
    },
    /** spaKey校验：新增时必填，编辑时可选 */
    validateSpaKey(rule, value, callback) {
      if (!this.isEdit && !value) {
        callback(new Error('预共享秘钥不能为空'))
      } else {
        callback()
      }
    },
    /** 探测列 tooltip：探测时间与失败原因 */
    probeTooltip(row) {
      const parts = []
      if (row.probeTime) {
        parts.push('探测时间：' + this.parseTime(row.probeTime))
      }
      if (row.probeMsg) {
        parts.push('原因：' + row.probeMsg)
      }
      return parts.length ? parts.join('\n') : ''
    },
    /** 查询线路列表 */
    getList() {
      this.loading = true
      listLineApp(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
          this.lineAppList = response.rows
          this.total = response.total
          this.loading = false
        }
      )
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.isEdit = false
      this.reset()
    },
    // 表单重置
    reset() {
      this.form = {
        appName: undefined,
        appId: undefined,
        appSecret: undefined,
        url: undefined,
        proxyEnabled: '0',
        proxyHost: undefined,
        proxyPort: undefined,
        host: undefined,
        srvPort: undefined,
        spaPort: undefined,
        spaKey: undefined,
        status: undefined,
        remark: undefined
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
      this.handleQuery()
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.isEdit = false
      this.title = "添加线路"
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.appId)
      this.single = selection.length != 1
      this.multiple = !selection.length
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const appId = row.appId || this.ids
      getLineApp(appId).then(response => {
        this.form = response.data
        // 编辑时清空密钥字段，避免显示MD5密文
        this.form.appSecret = ''
        this.form.spaKey = ''
        this.open = true
        this.isEdit = true
        this.title = "修改线路"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.isEdit) {
            // 编辑时，如果密钥字段为空则从提交数据中移除（后端会保持原值）
            const submitData = {...this.form}
            if (!submitData.appSecret) {
              delete submitData.appSecret
            }
            if (!submitData.spaKey) {
              delete submitData.spaKey
            }
            updateLineApp(submitData).then(() => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addLineApp(this.form).then(() => {
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
      const appIds = row.appId || this.ids
      this.$modal.confirm('是否确认删除线路ID为"' + appIds + '"的数据项？').then(function () {
        return delLineApp(appIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {
      })
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('yianlian/line/app/export', {
        ...this.queryParams
      }, `lineApp_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
