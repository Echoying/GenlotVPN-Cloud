<template>
  <div class="login">
    <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
      <h3 class="title">{{ title }}</h3>
      <el-form-item prop="username">
        <el-input
          v-model="loginForm.username"
          type="text"
          auto-complete="off"
          placeholder="账号"
          prefix-icon="el-icon-user"
        />
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          v-model="loginForm.password"
          type="password"
          auto-complete="off"
          placeholder="密码"
          prefix-icon="el-icon-lock"
          @keyup.enter.native="handleLogin"
        />
      </el-form-item>
      <el-form-item prop="code" v-if="captchaEnabled">
        <el-input
          v-model="loginForm.code"
          auto-complete="off"
          placeholder="验证码"
          style="width: 63%"
          prefix-icon="el-icon-key"
          @keyup.enter.native="handleLogin"
        />
        <div class="login-code">
          <img :src="codeUrl" @click="getCode" class="login-code-img" />
        </div>
      </el-form-item>
      <el-checkbox v-model="loginForm.rememberMe" style="margin: 0px 0px 25px 0px;">记住密码</el-checkbox>
      <el-form-item style="width: 100%;">
        <el-button
          :loading="loading"
          size="medium"
          type="primary"
          style="width: 100%;"
          @click.native.prevent="handleLogin"
        >
          <span v-if="!loading">登 录</span>
          <span v-else>登 录 中...</span>
        </el-button>
        <div style="text-align: right; margin-top: 10px;">
          <el-button type="text" @click="showChangePwd = true">修改密码</el-button>
        </div>
      </el-form-item>
    </el-form>

    <!-- 修改密码弹窗 -->
    <el-dialog title="修改密码" :visible.sync="showChangePwd" width="500px" append-to-body @close="resetChangePwdForm">
      <el-form ref="changePwdForm" :model="changePwdForm" :rules="changePwdRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="changePwdForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="changePwdForm.oldPassword" type="password" placeholder="请输入旧密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="changePwdForm.newPassword" type="password" placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="changePwdForm.confirmPassword" type="password" placeholder="请确认新密码" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="showChangePwd = false">取 消</el-button>
        <el-button type="primary" :loading="changePwdLoading" @click="handleChangePassword">确 定</el-button>
      </div>
    </el-dialog>

    <!--  底部  -->
    <div class="el-login-footer">
      <span>{{ footerContent }}</span>
    </div>
  </div>
</template>

<script>
import { getCodeImg, changePassword } from "@/api/login"
import Cookies from "js-cookie"
import { encrypt, decrypt } from "@/utils/jsencrypt"
import defaultSettings from "@/settings"

