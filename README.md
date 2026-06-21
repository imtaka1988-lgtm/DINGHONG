# 顶红公众号 / DINGHONG

> 当前状态：公众号 + 文章生成 + 直播单页结合体，仍处于整理、安全清理和部署梳理阶段。当前仓库还没有达到“换服务器后一键部署”的程度；新服务器部署前必须先核对环境变量、数据库、上传目录、微信回调和静态资源路径。

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
app/cline工作手册/        当前 Cline 规则文件位置；后续建议把 .clinerules 放到仓库根目录
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

当前 `application.yml` 已经把一部分配置改成环境变量形式，但不是所有变量都已经被业务代码实际使用。部署时必须区分“已经生效”和“只是预留”。

### 1. 当前实际生效并需要重点配置

```bash
SERVER_PORT=8080

DB_URL=jdbc:mysql://127.0.0.1:3306/dinghong?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
DB_USER=dinghong
DB_PASSWORD=change-me

ADMIN_USER=admin
ADMIN_PASS=change-me
JWT_SECRET=change-me-to-a-random-32-byte-minimum-secret
JWT_EXPIRATION_MS=86400000

LIVE_PROXY_SECRET=change-me-random-proxy-secret

ODDS_API_ENABLED=false
ODDS_API_KEY=
ODDS_API_REGIONS=eu,uk,us
ODDS_API_MARKETS=h2h,spreads,totals
ODDS_API_ODDS_FORMAT=decimal
ODDS_API_FOOTBALL_SPORTS=soccer_epl,soccer_uefa_champs_league,soccer_japan_j_league,soccer_spain_la_liga,soccer_italy_serie_a,soccer_germany_bundesliga,soccer_france_ligue_one,soccer_uefa_europa_league,soccer_usa_mls,soccer_fifa_world_cup,soccer_fifa_world_cup_winner,soccer_uefa_euro_qualification,soccer_uefa_nations_league,soccer_conmebol_copa_libertadores,soccer_england_efl_championship,soccer_netherlands_eredivisie,soccer_portugal_primeira_liga
ODDS_API_BASKETBALL_SPORTS=basketball_nba,basketball_wnba,basketball_euroleague,basketball_ncaab,basketball_nbl,basketball_nba_summer_league
```

说明：`ODDS_API_ENABLED` 当前默认关闭，目的是先避免文章生成阶段用同一套盘口后处理覆盖四位作者的独立观点。需要单独调试盘口接口时，再手动设置为 `true` 并配置 `ODDS_API_KEY`。

### 2. 当前已配置但业务代码未完全接上的变量

```bash
WECHAT_VERIFY_TOKEN=change-me
UPLOAD_DIR=/data/dinghong/upload
UPLOAD_PUBLIC_BASE_URL=https://example.com/upload
```

注意：

```text
1. WECHAT_VERIFY_TOKEN 目前还不能真正改变微信公众号服务器校验 Token，因为 WechatController 仍硬编码 TOKEN。
2. UPLOAD_DIR / UPLOAD_PUBLIC_BASE_URL 目前只是后续重构目标，UploadController 仍硬编码上传目录和公网 URL。
3. 新服务器部署时不要误以为配置了这些变量就已经生效。
```

### 3. 微信 appid / secret

```bash
WECHAT_APPID=
WECHAT_SECRET=
```

当前部分代码通过 `System.getenv("WECHAT_APPID")` / `System.getenv("WECHAT_SECRET")` 读取，而不是统一从 Spring 配置读取。新服务器可以先用系统环境变量注入，但后续应改成 `wechat.appid` / `wechat.secret` 统一管理。

## 五、当前已完成的安全清理

```text
1. 新增 .gitignore，阻止后续提交 target、上传目录、本地脚本和密钥文件。
2. 新增 .env.example，提供环境变量模板。
3. application.yml 改为从环境变量读取数据库、后台账号、JWT、直播代理和 Odds API 配置。
4. JwtAuthFilter 不再把 /admin/check-auth 当成公开接口。
5. JwtUtil 要求显式配置 JWT_SECRET，且至少 32 字符。
6. LiveProxyController 移除硬编码代理签名密钥，改为读取 live.proxy.secret。
7. 删除 target/classes/application.yml，避免构建产物重复携带敏感配置。
```

## 六、仍需处理的问题

