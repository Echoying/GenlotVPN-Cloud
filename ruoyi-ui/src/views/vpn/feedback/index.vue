<template>
  <div class="app-container">
    <div class="mb8">问题反馈</div>
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="80px">
      <el-form-item label="账号" prop="userName">
        <el-input
          v-model="queryParams.userName"
          placeholder="请输入账号"
          clearable
          style="width: 240px;"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="请选择状态"
          clearable
          style="width: 240px"
        >
          <el-option
            v-for="dict in dict.type.vpn_feedback_status"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="分类" prop="category">
        <el-select
          v-model="queryParams.category"
          placeholder="请选择分类"
          clearable
          style="width: 240px"
        >
          <el-option
            v-for="dict in dict.type.vpn_feedback_category"
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

    <el-table v-loading="loading" :data="list">
      <el-table-column label="标题" align="left" prop="title" width="180" show-overflow-tooltip />
      <el-table-column label="账号" align="center" prop="userName" width="110" show-overflow-tooltip />
      <el-table-column label="分类" align="center" prop="category" width="88">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.vpn_feedback_category" :value="scope.row.category"/>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="88">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.vpn_feedback_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="客户端版本" align="center" prop="clientVersion" width="110" show-overflow-tooltip />
      <el-table-column label="提交时间" align="center" prop="createTime" width="170">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="描述" align="left" prop="content" min-width="240" show-overflow-tooltip />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="100">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleView(scope.row)"
            v-hasPermi="['vpn:feedback:query']"
          >详情</el-button>
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

    <el-dialog :title="'反馈详情'" :visible.sync="open" width="640px" append-to-body>
      <el-form :model="form" label-width="100px" size="small">
        <el-form-item label="标题">
          <span>{{ form.title }}</span>
        </el-form-item>
        <el-form-item label="账号">
          <span>{{ form.userName }}</span>
        </el-form-item>
        <el-form-item label="分类">
          <dict-tag :options="dict.type.vpn_feedback_category" :value="form.category"/>
        </el-form-item>
        <el-form-item label="描述">
          <span style="white-space: pre-wrap;">{{ form.content }}</span>
        </el-form-item>
        <el-form-item label="版本">
          <span>{{ form.clientVersion }}</span>
        </el-form-item>
        <el-form-item label="平台">
          <span>{{ form.clientPlatform }}</span>
        </el-form-item>
        <el-form-item label="IP">
          <span>{{ form.ipaddr }}</span>
        </el-form-item>
        <el-form-item label="时间">
          <span>{{ parseTime(form.createTime) }}</span>
        </el-form-item>
        <el-form-item label="截图" v-if="imageList.length">
          <el-image
            v-for="(url, index) in imageList"
            :key="index"
            :src="url"
            :preview-src-list="imageList"
            style="width: 80px; height: 80px; margin-right: 8px"
            fit="cover"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio-button label="0">待处理</el-radio-button>
            <el-radio-button label="1">已排期</el-radio-button>
            <el-radio-button label="2">已修复</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm" v-hasPermi="['vpn:feedback:edit']">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listFeedback, getFeedback, updateFeedbackStatus } from "@/api/vpn/feedback"

export default {
  name: "VpnFeedback",
  dicts: ['vpn_feedback_status', 'vpn_feedback_category'],
  data() {
    return {
      loading: true,
      total: 0,
      list: [],
      open: false,
      imageList: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userName: undefined,
        status: undefined,
        category: undefined
      },
      form: {}
    }
  },
  created() {
    this.getList()
  },
  methods: {
    parseImageUrls(row) {
      try {
        const parsed = JSON.parse((row && row.imageUrls) || '[]')
        if (!Array.isArray(parsed)) {
          return []
        }
        return parsed.filter(url => !!url).slice(0, 3)
      } catch (e) {
        return []
      }
    },
    getList() {
      this.loading = true
      listFeedback(this.queryParams).then(response => {
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
      this.resetForm("queryForm")
      this.handleQuery()
    },
    handleView(row) {
      this.form = Object.assign({}, row)
      this.imageList = this.parseImageUrls(row)
      this.open = true
      getFeedback(row.id).then(response => {
        const data = response.data || {}
        this.form = data
        this.imageList = this.parseImageUrls(data)
      })
    },
    submitForm() {
      updateFeedbackStatus({ id: this.form.id, status: this.form.status }).then(() => {
        this.$modal.msgSuccess("保存成功")
        this.open = false
        this.getList()
      })
    }
  }
}
</script>
