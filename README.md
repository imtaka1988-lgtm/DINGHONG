# 顶红公众号 / DINGHONG

> 当前状态：安全清理进行中。此仓库是顶红公众号集合体项目，包含 Spring Boot 后端、后台页面、公众号回调、文章生成、赛事直播和第三方接口集成。

## 一、当前最重要的维护原则

```text
1. 不要把真实密钥、服务器密码、数据库密码、微信密钥、第三方 API Key、GitHub Token 提交到仓库。
2. 生产配置必须放在服务器环境变量或服务器本地 .env 中。
3. 仓库只保留源码、迁移脚本、部署文档和示例配置。
4. target/、上传图片、运行时文件、服务器运维脚本、本地调试脚本不得进入仓库。
5. 后续功能修改必须先建分支，再小步提交，再部署验证。
```

## 二、项目目录

```text
app/dinghong-api/        Spring Boot 后端服务
admin/                   早期后台静态页面，待确认是否仍在线上使用
顶红体育/admin_pages/    部署包后台页面，当前更接近可用版本
顶红体育/                旧部署文档、后台页面和参考配置
upload/                  运行时上传文件，后续应从仓库移除
```

## 三、必须配置的环境变量

参考 `.env.example`。生产环境至少需要：

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

WECHAT_APPID=
WECHAT_SECRET=
WECHAT_VERIFY_TOKEN=change-me

LIVE_PROXY_SECRET=change-me-random-proxy-secret
```

## 四、本轮安全清理内容

本轮第一阶段已做：

```text
1. 新增 .gitignore，阻止后续提交 target、上传目录、本地脚本和密钥文件。
2. 新增 .env.example，提供环境变量模板。
3. application.yml 改为从环境变量读取数据库、后台账号、JWT、微信、直播代理和 Odds API 配置。
4. JwtAuthFilter 不再把 /admin/check-auth 当成公开接口。
5. JwtUtil 要求显式配置 JWT_SECRET，且长度至少 32 字符。
6. LiveProxyController 移除硬编码代理签名密钥，改为读取 live.proxy.secret。
7. 删除 target/classes/application.yml，避免构建产物重复携带敏感配置。
```

仍需后续继续处理：

```text
1. 轮换已经暴露过的数据库密码、Odds API Key、JWT_SECRET、后台密码、微信 Token、直播代理密钥。
2. 清理 Git 历史中的旧密钥；如果仓库公开，单纯删除当前文件不够。
3. 删除 target/classes 下剩余 .class 文件。
4. 删除 upload/ 运行时上传文件。
5. 删除 admin/live/play.html.bak_* 备份文件。
6. 删除 server_info.txt、ssh_connect.py、deploy/fix/check 类本地脚本。
7. 合并或删除重复后台目录，确认线上到底使用 admin/ 还是 顶红体育/admin_pages/。
8. UploadController 增加文件大小、MIME、扩展名校验。
9. WechatController 的 verify token 需要继续环境变量化，并减少敏感日志。
```

## 五、本地/服务器部署提示

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
export WECHAT_VERIFY_TOKEN='你的微信服务器校验Token'
export LIVE_PROXY_SECRET='直播代理签名密钥'
export ODDS_API_KEY='你的Odds API Key'

mvn clean package
java -jar target/dinghong-api-1.0.0.jar
```

## 六、上线前检查清单

```text
1. 后端能启动，且缺少关键环境变量时不会默默使用默认弱口令。
2. /admin/login 能登录。
3. /admin/check-auth 无 token 时返回 401，有效 token 时返回 success。
4. 后台 matches / articles 页面请求会携带 Authorization: Bearer token。
5. /live/proxy 旧签名不可用，新签名可用。
6. 微信 callback 验证通过。
7. The Odds API 可正常取数。
8. 仓库中不再新增 target/、.class、.env、server_info.txt、upload/ 文件。
```

## 七、重要安全提醒

已经提交到公开仓库的密钥应视为泄露。即使后续删除文件，旧提交里仍可能查到。正确做法是：

```text
1. 先轮换生产密钥。
2. 再清理当前仓库内容。
3. 最后按需清理 Git 历史或重建干净仓库。
```
