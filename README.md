# 校园互助跑腿平台 - 部署说明

## 项目概述

**校园帮** - 面向高校的C2C互助跑腿服务平台，支持微信小程序、管理后台。

### 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.2 + MyBatis-Plus + MySQL 8 + Redis 6 |
| 用户端 | UniApp（微信小程序 / H5 / App） |
| 管理后台 | Vue 3 + Element Plus + ECharts + Vite |
| 认证 | JWT（JSON Web Token） |
| 第三方 | 微信登录、支付宝沙箱、微信订阅消息 |

---

## 环境要求

- JDK 17+
- Maven 3.6+
- Node.js 16+
- MySQL 8.0+
- Redis 6.0+
- HBuilderX（小程序开发）
- 微信开发者工具（小程序预览）

---

## 快速启动

### 1. 数据库初始化

```bash
# 登录 MySQL 并执行建表脚本
mysql -u root -p < database/schema.sql
```

### 2. 启动后端

```bash
cd backend
mvn clean package -DskipTests
java -jar target/campus-help-backend-1.0.0.jar
```

后端启动在 `http://localhost:8080`

### 3. 启动管理后台

```bash
cd admin
npm install
npm run dev
```

管理后台启动在 `http://localhost:5173`，默认账号 admin / admin123

### 4. 启动微信小程序

使用 HBuilderX 打开 `uniapp-vue3/uniapp`，运行到微信开发者工具。

---

## 配置文件说明

### 后端配置（backend/src/main/resources/application.yml）

```yaml
# 数据库
spring.datasource.url: jdbc:mysql://localhost:3306/campus_help
spring.datasource.username: root
spring.datasource.password: your_password

# Redis
spring.data.redis.host: localhost
spring.data.redis.port: 6379

# JWT
jwt.secret: YourSecretKey
jwt.expiration: 604800000

# 微信小程序
wechat.appid: wx491e4b37ff5d0130
wechat.secret: your_app_secret

# 支付宝沙箱
alipay.app-id: 9021000164657830
alipay.private-key: your_private_key
alipay.alipay-public-key: your_alipay_public_key
```

### 小程序配置（uniapp-vue3/uniapp/api/config.js）

```javascript
module.exports = {
  baseUrl: 'http://your-server-ip:8080/api',
  timeout: 30000
}
```

---

## 第三方服务配置

| 服务 | 状态 | 获取方式 |
|------|------|---------|
| 微信小程序 AppID | ✅ 已配置 | mp.weixin.qq.com 注册获取 |
| 微信 AppSecret | ✅ 已配置 | 小程序后台开发设置获取 |
| 支付宝沙箱 | ✅ 已配置 | open.alipay.com 沙箱环境获取 |
| 微信订阅消息 | ✅ 4个模板已配置 | mp.weixin.qq.com 功能→订阅消息 |
| 百度OCR | ⬜ 未配置 | ai.baidu.com 注册获取 |
| 腾讯地图 | ⬜ 未配置 | lbs.qq.com 注册获取 |

---

## API 接口列表

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/auth/wechat | POST | 微信登录 |
| /api/auth/info | GET | 获取用户信息 |
| /api/auth/realname | POST | 提交实名认证 |
| /api/order/create | POST | 创建订单 |
| /api/order/{id} | GET | 订单详情 |
| /api/order/my | GET | 我的订单 |
| /api/order/hall | GET | 订单大厅 |
| /api/order/accept | POST | 抢单（分布式锁） |
| /api/order/{id}/status | PUT | 更新订单状态 |
| /api/order/ge-match | GET | Redis GEO匹配 |
| /api/wallet/info | GET | 钱包信息 |
| /api/wallet/withdraw | POST | 提现申请 |
| /api/wallet/records | GET | 交易流水 |
| /api/chat/send | POST | 发送消息 |
| /api/chat/messages | GET | 聊天记录 |
| /api/evaluation/submit | POST | 提交评价 |
| /api/pay/create | POST | 创建支付宝支付 |
| /api/admin/login | POST | 管理员登录 |
| /api/admin/dashboard | GET | 数据大屏 |
| /api/admin/users | GET | 用户列表 |
| /api/admin/orders | GET | 订单列表 |
| /api/admin/appeals | GET | 申诉列表 |

---

## 生产部署

### 后端部署

```bash
# 打包
cd backend
mvn clean package -DskipTests

# 后台启动
nohup java -jar target/campus-help-backend-1.0.0.jar > campus-help.log 2>&1 &
```

### 管理后台部署

```bash
cd admin
npm run build
# 将 dist 目录部署到 Nginx
```

### Nginx 配置示例

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 管理后台
    root /var/www/admin/dist;
    index index.html;

    # API代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## 注意事项

1. **Redis GEO**：确保 Redis 版本 >= 3.2（支持 GEO 命令），实测 6.0 可用
2. **支付宝沙箱**：需在支付宝开放平台配置沙箱环境的回调地址
3. **微信小程序**：需要在微信公众平台配置服务器域名白名单
4. **生产环境**：必须配置 HTTPS，否则微信登录和支付会失败
5. **JWT密钥**：生产环境请更换 application.yml 中的 jwt.secret

---

## 项目结构

```
├── backend/                    # 后端（Spring Boot）
│   ├── src/main/java/          # Java源代码
│   │   ├── controller/         # API控制器
│   │   ├── service/            # 业务逻辑
│   │   ├── entity/             # 数据实体
│   │   ├── mapper/             # 数据库映射
│   │   ├── config/             # 配置类
│   │   ├── common/             # 公共类
│   │   └── dto/                # 数据传输对象
│   └── src/main/resources/     # 配置文件
│       └── application.yml     # 主配置
├── admin/                      # 管理后台（Vue 3）
│   └── src/views/              # 页面
├── uniapp-vue3/uniapp/         # 小程序（UniApp）
│   └── pages/                  # 页面
├── database/schema.sql         # 数据库建表脚本
└── 使用说明.md                  # 使用说明
```
