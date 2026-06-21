# DINGHONG 架构重构方案

> Issue #10: strengthen base architecture for future features
> Phase 0 产出 — 只读架构分析，不改业务代码

---

## 一、当前架构现状

### 1.1 包结构

```
com.dinghong
├── DingHongApplication.java          Spring Boot 入口
├── config/
│   ├── JwtAuthFilter.java            JWT 认证过滤器
│   └── JwtUtil.java                  JWT 工具类
├── controller/
│   ├── TestController.java           测试接口
│   ├── admin/      (9 个控制器)
│   │   ├── AuthController.java       登录认证
│   │   ├── UploadController.java     文件上传 ← 过重
│   │   ├── MatchAdminController.java 比赛管理
│   │   ├── GreetingConfigController.java 欢迎语配置
│   │   ├── LiveAdConfigController.java   直播广告配置
│   │   ├── WechatMenuController.java     微信菜单
│   │   ├── AdminBatchController.java     批处理
│   │   └── MatchCopyController.java      比赛复制
│   ├── editor/     (11 个控制器)
│   │   ├── ArticleController.java        文章 CRUD
│   │   ├── ArticleAdminController.java   文章管理
│   │   ├── ArticleApproveController.java 文章审核
│   │   ├── ArticlePublishController.java 文章发布
│   │   ├── ArticleRewriteController.java 文章重写
│   │   └── ...（其余 6 个）
│   ├── live/       (3 个控制器)
│   │   ├── LiveQrController.java        二维码海报 ← 过重 (402行)
│   │   ├── LiveProxyController.java     直播代理
│   │   └── LiveStatusController.java    直播状态
│   ├── prompt/     (1 个控制器)
│   └── wechat/     (1 个控制器)
│       └── WechatController.java        微信回调 ← 过重 (530+行)
└── service/
    ├── MatchDbService.java              ← 超级大文件，混合 SQL + 业务
    ├── MatchService.java
    ├── ai/DeepSeekService.java
    ├── editor/ (EditorService, EditorPsService, WechatMetaService)
    ├── football/FootballApiService.java
    ├── odds/OddsFetchService.java
    ├── research/ (MatchResearchService, ReviewResearchResult)
    ├── rule/RulePromptService.java
    ├── search/ (BaiduSearchService, BraveSearchService)
    └── wechat/ (WechatDraftService, WechatMenuService)
```

### 1.2 职责混乱的核心文件

| 文件 | 行数 | 当前承担的职责 | 应承担的职责 |
|------|------|--------------|-------------|
| **WechatController.java** | ~530 | HTTP 参数、Token 校验、XML 解析、欢迎语、菜单响应、access_token 获取、素材上传、SQL 查询、日志 | HTTP 接收 → 调用 Service → 返回 XML |
| **UploadController.java** | ~175 | 文件校验、路径拼接、access_token 获取、素材上传、SQL UPDATE | 文件校验 → 调用 StorageService |
| **LiveQrController.java** | ~420 | DB 查询、stream_key 生成、二维码生成、海报渲染、access_token、素材上传、DB 更新 | HTTP 接收 → 调用 LiveQrService |
| **MatchDbService.java** | 超大 | 所有 SQL + 业务逻辑混合 | 应拆分为多个 Repository + 业务 Service |
| **MatchResearchService.java** | ~610 | 赛果判断 + 队名匹配 + 联网资料整理 + AI 调用 | 应拆分为子 Service + 纯函数可测试部分 |

### 1.3 重复代码

| 逻辑 | 重复位置 |
|------|---------|
| access_token 获取 + 微信 API 调用 | WechatController(第450行)、UploadController(第90行)、LiveQrController(第310行) |
| 文件路径拼接 + 目录创建 | UploadController(第25行)、LiveQrController(第38行) |
| 文件扩展名/大小/类型校验 | **不存在**（上传时仅检查 `file.isEmpty()`）|

---

## 二、目标架构

### 2.1 拟拆出的包结构

