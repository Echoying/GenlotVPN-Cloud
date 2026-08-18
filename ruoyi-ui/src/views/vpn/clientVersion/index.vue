<template>
  <div class="app-container">
    <el-card shadow="never">
      <div slot="header" class="clearfix">
        <span>客户端版本策略</span>
      </div>
      <el-form ref="form" :model="form" :rules="rules" size="small" label-width="140px" v-loading="loading">
        <el-form-item label="启用版本拦截" prop="enabled">
          <el-switch
            v-model="form.enabled"
            active-value="1"
            inactive-value="0"
          />
        </el-form-item>
        <el-form-item label="最低客户端版本" prop="minVersion">
          <el-input
            v-model="form.minVersion"
            placeholder="格式如 1.2.0"
            maxlength="32"
            style="width: 360px"
          />
        </el-form-item>
        <el-form-item label="Windows 下载链接" prop="downloadUrlWindows">
          <el-input
            v-model="form.downloadUrlWindows"
            placeholder="http:// 或 https://"
            maxlength="512"
            style="width: 480px"
          />
        </el-form-item>
        <el-form-item label="macOS 下载链接" prop="downloadUrlMacos">
          <el-input
            v-model="form.downloadUrlMacos"
            placeholder="http:// 或 https://"
            maxlength="512"
            style="width: 480px"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            maxlength="500"
            style="width: 480px"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="mini"
            :loading="saving"
            @click="submitForm"
            v-hasPermi="['vpn:clientVersion:edit']"
          >保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { getClientVersionPolicy, updateClientVersionPolicy } from '@/api/vpn/clientVersion'

export default {
  name: 'VpnClientVersion',
  data() {
    const validateMinVersion = (rule, value, callback) => {
      const enabled = this.form.enabled === '1'
      const v = value == null ? '' : String(value).trim()
      if (!enabled) {
        if (v && !/^\d+\.\d+\.\d+$/.test(v)) {
          callback(new Error('版本号格式须为 x.y.z'))
        } else {
          callback()
        }
        return
      }
      if (!v) {
        callback(new Error('启用版本拦截时最低客户端版本不能为空'))
        return
      }
      if (!/^\d+\.\d+\.\d+$/.test(v)) {
        callback(new Error('版本号格式须为 x.y.z'))
        return
      }
      callback()
    }
    const validateUrl = (rule, value, callback) => {
      const v = value == null ? '' : String(value).trim()
      if (!v) {
        callback()
        return
      }
      if (!/^https?:\/\//i.test(v)) {
        callback(new Error('链接须以 http:// 或 https:// 开头'))
        return
      }
      callback()
    }
    return {
      loading: false,
      saving: false,
      form: {
        id: 1,
        enabled: '0',
        minVersion: '',
        downloadUrlWindows: '',
        downloadUrlMacos: '',
        remark: ''
      },
      rules: {
        minVersion: [{ validator: validateMinVersion, trigger: 'blur' }],
        downloadUrlWindows: [{ validator: validateUrl, trigger: 'blur' }],
        downloadUrlMacos: [{ validator: validateUrl, trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getPolicy()
  },
  methods: {
    getPolicy() {
      this.loading = true
      getClientVersionPolicy().then(response => {
        const data = response.data || {}
        this.form = {
          id: data.id != null ? data.id : 1,
          enabled: data.enabled != null ? data.enabled : '0',
          minVersion: data.minVersion || '',
          downloadUrlWindows: data.downloadUrlWindows || '',
          downloadUrlMacos: data.downloadUrlMacos || '',
          remark: data.remark || ''
        }
      }).finally(() => {
        this.loading = false
      })
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) {
          return
        }
        this.saving = true
        updateClientVersionPolicy(this.form).then(() => {
          this.$modal.msgSuccess('保存成功')
          this.getPolicy()
        }).finally(() => {
          this.saving = false
        })
      })
    }
  }
}
</script>