export default {
  name: "Login",
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.changePwdForm.newPassword) {
        callback(new Error("两次输入的密码不一致"))
      } else {
        callback()
      }
    }
    return {
      title: process.env.VUE_APP_TITLE,
      footerContent: defaultSettings.footerContent,
      codeUrl: "",
      loginForm: {
        username: "",
        password: "",
        rememberMe: false,
        code: "",
        uuid: ""
      },
      loginRules: {
        username: [
          { required: true, trigger: "blur", message: "请输入您的账号" }
        ],
        password: [
          { required: true, trigger: "blur", message: "请输入您的密码" }
        ],
        code: [{ required: true, trigger: "change", message: "请输入验证码" }]
      },
      loading: false,
      captchaEnabled: true,
      redirect: undefined,
      // 修改密码
      showChangePwd: false,
      changePwdLoading: false,
      changePwdForm: {
        username: "",
        oldPassword: "",
        newPassword: "",
        confirmPassword: ""
      },
      changePwdRules: {
        username: [
          { required: true, trigger: "blur", message: "请输入用户名" }
        ],
        oldPassword: [
          { required: true, trigger: "blur", message: "请输入旧密码" }
        ],
        newPassword: [
          { required: true, trigger: "blur", message: "请输入新密码" },
          { min: 5, max: 20, message: "密码长度在 5 到 20 个字符", trigger: "blur" }
        ],
        confirmPassword: [
          { required: true, trigger: "blur", message: "请确认新密码" },
          { validator: validateConfirmPassword, trigger: "blur" }
        ]
      }
    }
  },
  watch: {
    $route: {
      handler: function(route) {
        this.redirect = route.query && route.query.redirect
      },
      immediate: true
    }
  },
  created() {
    this.getCode()
    this.getCookie()
  },
  methods: {
    getCode() {
      getCodeImg().then(res => {
        this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled
        if (this.captchaEnabled) {
          this.codeUrl = "data:image/jpeg;base64," + res.img
          this.loginForm.uuid = res.uuid
        }
      }).catch(err => {
        console.error('获取验证码失败:', err)
      })
    },
    getCookie() {
      const username = Cookies.get("vpn-username")
      const password = Cookies.get("vpn-password")
      const rememberMe = Cookies.get("vpn-rememberMe")
      this.loginForm = {
        username: username === undefined ? this.loginForm.username : username,
        password: password === undefined ? this.loginForm.password : decrypt(password),
        rememberMe: rememberMe === undefined ? false : Boolean(rememberMe),
        code: "",
        uuid: ""
      }
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true
          if (this.loginForm.rememberMe) {
            Cookies.set("vpn-username", this.loginForm.username, { expires: 30 })
            Cookies.set("vpn-password", encrypt(this.loginForm.password), { expires: 30 })
            Cookies.set("vpn-rememberMe", this.loginForm.rememberMe, { expires: 30 })
          } else {
            Cookies.remove("vpn-username")
            Cookies.remove("vpn-password")
            Cookies.remove("vpn-rememberMe")
          }
          this.$store.dispatch("Login", this.loginForm).then(() => {
            this.$router.push({ path: "/select-line" }).catch(() => {})
          }).catch(() => {
            this.loading = false
            if (this.captchaEnabled) {
              this.getCode()
            }
          })
        }
      })
    },
    handleChangePassword() {
      this.$refs.changePwdForm.validate(valid => {
        if (valid) {
          if (this.changePwdForm.oldPassword === this.changePwdForm.newPassword) {
            this.$alert("新密码不能与旧密码相同", "提示", { type: "warning" })
            return
          }
          this.changePwdLoading = true
          changePassword({
            username: this.changePwdForm.username,
            oldPassword: this.changePwdForm.oldPassword,
            newPassword: this.changePwdForm.newPassword
          }).then(() => {
            this.$alert("密码修改成功，请使用新密码登录", "提示", { type: "success" }).then(() => {
              this.showChangePwd = false
              // 将用户名回填到登录表单
              this.loginForm.username = this.changePwdForm.username
              this.loginForm.password = ""
            })
          }).catch((error) => {
            const msg = error && error.message ? error.message : "修改密码失败，请稍后重试"
            this.$alert(msg, "修改失败", { type: "error" })
          }).finally(() => {
            this.changePwdLoading = false
          })
        }
      })
    },
    resetChangePwdForm() {
      if (this.$refs.changePwdForm) {
        this.$refs.changePwdForm.resetFields()
      }
    }
  }
}
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
.login {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  background-image: url("../assets/images/login-background.jpg");
  background-size: cover;
}
.title {
  margin: 0px auto 30px auto;
  text-align: center;
  color: #707070;
}
.login-form {
  border-radius: 6px;
  background: #ffffff;
  width: 400px;
  padding: 25px 25px 5px 25px;
  z-index: 1;
  .el-input {
    height: 38px;
    input {
      height: 38px;
    }
  }
}
.login-code {
  width: 33%;
  height: 38px;
  float: right;
  img {
    cursor: pointer;
    vertical-align: middle;
  }
}
.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: #fff;
  font-family: Arial;
  font-size: 12px;
  letter-spacing: 1px;
}
.login-code-img {
  height: 38px;
}
</style>
