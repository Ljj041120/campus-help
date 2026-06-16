# 🏃 校园帮 — 校园互助跑腿平台

> 面向高校的 C2C 互助跑腿服务平台，支持微信小程序 + 管理后台。

[![Java](https://img.shields.io/badge/java-17-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/spring--boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/vue-3.x-4fc08d.svg)](https://vuejs.org/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

---

## 📋 功能概览

| 模块 | 功能 |
|------|------|
| 🔐 用户系统 | 微信一键登录、实名认证（学生证上传）、信用分体系 |
| 📦 订单系统 | 发布跑腿订单、订单大厅、抢单（Redis分布式锁）、状态流转 |
| 💰 支付系统 | 支付宝沙箱支付、平台抽成、跑腿员结算、钱包管理 |
| 🗺️ 智能匹配 | Redis GEO 地理位置匹配（3km范围）+ 多维加权评分 |
| 💬 实时通讯 | 订单内聊天（文字）、微信订阅消息推送 |
| ⭐ 评价系统 | 五星评价、信用分联动增减（SQL原子更新） |
| 🛡️ 申诉仲裁 | 订单纠纷申诉、管理员仲裁 |
| 📊 管理后台 | 数据大屏、用户管理、订单管理、实名审核、申诉处理 |

---

## 🏗️ 技术架构

| 层级 | 技术选型 |
|------|---------|
| 后端框架 | Spring Boot 3.2.5 + MyBatis-Plus 3.5.6 |
| 安全认证 | Spring Security + JWT (jjwt 0.12.5) + BCrypt |
| 数据库 | MySQL 8.0 + Redis 6.0（GEO/锁/缓存） |
| 用户端 | UniApp（Vue 3）— 微信小程序 / H5 / App |
| 管理后台 | Vue 3 + Element Plus + ECharts 5 + Vite |
| 支付 | 支付宝沙箱（alipay-sdk-java 4.15） |
| 工具库 | Hutool 5.8.25 + Lombok |

---

## 🚀 快速启动

### 环境要求

| 软件 | 版本 |
|------|------|
| JDK | 17+ |
| Maven | 3.6+ |
| MySQL | 8.0+ |
| Redis | 6.0+ |
| Node.js | 16+ |

### 1. 数据库初始化

```bash
mysql -u root -p < database/schema.sql
```

### 2. 配置环境变量

在 IntelliJ 的运行配置中设置：

```
DB_PASSWORD=你的数据库密码
JWT_SECRET=你的JWT密钥
WECHAT_SECRET=你的微信secret
```

### 3. 启动后端

```bash
cd backend
mvn clean package -DskipTests
java -jar target/campus-help-backend-1.0.0.jar
```

后端启动在 `http://localhost:8080`

### 4. 启动管理后台

```bash
cd admin
npm install
npm run dev
```

管理后台启动在 `http://localhost:5173`

### 5. 启动小程序

使用 HBuilderX 打开 `uniapp-vue3/uniapp`，运行到微信开发者工具。

---

## 🧪 测试账号

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 管理员 | `admin_test` | `admin123` | 管理后台登录（BCrypt加密） |
| 跑腿员 | `runner_test` | 微信登录 | 已实名，信用分85 |
| 发布者 | `publisher_test` | 微信登录 | 已实名，信用分80 |

---

## 📡 API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/auth/wechat` | POST | 微信登录 |
| `/api/auth/info` | GET | 用户信息 |
| `/api/auth/realname` | POST | 实名认证 |
| `/api/order/create` | POST | 创建订单 |
| `/api/order/{id}` | GET | 订单详情 |
| `/api/order/my` | GET | 我的订单 |
| `/api/order/hall` | GET | 订单大厅 |
| `/api/order/accept` | POST | 抢单 |
| `/api/order/{id}/status` | PUT | 更新状态 |
| `/api/order/ge-match` | GET | GEO匹配 |
| `/api/wallet/info` | GET | 钱包信息 |
| `/api/wallet/withdraw` | POST | 提现 |
| `/api/chat/send` | POST | 发送消息 |
| `/api/chat/messages` | GET | 聊天记录 |
| `/api/evaluation/submit` | POST | 提交评价 |
| `/api/pay/create` | POST | 创建支付 |
| `/api/admin/login` | POST | 管理员登录 |
| `/api/admin/dashboard` | GET | 数据大屏 |

---

## 🔒 安全说明

- 敏感配置通过环境变量注入，不存储在代码仓库
- 管理员密码使用 BCrypt 加密存储
- CORS 配置白名单，不开放通配符
- 订单状态修改仅限订单发布者或跑腿员
- SQL 注入防护：使用 MyBatis-Plus Page 对象
- 支付宝回调验签 + 幂等处理

---

## 📁 项目结构

```
campus-help/
├── backend/           # Spring Boot 后端
│   ├── src/main/java/com/campushelp/
│   │   ├── controller/   # API 控制器
│   │   ├── service/      # 业务逻辑
│   │   ├── entity/       # 数据实体
│   │   ├── mapper/       # 数据库映射
│   │   ├── config/       # 配置类
│   │   └── enums/        # 枚举
│   └── src/main/resources/
│       └── application.yml
├── admin/             # Vue 3 管理后台
│   └── src/views/     # 7个页面
├── uniapp-vue3/       # UniApp 小程序
│   └── uniapp/pages/  # 7个页面
├── database/
│   └── schema.sql     # 数据库建表脚本

```

---

## 📝 License

MIT © 2025