```
com.dinghong
├── DingHongApplication.java
├── common/                     ← 新增
│   ├── ApiResponse.java       统一 JSON 响应
│   ├── AppException.java      统一异常
│   └── GlobalExceptionHandler.java
├── config/                     ← 扩展
│   ├── JwtAuthFilter.java
│   ├── JwtUtil.java
│   ├── AdminProperties.java       ← 新增：@ConfigurationProperties("admin")
│   ├── JwtProperties.java         ← 新增：@ConfigurationProperties("jwt")
│   ├── WechatProperties.java      ← 新增：@ConfigurationProperties("wechat")
│   ├── UploadProperties.java      ← 新增：@ConfigurationProperties("upload")
│   └── OddsProperties.java        ← 新增：@ConfigurationProperties("odds")
├── repository/                 ← 新增
│   ├── MatchLiveRepository.java
│   ├── WechatGreetingRepository.java
│   ├── ArticleRepository.java
│   └── AdminRepository.java
├── controller/
│   ├── admin/     (保持不变，但变薄)
│   ├── editor/    (保持不变)
│   ├── live/      (LiveQrController → 只调 Service)
│   ├── wechat/    (WechatController → 只调 Service)
│   └── prompt/
└── service/
    ├── wechat/                  ← 扩展
    │   ├── WechatAccessTokenService.java   ← 新增
    │   ├── WechatMediaService.java         ← 新增
    │   ├── WechatMessageService.java       ← 新增
    │   ├── WechatCallbackService.java      ← 新增（重构 WechatController 逻辑）
    │   ├── WechatDraftService.java         (已存在)
    │   └── WechatMenuService.java          (已存在)
    ├── storage/                 ← 新增
    │   ├── UploadStorageService.java       ← 新增
    │   └── UploadFileValidator.java        ← 新增
    ├── live/                    ← 新增
    │   ├── LiveQrService.java              ← 新增
    │   ├── QrPosterRenderService.java      ← 新增
    │   └── LiveMatchRepository.java        ← 移至 repository 层
    ├── research/                (已存在，拆分内部函数)
    │   ├── MatchResearchService.java       (变薄，委托子模块)
    │   ├── ScoreResultMatcher.java         ← 新增（赛果判断）
    │   ├── TeamNameMatcher.java            ← 新增（队名匹配）
    │   └── ReviewResearchResult.java       (已存在)
    ├── ai/DeepSeekService.java
    ├── editor/
    ├── football/
    ├── odds/
    ├── rule/
    └── search/
```

### 2.2 配置类分层

| 类 | 映射前缀 | 替代的散落代码 |
|----|---------|-------------|
| AdminProperties | `admin` | AuthController 中的 `@Value("${admin.user}")` |
| JwtProperties | `jwt` | JwtUtil 中的 `@Value("${jwt.secret}")` |
| WechatProperties | `wechat` | WechatController/UploadController/LiveQrController 中的 wechat 相关 `@Value` |
| UploadProperties | `upload` | UploadController/LiveQrController 中的 upload 相关 `@Value` |
| OddsProperties | `odds.api` | OddsFetchService 中的 odds 相关 `@Value` |

---

## 三、分阶段执行计划

### Phase 1：配置体系统一（预计 6 文件变更）

| 操作 | 文件 | 说明 |
|------|------|------|
| 新增 | `config/AdminProperties.java` | `@ConfigurationProperties("admin")` |
| 新增 | `config/JwtProperties.java` | `@ConfigurationProperties("jwt")` |
| 新增 | `config/WechatProperties.java` | `@ConfigurationProperties("wechat")` |
| 新增 | `config/UploadProperties.java` | `@ConfigurationProperties("upload")` |
| 新增 | `config/OddsProperties.java` | `@ConfigurationProperties("odds.api")` |
| 修改 | `DingHongApplication.java` | 添加 `@EnableConfigurationProperties` |
| 修改 | 各 Controller/Service | `@Value` → 注入 Properties Bean |

**验证：** `mvn clean package` 通过，启动不报错。

### Phase 2：微信服务层重构（预计 5 文件新增 + 3 文件修改）

