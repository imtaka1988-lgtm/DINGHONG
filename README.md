# 顶红公众号 / DINGHONG

> 当前状态：公众号 + 文章生成 + 直播单页结合体，仍处于整理和安全清理阶段。此仓库包含 Spring Boot 后端、后台静态页面、微信公众号回调、赛事文章生成、直播单页代理和第三方接口集成。

## 一、当前维护原则

```text
1. 不再把真实密钥、服务器密码、数据库密码、微信密钥、第三方 API Key 提交到仓库。
2. 生产配置优先放在服务器环境变量或服务器本地 .env 中。
3. 后续修改必须先建分支，再小步提交，再验证。
4. 不要直接相信旧 README、旧脚本、旧备份文件；以当前代码为准。
5. 公众号链路、后台链路、直播单页链路要分开验证，不要一次性大改。
```

## 二、项目目录现状

```text
app/dinghong-api/        Spring Boot 后端服务
admin/                   早期后台静态页面，是否仍在线上使用待确认
顶红体育/admin_pages/    部署包后台页面，当前更接近可用版本
顶红体育/                旧部署文档、后台页面和参考配置
upload/                  运行时上传文件，后续应从仓库移除
```

## 三、当前后端技术栈

```text
Java 21
Spring Boot 3.5.0
Spring Web
Spring JDBC
MySQL 8 驱动
JJWT 0.11.5
ZXing 二维码相关依赖
```

## 四、环境变量现状

当前 `application.yml` 已经把大部分配置改成环境变量形式，但不是所有变量都已经被业务代码实际使用。

### 1. 当前实际需要重点配置

```bash
SERVER_PORT=8080

DB_URL=jdbc:mysql://127.0.0.1:3306/dinghong?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
DB_USER=dinghong
DB_PASSWORD=change-me

ADMIN_USER=admin
ADMIN_PASS=change-me
JWT_SECRET=change-me-to-a-random-32-byte-minimum-secret
JWT_EXPIRATION_MS=86400000

ODDS_API_ENABLED=true
ODDS_API_KEY=
ODDS_API_REGIONS=eu,uk,us
ODDS_API_MARKETS=h2h,spreads,totals

LIVE_PROXY_SECRET=change-me-random-proxy-secret
```

### 2. 特别注意：微信 Token 目前仍未完成环境变量化

`application.yml` 里虽然预留了：

```bash
WECHAT_VERIFY_TOKEN=change-me
```

但当前 `WechatController` 代码仍然硬编码：

```java
private static final String TOKEN = "dinghong2026";
```

所以当前不要误以为设置 `WECHAT_VERIFY_TOKEN` 就会改变公众号服务器校验 Token。这个必须后续单独修复。

## 五、当前已完成的安全清理

```text
1. 新增 .gitignore，阻止后续提交 target、上传目录、本地脚本和密钥文件。
2. 新增 .env.example，提供环境变量模板。
3. application.yml 改为从环境变量读取数据库、后台账号、JWT、直播代理和 Odds API 配置。
4. JwtAuthFilter 不再把 /admin/check-auth 当成公开接口。
5. JwtUtil 要求显式配置 JWT_SECRET，且长度至少 32 字符。
6. LiveProxyController 移除硬编码代理签名密钥，改为读取 live.proxy.secret。
7. 删除 target/classes/application.yml，避免构建产物重复携带敏感配置。
```

## 六、仍需处理的问题

```text
1. WechatController 仍然硬编码 verify token，需要改成读取 wechat.verify-token。
2. WechatController 仍会打印原始 XML、openid、用户输入和回复内容，生产环境日志需要脱敏。
3. AuthController 代码里仍有 admin / DingHong2026 的 @Value fallback；虽然 application.yml 当前会用空值覆盖，但后续仍应彻底删除 fallback。
4. 需要确认线上到底使用 admin/ 还是 顶红体育/admin_pages/，避免两个后台目录长期并存。
5. target/classes 下剩余 .class 文件、upload 运行时文件、bak 文件和本地运维脚本仍应继续清理。
6. UploadController 需要补文件大小、MIME、扩展名和异常返回控制。
7. 已经暴露过的历史密钥仍应视为泄露，生产密钥需要轮换；如果仓库保持 public，单纯删除当前文件不等于历史安全。
```

## 七、主要业务链路

### 1. 后台登录

```text
/admin/login 公开
/admin/check-auth 需要 JWT
其他 /admin/** 默认需要 JWT
```

### 2. 直播单页代理

```text
/live/proxy 是公开路径，但要求 t 和 s 签名参数。
签名密钥来自 LIVE_PROXY_SECRET。
```

### 3. 微信公众号回调

```text
GET  /wechat/callback  用于微信服务器校验
POST /wechat/callback  用于接收公众号消息和菜单事件
```

当前微信校验 token 仍硬编码，后续必须修。

### 4. 文章/菜单内容

公众号菜单关键词包括：

```text
今日推荐
昨日复盘
最新文章
最近直播
今日足球
今日篮球
智能客服
联系人工
```

这些入口最终通过 `MatchDbService` 和文章/比赛相关服务返回文本、图文或二维码海报。

## 八、部署提示

生产服务器不要提交 `.env`。可以通过 systemd、PM2、shell profile 或 Docker 环境变量注入。

示例：

```bash
cd /var/www/DINGHONG/app/dinghong-api

export DB_URL='jdbc:mysql://127.0.0.1:3306/dinghong?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
export DB_USER='dinghong'
export DB_PASSWORD='你的真实密码'
export ADMIN_USER='admin'
export ADMIN_PASS='你的真实后台密码'
export JWT_SECRET='至少32字符的随机字符串'
export LIVE_PROXY_SECRET='直播代理签名密钥'
export ODDS_API_KEY='你的Odds API Key'

mvn clean package
java -jar target/dinghong-api-1.0.0.jar
```

注意：微信 Token 当前不要只靠环境变量配置，因为代码还没接上。

## 九、上线前检查清单

```text
1. 后端能启动。
2. JWT_SECRET 缺失或长度不足时应启动失败。
3. LIVE_PROXY_SECRET 缺失或过短时应启动失败。
4. /admin/login 能登录。
5. /admin/check-auth 无 token 返回 401，有效 token 返回 success。
6. 后台 matches / articles 页面请求会携带 Authorization: Bearer token。
7. /live/proxy 旧签名不可用，新签名可用。
8. 微信 callback 验证通过；当前仍需确认硬编码 token 与微信后台一致。
9. The Odds API 根据额度情况决定是否启用；额度紧张时设置 ODDS_API_ENABLED=false。
10. 仓库中不再新增 target/、.class、.env、server_info.txt、upload/ 文件。
```

## 十、重要安全提醒

已经提交到公开仓库的密钥应视为泄露。即使后续删除文件，旧提交里仍可能查到。正确做法是：

```text
1. 先轮换生产密钥。
2. 再清理当前仓库内容。
3. 最后按需清理 Git 历史或重建干净仓库。
```
