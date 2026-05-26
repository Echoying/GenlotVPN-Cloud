# ruoyi-vpn-ui

GenlotVPN 用户端前端项目

## 项目说明

这是一个精简版的 Vue 2 前端项目，专门为 VPN 用户提供登录界面。

### 技术栈

- Vue 2.6.12
- Vue Router 3.4.9
- Vuex 3.6.0
- Element UI 2.15.14
- Axios 0.30.3

### 主要特性

- ✅ 用户登录（账号密码 + 验证码）
- ✅ 记住密码（RSA 加密存储）
- ✅ Token 认证（独立 Token key: `Vpn-Token`）
- ✅ 代理到 VPN Gateway（端口 8060）

## 开发

```bash
# 安装依赖
npm install

# 启动开发服务器（端口 8060）
npm run dev
```

## 构建

```bash
# 生产环境构建
npm run build:prod

# 预发布环境构建
npm run build:stage
```

## 项目结构

```
ruoyi-vpn-ui/
├── public/              # 静态资源
├── src/
│   ├── api/            # API 接口
│   ├── assets/         # 资源文件（图片、图标）
│   ├── router/         # 路由配置
│   ├── store/          # Vuex 状态管理
│   ├── utils/          # 工具函数
│   ├── views/          # 页面组件
│   ├── App.vue         # 根组件
│   ├── main.js         # 入口文件
│   ├── permission.js   # 路由守卫
│   └── settings.js     # 全局配置
├── .env.development    # 开发环境变量
├── .env.production     # 生产环境变量
├── babel.config.js     # Babel 配置
├── package.json        # 项目依赖
└── vue.config.js       # Vue CLI 配置
```

## 配置说明

### 代理配置

开发环境代理到 `http://10.9.2.177:8060`，通过 `VUE_APP_BASE_API=/vpn-api` 路径前缀。

### 认证接口

- 登录：`POST /auth/login`
- 验证码：`GET /code`

### Token 存储

Token 存储在 Cookie 中，key 为 `Vpn-Token`，与管理后台的 `Admin-Token` 区分。

## 与 ruoyi-ui 的区别

1. **精简依赖**：移除了不必要的组件和插件
2. **独立 Token**：使用 `Vpn-Token` 而非 `Admin-Token`
3. **简化路由**：只有登录页和首页，无动态路由
4. **简化 Store**：只保留 user 模块，无权限、菜单等模块
5. **代理目标**：指向 VPN Gateway（8060）而非管理 Gateway（8080）
6. **图标方案**：使用 Element UI 内置图标，无需 svg-sprite-loader
