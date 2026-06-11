# 🏃 校园帮 — 校园互助跑腿平台

> 面向高校的 C2C 互助跑腿服务平台，支持微信小程序 + 管理后台，技术栈为 Spring Boot 3 + Vue 3 + UniApp。

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-17-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/spring--boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/vue-3.x-4fc08d.svg)](https://vuejs.org/)

---

## 📋 功能概览

| 模块 | 功能 |
|------|------|
| 🔐 用户系统 | 微信一键登录、实名认证（学生证上传）、信用分体系 |
| 📦 订单系统 | 发布跑腿订单、订单大厅、抢单（Redis 分布式锁）、状态流转 |
| 💰 支付系统 | 支付宝沙箱支付、平台抽成、跑腿员结算、钱包管理 |
| 🗺️ 智能匹配 | Redis GEO 地理位置匹配（3km 范围）+ 多维加权评分 |
| 💬 实时通讯 | 订单内聊天（文字/图片）、微信订阅消息推送 |
| ⭐ 评价系统 | 五星评价、信用分联动增减 |
| 🛡️ 申诉仲裁 | 订单纠纷申诉、管理员仲裁 |
| 📊 管理后台 | 数据大屏、用户管理、订单管理、实名审核、申诉处理 |

---

## 🏗️ 技术架构

```
┌──────────────────────────────────────────────────┐
│                    用户层                         │
│  微信小程序 (UniApp)  │  管理后台 (Vue 3)         │
└──────────────┬────────────────┬──────────────────┘
               │    HTTPS       │
┌──────────────▼────────────────▼──────────────────┐
│              后端服务 (Spring Boot 3.2)           │
│  ┌──────────┬──────────┬──────────┬──────────┐   │
│  │ 认证模块 │ 订单模块 │ 支付模块 │ 钱包模块 │   │
│  │ (JWT)   │ (GEO)   │ (Alipay)│          │   │
│  └──────────┴──────────┴──────────┴──────────┘   │
│  ┌──────────┬──────────┬──────────┬──────────┐   │
│  │ 聊天模块 │ 评价模块 │ 申诉模块 │ 管理模块 │   │
│  └──────────┴──────────┴──────────┴──────────┘   │
└──────────────┬────────────────┬──────────────────┘
               │                │
┌──────────────▼─────┐  ┌──────▼──────────┐
│   MySQL 8.0        │  │   Redis 6.0+    │
│   (业务数据)        │  │   (缓存/GEO/锁) │
└────────────────────┘  └─────────────────┘
```

| 层级 | 技术选型 |
|------|---------|
| 后端框架 | Spring Boot 3.2.5 + MyBatis-Plus 3.5.6 |
| 安全认证 | Spring Security + JWT (jjwt 0.12.5) |
| 数据库 | MySQL 8.0 + Redis 6.0 (GEO / 分布式锁 / 缓存) |
| 用户端 | UniApp (Vue 3) — 微信小程序 / H5 / App |
| 管理后台 | Vue 3 + Element Plus + ECharts 5 + Vite |
| 支付 | 支付宝沙箱 (alipay-sdk-java 4.15) |
| 工具库 | Hutool 5.8.25 + Lombok |

---

## 🚀 快速启动

### 环境要求

| 软件 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | 后端运行环境 |
| Maven | 3.6+ | 后端构建 |
| Node.js | 16+ | 管理后台构建 |
| MySQL | 8.0+ | 业务数据库 |
| Redis | 6.0+ | 缓存 / GEO / 分布式锁 |
| HBuilderX | 最新版 | 小程序开发工具 |
| 微信开发者工具 | 最新版 | 小程序预览调试 |

### 1. 克隆项目

```bash
git clone https://github.com/Ljj041120/campus-help.git
cd campus-help
```

### 2. 数据库初始化

```bash
mysql -u root -p < database/schema.sql
```

脚本会自动创建 `campus_help` 数据库及所有表，并插入初始配置和 3 个测试用户。

### 3. 配置环境变量

```bash
cd backend
cp .env.example .env
# 编辑 .env 填入真实的数据库密码、微信 secret、支付宝密钥等
```

| 变量 | 说明 | 示例 |
|------|------|------|
| `DB_PASSWORD` | MySQL 密码 | `your_password` |
| `JWT_SECRET` | JWT 签名密钥（≥256 位） | `base64url-encoded-key` |
| `WECHAT_APPID` | 微信小程序 AppID | `wx491e4b37ff5d0130` |
| `WECHAT_SECRET` | 微信小程序 AppSecret | `your_secret` |
| `ALIPAY_APP_ID` | 支付宝沙箱 AppID | `9021000164657830` |
| `ALIPAY_PRIVATE_KEY` | 支付宝应用私钥 | `MIIEpA...` |
| `ALIPAY_PUBLIC_KEY` | 支付宝公钥 | `MIIBIj...` |
| `ALIPAY_NOTIFY_URL` | 支付宝回调地址 | `https://your-domain/api/pay/alipay/notify` |

### 4. 启动后端

```bash
cd backend
mvn clean package -DskipTests
java -jar target/campus-help-backend-1.0.0.jar
```

后端启动在 `http://localhost:8080`

### 5. 启动管理后台

```bash
cd admin
npm install
npm run dev
```

管理后台启动在 `http://localhost:5173`

### 6. 启动小程序

使用 HBuilderX 打开 `uniapp-vue3/uniapp`，修改 `api/config.js` 中的 `baseUrl` 为你的后端地址，然后运行到微信开发者工具。

---

## 📡 API 接口列表

| 接口 | 方法 | 认证 | 说明 |
|------|------|------|------|
| `/api/auth/wechat` | POST | — | 微信登录 |
| `/api/auth/info` | GET | JWT | 获取用户信息 |
| `/api/auth/realname` | POST | JWT | 提交实名认证 |
| `/api/order/create` | POST | JWT | 创建订单 |
| `/api/order/{id}` | GET | JWT | 订单详情 |
| `/api/order/my` | GET | JWT | 我的订单 |
| `/api/order/hall` | GET | JWT | 订单大厅 |
| `/api/order/accept` | POST | JWT | 抢单（分布式锁） |
| `/api/order/{id}/status` | PUT | JWT | 更新订单状态 |
| `/api/order/ge-match` | GET | JWT | Redis GEO 匹配 |
| `/api/wallet/info` | GET | JWT | 钱包信息 |
| `/api/wallet/withdraw` | POST | JWT | 提现申请 |
| `/api/wallet/records` | GET | JWT | 交易流水 |
| `/api/chat/send` | POST | JWT | 发送消息 |
| `/api/chat/messages` | GET | JWT | 聊天记录 |
| `/api/evaluation/submit` | POST | JWT | 提交评价 |
| `/api/pay/create` | POST | JWT | 创建支付宝支付 |
| `/api/pay/alipay/notify` | POST | — | 支付宝异步回调 |
| `/api/admin/login` | POST | — | 管理员登录 |
| `/api/admin/dashboard` | GET | JWT | 数据大屏 |
| `/api/admin/users` | GET | JWT | 用户列表 |
| `/api/admin/orders` | GET | JWT | 订单列表 |
| `/api/admin/appeals` | GET | JWT | 申诉列表 |
| `/api/upload` | POST | JWT | 文件上传 |

---

## 📁 项目结构

```
campus-help/
├── backend/                         # 后端 (Spring Boot)
│   ├── pom.xml                      # Maven 依赖
│   ├── .env.example                 # 环境变量模板
│   └── src/main/
│       ├── java/com/campushelp/
│       │   ├── CampusHelpApplication.java
│       │   ├── common/              # Result / 异常 / 全局处理
│       │   ├── config/              # Security / JWT / Redis / MyBatis
│       │   ├── controller/          # API 控制器
│       │   │   ├── admin/           # 管理后台接口
│       │   │   ├── auth/            # 认证接口
│       │   │   ├── chat/            # 聊天接口
│       │   │   ├── evaluation/      # 评价接口
│       │   │   ├── order/           # 订单接口
│       │   │   └── wallet/          # 钱包接口
│       │   ├── dto/                 # 请求/响应 DTO
│       │   ├── entity/              # 数据实体 (9 张表)
│       │   ├── enums/               # 枚举
│       │   ├── mapper/              # MyBatis Mapper
│       │   └── service/             # 业务逻辑
│       └── resources/
│           └── application.yml      # 主配置
├── admin/                           # 管理后台 (Vue 3)
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── main.js
│       ├── App.vue
│       ├── router/index.js          # 路由 + 导航守卫
│       └── views/                   # 7 个页面
├── uniapp-vue3/uniapp/              # 小程序 (UniApp)
│   ├── pages/                       # 页面
│   │   ├── index/                   # 首页 / 订单大厅
│   │   ├── login/                   # 微信登录
│   │   ├── publish/                 # 发布订单
│   │   ├── order/                   # 我的订单
│   │   ├── chat/                    # 聊天
│   │   ├── user/                    # 个人中心
│   │   └── wallet/                  # 钱包
│   ├── api/                         # 接口封装
│   │   ├── config.js                # baseUrl 配置
│   │   └── request.js               # 请求拦截器
│   └── manifest.json                # 应用配置
├── database/
│   └── schema.sql                   # 数据库建表 + 初始数据
└── 优化方案.md                       # 代码优化方案
```

---

## 🔒 安全说明

- **敏感配置全部外部化**：数据库密码、JWT 密钥、微信 secret、支付宝私钥均通过环境变量注入，不存储在代码仓库中
- **CORS 白名单**：仅允许配置的域名跨域访问（`cors.allowed-origins`）
- **分布式锁**：抢单使用 Redis + Lua 脚本原子操作，防止锁误删
- **生产环境务必**：
  - 使用 HTTPS 部署
  - 更换所有默认密钥和沙箱凭证
  - 在微信公众平台配置服务器域名白名单
  - 在支付宝开放平台配置正式环境参数

---

## 🚢 生产部署

### 后端

```bash
cd backend
mvn clean package -DskipTests

# 设置环境变量后启动
export DB_PASSWORD=xxx
export JWT_SECRET=xxx
# ... 其他变量

nohup java -jar target/campus-help-backend-1.0.0.jar > app.log 2>&1 &
```

### 管理后台

```bash
cd admin
npm run build
# 将 dist/ 目录部署到 Nginx
```

### Nginx 配置示例

```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate     /etc/ssl/certs/your-cert.pem;
    ssl_certificate_key /etc/ssl/private/your-key.pem;

    # 管理后台
    root /var/www/admin/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 上传文件
    location /uploads/ {
        alias /path/to/uploads/;
    }
}
```

---

## 🧪 测试账号

数据库初始化时自动创建：

| 角色 | 用户名 | 说明 |
|------|--------|------|
| 管理员 | `admin_test` | 管理后台登录 |
| 跑腿员 | `runner_test` | 已实名，信用分 85 |
| 发布者 | `publisher_test` | 已实名，信用分 80 |

> 管理后台登录账号由后端 `/api/admin/login` 接口验证，具体凭据取决于数据库 `users` 表。

---

## 📝 License

MIT © 2025
