# 顶红公众号 / DINGHONG

## 一、项目概述

顶红体育公众号后端 + 后台管理 + 直播落地页。

- 后端：Spring Boot 3.5.0（Java 21）
- 数据库：MySQL 8
- 部署指南：`deploy/README-new-server.md`

## 二、环境变量

所有配置通过 `application.yml` + `.env` 环境变量管理。完整模板见 `.env.example`。

**必须配置：**
- `DB_URL` / `DB_USER` / `DB_PASSWORD`
- `ADMIN_USER` / `ADMIN_PASS`（密码≥6位）
- `JWT_SECRET`（≥32字符）
- `WECHAT_VERIFY_TOKEN`（≥8位）
- `UPLOAD_DIR`
- `UPLOAD_PUBLIC_BASE_URL`（必须 http:// 或 https:// 开头）

**可选配置：**
- `WECHAT_APPID` / `WECHAT_SECRET` — 微信公众号功能
- `DEEPSEEK_API_KEY` — AI 写作
- `BAIDU_SEARCH_KEY` — 联网搜索
- `ODDS_API_KEY` — Odds API

## 三、当前架构

### 配置层
- `AdminProperties`、`JwtProperties`、`WechatProperties`、`UploadProperties`、`OddsProperties` — 统一的 `@ConfigurationProperties` 类
- 启动时校验 ADMIN_USER/PASS/JWT_SECRET/WECHAT_VERIFY_TOKEN

### 服务层
- `service/wechat/` — WechatAccessTokenService、WechatMediaService、WechatMessageService
- `service/storage/` — UploadStorageService、UploadFileValidator
- `service/research/` — MatchResearchService（赛果判断逻辑已修复，泛化"比分"和"结束"不再当作终局标志）

### 数据层
- `repository/` — MatchLiveRepository、WechatGreetingRepository
- Controller 不再直接持有 DataSource 写 SQL

### 部署
- `deploy/README-new-server.md` — 完整新服务器部署指南
- `deploy/systemd/dinghong-api.service.example` — systemd 配置模板
- `.github/workflows/ci.yml` — JDK 21 Maven 自动构建

## 四、已修复的关键问题

- ✅ WechatController Token 从硬编码改为 Spring 配置注入
- ✅ AuthController 移除弱默认 fallback，改用 AdminProperties
- ✅ UploadController / LiveQrController 路径和域名参数化
- ✅ wechat appid/secret 统一通过 Spring @Value 注入（不再 System.getenv）
- ✅ MatchResearchService 赛果判断：泛化"比分"/"结束"不再作为终局标志，排除预测类表达
- ✅ WechatController 日志脱敏（不打印原始 XML、完整 openid、access_token 全文）
- ✅ UploadController 文件校验（扩展名、MIME、大小限制）
- ✅ Unit test：赛季结果判断 7 个测试用例

## 五、安全注意

- 所有密钥已从代码中移除，通过环境变量注入
- .env 和 deploy_config.py 已加入 .gitignore，不提交到仓库
- 已暴露的历史密钥应视为泄露，生产环境需轮换