| 操作 | 文件 | 说明 |
|------|------|------|
| 新增 | `service/wechat/WechatAccessTokenService.java` | 统一 access_token 获取（替代 3 处重复） |
| 新增 | `service/wechat/WechatMediaService.java` | 微信素材上传（替代 2 处重复） |
| 新增 | `service/wechat/WechatMessageService.java` | 客服消息发送 |
| 新增 | `service/wechat/WechatCallbackService.java` | 回调处理逻辑（从 WechatController 移出） |
| 修改 | `WechatController.java` | 变薄，只做 HTTP ↔ Service 中转 |
| 修改 | `UploadController.java` | 改用 WechatMediaService |
| 修改 | `LiveQrController.java` | 改用 WechatMediaService |

**验证：** GET callback 校验可用、关注欢迎语可用、菜单可返回文章直播、不再打印 access_token 全文。

### Phase 3：上传和 Live QR 重构（预计 5 文件新增 + 3 文件修改）

| 操作 | 文件 | 说明 |
|------|------|------|
| 新增 | `service/storage/UploadStorageService.java` | 统一文件保存 + 路径生成 |
| 新增 | `service/storage/UploadFileValidator.java` | 扩展名/大小/MIME 校验 |
| 新增 | `service/live/LiveQrService.java` | 二维码海报生成逻辑 |
| 新增 | `service/live/QrPosterRenderService.java` | 海报渲染（从 LiveQrController 移出） |
| 修改 | `UploadController.java` | 改用 UploadStorageService |
| 修改 | `LiveQrController.java` | 改用 LiveQrService，只保留 HTTP 中转 |
| 修改 | `common/ApiResponse.java` | 统一上传响应格式 |

**验证：** `/admin/upload` 可上传、`/admin/matches/{id}/qr` 可生成、缺少 `UPLOAD_PUBLIC_BASE_URL` 时启动报错。

### Phase 4：Repository 层收敛 SQL（预计 4 文件新增 + N 文件修改）

| 操作 | 文件 | 说明 |
|------|------|------|
| 新增 | `repository/MatchLiveRepository.java` | match_live 表操作 |
| 新增 | `repository/WechatGreetingRepository.java` | wechat_greeting_config 表 |
| 新增 | `repository/ArticleRepository.java` | articles 表 |
| 新增 | `repository/AdminRepository.java` | 通用管理查询 |
| 修改 | 各 Controller/Service | 不再直接拿 DataSource 写 SQL |

**验证：** 登录、文章菜单、直播二维码、每日欢迎语功能不退化。

### Phase 5：部署与测试补齐（预计 5 文件新增 + 1 文件修改）

| 操作 | 文件 | 说明 |
|------|------|------|
| 新增 | `deploy/README-new-server.md` | 新服务器完整部署指南 |
| 新增 | `deploy/systemd/dinghong-api.service.example` | systemd 配置模板 |
| 新增 | `app/dinghong-api/sql/README.md` | 数据库初始化说明 + SQL 执行顺序 |
| 新增 | `app/dinghong-api/src/test/java/com/dinghong/service/research/ScoreResultMatcherTest.java` | 赛果判断单元测试 |
| 新增 | `.github/workflows/ci.yml` | CI：`mvn clean package` |
| 修改 | `顶红体育/README_部署说明.md` | 更新过时说明 |

**验证：** `mvn test` 通过、新服务器可按 README 从零部署。

---

## 四、风险点

| 风险 | 等级 | 缓解措施 |
|------|------|---------|
| Controller 变薄过程引入 bug | 中 | 每个 Phase 只改一个功能方向，每次 `mvn clean package` 验证 |
| MatchDbService 拆分破坏业务 | 高 | Phase 4 放在最后，等前面 Service 层稳定后再动 |
| 仓库没有测试，验证靠手动 | 高 | Phase 5 先补关键测试，再改 MatchDbService |
| 现有数据库表结构不完整 | 中 | Phase 5 只做文档梳理，不猜表结构 |

---

## 五、后续（Phase 6+ 不再 Issue #10 范围）

- 引入 SLF4J/Logback 替换 `System.out.println`
- 引入 Flyway/数据库迁移工具
- MySQL → Spring Data JPA（待讨论）

---

*文档版本: Phase 0 — 2026-06-21*
*下一步: 用户批准后进入 Phase 1（配置体系统一）*