```text
1. WechatController 仍然硬编码 verify token，需要改成读取 wechat.verify-token。
2. WechatController 仍会打印原始 XML、openid、用户输入和回复内容，生产环境日志需要脱敏。
3. AuthController 代码里仍有 admin / DingHong2026 的 @Value fallback；虽然 application.yml 当前会用空值覆盖，但后续仍应彻底删除 fallback 并做启动校验。
4. UploadController 仍硬编码 /data/dinghong/upload/ 和 https://api.5q.lol/upload/，换服务器或换域名时必须改代码；后续应改为 UPLOAD_DIR / UPLOAD_PUBLIC_BASE_URL。
5. 需要确认线上到底使用 admin/ 还是 顶红体育/admin_pages/，避免两个后台目录长期并存。
6. target/classes 下剩余 .class 文件、upload 运行时文件、bak 文件和本地运维脚本仍应继续清理。
7. UploadController 还需要补文件大小、MIME、扩展名和异常返回控制。
8. 已经暴露过的历史密钥仍应视为泄露，生产密钥需要轮换；如果仓库保持 public，单纯删除当前文件不等于历史安全。
9. 四位作者的文章差异化第一步已通过关闭默认盘口覆盖处理，后续还应继续做作者风格化推荐策略。
10. 复盘人工比分兜底逻辑仍需修正：不要把“比分预测 2-1”误认为“最终比分 2-1”。
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

当前微信校验 token 仍硬编码，后续必须修。新服务器上配置 `WECHAT_VERIFY_TOKEN` 还不够。

### 4. 上传图片

```text
/admin/upload
/admin/upload/{matchId}
```

当前上传目录和返回 URL 仍硬编码。新服务器部署前必须确认：

```text
1. /data/dinghong/upload/ 是否存在且 Java 进程有写权限。
2. https://api.5q.lol/upload/ 是否仍是正确公网访问域名。
3. Nginx 是否把 /upload/ 映射到真实上传目录。
```

后续目标是改成 `UPLOAD_DIR` 和 `UPLOAD_PUBLIC_BASE_URL`，但当前代码还未完成。

### 5. 文章/菜单内容

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

当前只适合“人工部署”，还不是完整的一键换服方案。生产服务器不要提交 `.env`。可以通过 systemd、shell profile、PM2 或 Docker 环境变量注入，但仓库目前还没有正式的 systemd/Docker 部署模板。

临时手动启动示例：

```bash
cd /var/www/DINGHONG/app/dinghong-api

export DB_URL='jdbc:mysql://127.0.0.1:3306/dinghong?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
export DB_USER='dinghong'
export DB_PASSWORD='你的真实密码'
export ADMIN_USER='admin'
export ADMIN_PASS='你的真实后台密码'
export JWT_SECRET='至少32字符的随机字符串'
export LIVE_PROXY_SECRET='直播代理签名密钥'
export ODDS_API_ENABLED='false'
# 需要调试盘口接口时再设置 ODDS_API_ENABLED='true' 并配置 ODDS_API_KEY

# 当前微信 appid/secret 仍有代码直接读 System.getenv
export WECHAT_APPID='你的微信公众号 appid'
export WECHAT_SECRET='你的微信公众号 secret'

mvn clean package
java -jar target/dinghong-api-1.0.0.jar
```

注意：

```text
1. 微信 Token 当前不要只靠环境变量配置，因为代码还没接上。
2. 上传路径和图片公网域名当前不要只靠环境变量配置，因为代码还没接上。
3. 新服务器从零部署还需要补数据库 schema / 迁移脚本顺序说明。
```

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
9. 上传接口能写入当前硬编码目录 /data/dinghong/upload/，且公网 /upload/ 能访问。
10. 生成同一场比赛的四位作者文章时，先确认 ODDS_API_ENABLED=false，避免旧盘口覆盖逻辑把今日看法统一成同一套结果。
11. 仓库中不再新增 target/、.class、.env、server_info.txt、upload/ 文件。
```

## 十、重要安全提醒

已经提交到公开仓库的密钥应视为泄露。即使后续删除文件，旧提交里仍可能查到。正确做法是：

```text
1. 先轮换生产密钥。
2. 再清理当前仓库内容。
3. 最后按需清理 Git 历史或重建干净仓库。
```
