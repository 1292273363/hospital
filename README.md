# 医院微信小程序

微信小程序一键登录完整示例，包含原生小程序前端 + Spring Boot 后端。

## 项目结构

```
hospital/
├── miniprogram/              # 微信小程序前端
│   ├── app.js                # 全局入口
│   ├── app.json              # 全局配置
│   ├── app.wxss              # 全局样式
│   ├── project.config.json   # 项目配置
│   ├── sitemap.json
│   ├── images/               # 图片资源
│   ├── utils/
│   │   └── request.js        # 网络请求封装
│   └── pages/
│       ├── login/            # 登录页
│       │   ├── login.js
│       │   ├── login.wxml
│       │   ├── login.wxss
│       │   └── login.json
│       └── index/            # 首页（登录后）
│           ├── index.js
│           ├── index.wxml
│           ├── index.wxss
│           └── index.json
└── backend/                  # Spring Boot 后端
    ├── pom.xml
    └── src/main/
        ├── resources/
        │   ├── application.yml
        │   └── schema.sql    # 数据库建表SQL
        └── java/com/hospital/wechat/
            ├── WechatApplication.java
            ├── controller/
            │   └── WxLoginController.java
            ├── service/
            │   ├── WxLoginService.java
            │   └── impl/WxLoginServiceImpl.java
            ├── entity/
            │   ├── WxUser.java
            │   └── WxUserMapper.java
            ├── dto/
            │   ├── WxLoginRequest.java
            │   ├── LoginResponse.java
            │   └── Result.java
            ├── config/
            │   ├── WebConfig.java
            │   ├── JwtAuthFilter.java
            │   └── MyMetaObjectHandler.java
            └── util/
                └── JwtUtil.java
```

## 快速启动

### 1. 数据库初始化

```sql
-- 执行 backend/src/main/resources/schema.sql
mysql -u root -p < backend/src/main/resources/schema.sql
```

### 2. 修改后端配置

编辑 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/hospital_db
    username: root
    password: 你的MySQL密码

wechat:
  miniprogram:
    appid: 你的小程序AppID
    secret: 你的小程序Secret

jwt:
  secret: 自定义JWT密钥（建议32位以上随机字符串）
```

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
# 服务启动在 http://localhost:8080
```

### 4. 配置小程序

编辑 `miniprogram/app.js`：

```js
globalData: {
  baseUrl: 'http://localhost:8080'  // 开发环境
  // baseUrl: 'https://你的域名'    // 生产环境
}
```

编辑 `miniprogram/project.config.json`，填入你的 `appid`。

用微信开发者工具打开 `miniprogram` 目录即可运行。

## 登录流程说明

```
小程序端                          后端
  │                                │
  │  1. wx.login() 获取 code       │
  │──────────────────────────────▶│
  │                                │  2. 用code调微信接口换取openid
  │                                │     GET code2session API
  │                                │  3. 查询/创建用户记录
  │                                │  4. 生成JWT token
  │◀──────────────────────────────│
  │  5. 返回 token + userInfo      │
  │                                │
  │  6. 本地存储token              │
  │  7. 跳转首页                   │
```

### 手机号授权流程

- 用户点击「微信一键登录」按钮（open-type="getPhoneNumber"）
- 授权后前端获得 `phoneCode`，随 `code` 一起发送到后端
- 后端用 `phoneCode` 调微信 API 获取真实手机号并绑定到用户

## API 接口

### POST /api/wx/login

**请求体：**
```json
{
  "code": "wx.login()返回的code",
  "phoneCode": "手机号授权code（可选）"
}
```

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGci...",
    "userInfo": {
      "id": 1,
      "openid": "oXxx...",
      "nickName": "微信用户",
      "avatarUrl": "",
      "phone": "138****8888"
    }
  }
}
```

### GET /api/wx/health

服务健康检查。

## 技术栈

| 层 | 技术 |
|---|---|
| 小程序前端 | 微信原生小程序 |
| 后端框架 | Spring Boot 3.2 |
| 持久层 | MyBatis Plus 3.5 |
| 数据库 | MySQL 8.0 |
| 认证 | JWT (jjwt 0.12) |
| JDK | Java 17 |

## 注意事项

1. **AppID 和 Secret** 在微信公众平台申请，不要提交到代码仓库
2. **生产环境** 需将后端部署到有域名+HTTPS的服务器，微信要求合法域名
3. **access_token** 建议用 Redis 缓存（有效期2小时），避免频繁请求微信接口
4. **JWT Secret** 建议使用 32 位以上随机字符串，妥善保管

