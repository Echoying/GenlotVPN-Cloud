<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
    <el-form-item label="线路" prop="appId">
        <el-select v-model="queryParams.appId" placeholder="请选择线路" clearable style="width: 180px" @change="handleQueryLineChange">
      <el-option v-for="item in lineAppList" :key="item.appId" :label="item.appName" :value="item.appId" />
     </el-select>
      </el-form-item>
      <el-form-item label="应用组" prop="serviceGroupId">
      <el-select v-model="queryParams.serviceGroupId" placeholder="请选择应用组" clearable style="width: 180px">
          <el-option v-for="item in serviceGroupOptions" :key="item.id" :label="item.label" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="应用名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入" clearable style="width: 160px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
      <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['yianlian:service:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['yianlian:service:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="serviceList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="应用名称" prop="name" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="线路" prop="appName" width="120" />
      <el-table-column label="应用组" prop="serviceGroupName" width="120" />
      <el-table-column label="类型" width="80">
        <template slot-scope="scope">{{ typeName(scope.row.type) }}</template>
      </el-table-column>
      <el-table-column label="地址" :show-overflow-tooltip="true">
        <template slot-scope="scope">{{ formatAddress(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="端口" width="80">
        <template slot-scope="scope">{{ scope.row.type === 'cs' ? '-' : scope.row.webPort }}</template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="160">
        <template slot-scope="scope"><span>{{ parseTime(scope.row.createTime) }}</span></template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center">
      <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['yianlian:service:edit']">修改</el-button>
      <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['yianlian:service:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 添加或修改对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="800px" append-to-body>
      <el-form ref="form" :model="form" :rules="formRules" label-width="120px">
        <el-row>
     <el-col :span="12">
     <el-form-item label="线路" prop="appId">
              <el-input :value="formLineName" disabled />
            </el-form-item>
        </el-col>
      <el-col :span="12">
            <el-form-item label="应用组" prop="serviceGroupId">
           <treeselect v-model="form.serviceGroupId" :options="groupTreeOptions" :normalizer="normalizer" placeholder="请选择应用组" :disabled="form.id != undefined" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="应用名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入应用名称" />
       </el-form-item>
      </el-col>
          <el-col :span="12">
            <el-form-item label="应用类型" prop="type">
        <el-select v-model="form.type" placeholder="请选择" @change="handleTypeChange">
     <el-option label="Web应用" value="web" />
         <el-option label="隧道应用" value="cs" />
         </el-select>
          </el-form-item>
          </el-col>
    </el-row>
        <el-row v-if="form.type === 'web'" key="row-web-addr">
      <el-col :span="12">
          <el-form-item label="应用地址" prop="url" key="url">
       <el-input v-model="form.url" placeholder="请输入应用地址" />
       </el-form-item>
          </el-col>
     <el-col :span="12">
     <el-form-item label="端口" prop="webPort" key="webPort">
          <el-input v-model="form.webPort" placeholder="请输入端口" maxlength="6" oninput="value=value.replace(/[^\d]/g,'')" />
       </el-form-item>
          </el-col>
    </el-row>
    <el-row key="row-browser">
          <el-col :span="12">
            <el-form-item label="浏览器类型" prop="browserType" key="browserType">
              <el-select v-model="form.browserType" placeholder="请选择">
                <el-option label="默认浏览器" value="0" />
        <el-option label="IE浏览器" value="1" />
        <el-option label="非IE浏览器" value="2" />
            <el-option label="国密浏览器" value="3" />
              </el-select>
        </el-form-item>
          </el-col>
     <el-col :span="12">
          <el-form-item label="安全等级" prop="creditLevelId">
            <el-select v-model="form.creditLevelId" placeholder="请选择">
             <el-option label="高" value="00000000001" />
           <el-option label="中" value="00000000000002" />
       <el-option label="低" value="00000000000003" />
           </el-select>
        </el-form-item>
          </el-col>
     </el-row>
      <el-row>
        <el-col :span="12">
            <el-form-item label="是否展示">
           <el-switch v-model="form.ifShow"></el-switch>
   </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="二次认证">
            <el-select v-model="form.secondAuthEnable">
          <el-option label="启用" value="0" />
                <el-option label="禁用" value="1" />
       </el-select>
          </el-form-item>
        </el-col>
      </el-row>

        <!-- CS服务器配置 -->
        <el-row v-if="form.type === 'cs'">
          <el-col :span="24">
            <el-form-item label="CS服务器配置">
            <el-button type="primary" size="small" @click="handleAddCsServer">添加服务器</el-button>
            </el-form-item>
          </el-col>
        </el-row>
        <div v-if="form.type === 'cs' && form.reqCsServerVos && form.reqCsServerVos.length > 0">
          <el-card v-for="(server, index) in form.reqCsServerVos" :key="index" style="margin-bottom: 10px;">
          <div slot="header" class="clearfix">
        <span>服务器 {{ index + 1 }}</span>
              <el-button style="float: right; padding: 3px 0" type="text" @click="handleRemoveCsServer(index)">删除</el-button>
            </div>
            <el-row :gutter="10">
          <el-col :span="12">
                <el-form-item label="协议类型" :prop="'reqCsServerVos.' + index + '.protocolType'" :rules="[{ required: true, message: '请选择协议类型', trigger: 'change' }]">
               <el-select v-model="server.protocolType" placeholder="请选择" @change="handleProtocolTypeChange(server)">
                    <el-option label="http/https" value="0" />
                <el-option label="TCP/UDP/ANY" value="1" />
              </el-select>
              </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="协议" :prop="'reqCsServerVos.' + index + '.protocol'" :rules="[{ required: true, message: '请选择协议', trigger: 'change' }]">
            <el-select v-model="server.protocol" placeholder="请选择" :disabled="server.protocolType === '0'" @change="handleProtocolChange(server)">
                    <el-option v-if="server.protocolType === '0'" label="HTTP/HTTPS" value="HTTP" />
               <el-option v-if="server.protocolType === '1'" label="TCP" value="TCP" />
                    <el-option v-if="server.protocolType === '1'" label="UDP" value="UDP" />
               <el-option v-if="server.protocolType === '1'" label="ANY" value="ANY" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
         <el-row :gutter="10">
              <el-col :span="12">
                <el-form-item label="IP地址" :prop="'reqCsServerVos.' + index + '.ip'" :rules="[{ required: true, message: 'IP地址不能为空', trigger: 'blur' }]">
           <el-input v-model="server.ip" placeholder="请输入IP地址" />
              </el-form-item>
           </el-col>
              <el-col :span="12">
                <el-form-item label="端口" :prop="'reqCsServerVos.' + index + '.port'" :rules="[{ required: true, message: '端口不能为空', trigger: 'blur' }]">
                  <el-input v-model="server.port" placeholder="请输入端口" :disabled="server.protocol === 'ANY'" />
          </el-form-item>
              </el-col>
            </el-row>
          </el-card>
        </div>

        <el-row>
        <el-col :span="24">
         <el-form-item label="描述">
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
import { listService, getService, addService, updateService, delService } from "@/api/vpn/service"
import { listLineApp } from "@/api/vpn/line"
import { serviceGroupTreeselect } from "@/api/vpn/serviceGroup"
import Treeselect from "@riophae/vue-treeselect"
import "@riophae/vue-treeselect/dist/vue-treeselect.css"

export default {
  name: "VpnServiceApp",
  components: { Treeselect },
  data() {
    return {
      loading: true, ids: [], single: true, multiple: true, showSearch: true,
      total: 0, serviceList: [], lineAppList: [], serviceGroupOptions: [],
      title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, appId: undefined, serviceGroupId: undefined, name: undefined },
      form: {},
    }
  },
  computed: {
    /** 表单中展示的线路名称（来自列表当前选中线路，不可修改） */
    formLineName() {
      const appId = this.form.appId
      if (!appId) return ""
      const line = this.lineAppList.find(item => item.appId === appId)
      return line ? line.appName : appId
    },
    /** 根据应用类型动态校验：Web 必填地址/端口，隧道应用不要求 */
    formRules() {
      const rules = {
        appId: [{ required: true, message: "请选择线路", trigger: "change" }],
        serviceGroupId: [{ required: true, message: "请选择应用组", trigger: "change" }],
        name: [{ required: true, message: "应用名称不能为空", trigger: "blur" }],
        type: [{ required: true, message: "请选择应用类型", trigger: "change" }],
        browserType: [{ required: true, message: "请选择浏览器类型", trigger: "change" }]
      }
      if (this.form.type === "web") {
        rules.url = [{ required: true, message: "应用地址不能为空", trigger: "blur" }]
        rules.webPort = [
          { required: true, message: "端口不能为空", trigger: "blur" },
          { pattern: /^\d{1,6}$/, message: "端口必须是1-6位数字", trigger: "blur" }
        ]
      }
      return rules
    },
    /** 根据选中线路过滤应用组树 */
    groupTreeOptions() {
      if (!this.form.appId) {
        return []
      }
      return this.serviceGroupOptions.filter(opt => opt.appId === this.form.appId)
    }
  },
  created() {
    this.loadLineAppList()
    this.getServiceGroupList()
  },
  methods: {
    /** 加载线路列表，默认选中第一条并查询 */
    loadLineAppList() {
      listLineApp({ pageSize: 1000, pageNum: 1 }).then(r => {
        this.lineAppList = r.rows || []
        if (this.lineAppList.length > 0) {
          this.queryParams.appId = this.lineAppList[0].appId
          this.getList()
        } else {
          this.serviceList = []
          this.total = 0
          this.loading = false
        }
      })
    },
    handleQueryLineChange() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    getList() {
      if (!this.queryParams.appId) {
        this.serviceList = []
        this.total = 0
        this.loading = false
        return
      }
      this.loading = true
      listService(this.queryParams).then(response => {
        this.serviceList = response.rows; this.total = response.total; this.loading = false
      })
    },
    getServiceGroupList() { serviceGroupTreeselect({}).then(r => { this.serviceGroupOptions = r.data }) },
    /** 应用类型名称 */
    typeName(type) {
      if (type === 'web') return 'Web应用'
      if (type === 'cs') return '隧道应用'
      return type
    },
    /** 列表地址显示：隧道应用显示各CS服务器的"地址:端口"，Web应用显示url */
    formatAddress(row) {
      if (row.type === 'cs') {
        if (!row.reqCsServerVos || row.reqCsServerVos.length === 0) {
          return ''
        }
        return row.reqCsServerVos
          .map(s => `${s.ip || ''}:${s.port || ''}`)
          .join('，')
      }
      return row.url
    },
    normalizer(node) {
    return { id: node.id, label: node.label, children: node.children }
    },
    handleTypeChange() {
      if (this.form.type === 'web') {
        // Web应用：清空CS服务器配置
        this.form.reqCsServerVos = []
      } else if (this.form.type === 'cs') {
        // 隧道应用：清空地址/端口，至少保留一组CS服务器
        this.form.url = undefined
        this.form.webPort = undefined
        if (!this.form.reqCsServerVos || this.form.reqCsServerVos.length === 0) {
          this.handleAddCsServer()
        }
      }
      this.$nextTick(() => {
        if (this.$refs.form) {
          this.$refs.form.clearValidate()
        }
      })
    },
    handleAddCsServer() {
      if (!this.form.reqCsServerVos) {
        this.form.reqCsServerVos = []
      }
      this.form.reqCsServerVos.push({
      protocol: '',
        protocolType: '',
        ip: '',
        port: ''
      })
    },
    handleRemoveCsServer(index) {
      if (this.form.type === 'cs' && this.form.reqCsServerVos.length <= 1) {
        this.$modal.msgWarning("隧道应用至少需要保留一组CS服务器")
        return
      }
      this.form.reqCsServerVos.splice(index, 1)
    },
    handleProtocolTypeChange(server) {
      // 协议类型改变时，重置协议和端口
      server.protocol = ''
      if (server.protocolType === '0') {
        server.protocol = 'HTTP'
      }
    },
    handleProtocolChange(server) {
      // 协议为ANY时，端口必须为ANY
      if (server.protocol === 'ANY') {
        server.port = 'ANY'
      } else if (server.port === 'ANY') {
        server.port = ''
      }
    },
    cancel() { this.open = false; this.reset() },
    reset() {
      this.form = {
        id: undefined, appId: undefined, serviceGroupId: undefined, name: undefined,
        type: "web", url: undefined, browserType: "0", webPort: undefined,
        creditLevelId: "00000000000002", icon: "/diy/default-house.svg",
        ifShow: true, secondAuthEnable: "1", ifSelfApply: true,
        ifSAlarmTip: false, ifCustomAlarmContent: false, customAlarmContent: undefined,
        createType: "3", description: undefined, reqCsServerVos: []
      }
      this.resetForm("form")
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() {
      const appId = this.queryParams.appId
      this.resetForm("queryForm")
      this.queryParams.appId = appId
      this.handleQuery()
    },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.id); this.single = selection.length != 1; this.multiple = !selection.length },
    handleAdd() {
      if (!this.queryParams.appId) {
        this.$modal.msgWarning("请先选择线路")
        return
      }
      this.reset()
      this.form.appId = this.queryParams.appId
      this.open = true
      this.title = "新增应用"
      this.$nextTick(() => {
        if (this.$refs.form) {
          this.$refs.form.clearValidate()
        }
      })
    },
    handleUpdate(row) {
      this.reset()
   getService(row.id || this.ids[0]).then(r => {
        this.form = r.data
        if (!this.form.reqCsServerVos) {
          this.form.reqCsServerVos = []
        }
        if (this.form.type === 'cs') {
          // 隧道应用：地址/端口不展示，至少保留一组CS服务器
          this.form.url = undefined
          this.form.webPort = undefined
          if (this.form.reqCsServerVos.length === 0) {
            this.handleAddCsServer()
          }
        }
        this.open = true
        this.title = "修改应用"
        this.$nextTick(() => {
          if (this.$refs.form) {
            this.$refs.form.clearValidate()
          }
        })
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (!valid) {
          return
        }
        if (this.form.type === 'cs') {
          // 隧道应用：至少一组CS服务器，地址/端口置空
          if (!this.form.reqCsServerVos || this.form.reqCsServerVos.length === 0) {
            this.$modal.msgWarning("隧道应用至少需要配置一组CS服务器")
            return
          }
          this.form.url = undefined
          this.form.webPort = undefined
        } else {
          // Web应用：不传CS服务器
          this.form.reqCsServerVos = []
        }
        if (this.form.id != undefined) {
          updateService(this.form).then(() => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() })
        } else {
          addService(this.form).then(() => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() })
        }
      })
    },
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('是否确认删除选中的应用？').then(() => delService(ids)).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {})
    }
  }
}
</script>
