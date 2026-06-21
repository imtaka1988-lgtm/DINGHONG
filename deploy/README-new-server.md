# 顶红体育 — 新服务器部署指南

从零开始在新服务器上部署 DINGHONG。

## 环境要求

- **JDK 17+** (`java --version`)
- **Maven 3.8+** (`mvn --version`)
- **MySQL 8.0+** (或 MariaDB 10.5+)
- **Nginx 1.x**
- **中文字体** (`yum install -y wqy-microhei-fonts`)
- **ImageMagick** (`yum install -y ImageMagick`) — 封面图生成需要

## 1. 创建数据库

```sql
CREATE DATABASE dinghong DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'dinghong'@'localhost' IDENTIFIED BY 'your-strong-password';
GRANT ALL PRIVILEGES ON dinghong.* TO 'dinghong'@'localhost';
FLUSH PRIVILEGES;
```

## 2. 导入数据库

```bash
# 导入基础结构
mysql -u dinghong -p'your-password' dinghong < 顶红体育/dinghong_db.sql

# 导入欢迎语迁移
mysql -u dinghong -p'your-password' dinghong < 顶红体育/greeting_migration.sql
```

> **注意：** 当前仓库没有完整 schema。如需从旧服务器迁移数据，请使用 `mysqldump` 导出完整结构。

## 3. 克隆项目

```bash
git clone https://github.com/imtaka1988-lgtm/DINGHONG.git /data/dinghong
cd /data/dinghong
```

## 4. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env，填入实际值
vi .env
```

**必须配置的变量：**
- `DB_URL` / `DB_USER` / `DB_PASSWORD` — 数据库连接
- `ADMIN_USER` / `ADMIN_PASS` — 后台登录凭据（密码≥6位）
- `JWT_SECRET` — JWT 签名密钥（随机32字节以上）
- `WECHAT_VERIFY_TOKEN` — 微信回调校验 Token（≥8位）
- `UPLOAD_DIR` — 上传文件存储路径（如 `/data/dinghong/upload`）
- `UPLOAD_PUBLIC_BASE_URL` — 上传文件公开访问 URL（如 `https://api.example.com/upload`）

**可选变量：**
- `WECHAT_APPID` / `WECHAT_SECRET` — 微信后台功能
- `DEEPSEEK_API_KEY` — AI 写作功能
- `BAIDU_SEARCH_KEY` — 联网搜索功能
- `ODDS_API_KEY` — Odds API 数据

## 5. 创建上传目录

```bash
mkdir -p /data/dinghong/upload/live_qr
mkdir -p /data/dinghong/cover
mkdir -p /data/dinghong/article_images
chmod 755 /data/dinghong/upload
chmod 755 /data/dinghong/cover
chmod 755 /data/dinghong/article_images
```

## 6. 编译打包

```bash
cd app/dinghong-api
mvn clean package -DskipTests
```

编译成功后在 `target/` 目录找到 `dinghong-api-1.0.0.jar`。

## 7. 启动服务

### 方式 A：直接启动

```bash
cd /data/dinghong/app/dinghong-api
source /data/dinghong/.env
nohup java -jar target/dinghong-api-1.0.0.jar > /tmp/dinghong-api.log 2>&1 &
```

### 方式 B：systemd 管理

```bash
cp /data/dinghong/deploy/systemd/dinghong-api.service.example /etc/systemd/system/dinghong-api.service
vi /etc/systemd/system/dinghong-api.service  # 修改路径和密码
systemctl daemon-reload
systemctl enable dinghong-api
systemctl start dinghong-api
systemctl status dinghong-api
```

## 8. 配置 Nginx

```bash
cp 顶红体育/nginx_bt.conf /etc/nginx/conf.d/dinghong.conf
# 或：cp 顶红体育/nginx_docker.conf /etc/nginx/conf.d/dinghong.conf
vi /etc/nginx/conf.d/dinghong.conf  # 修改 server_name 域名
nginx -t && nginx -s reload
```

**需要 3 个子域名指向服务器 IP：**
- `api.example.com` → 反向代理到 `127.0.0.1:8080`
- `admin.example.com` → 静态文件根目录 `/data/dinghong/admin`
- `live.example.com` → 静态文件根目录 `/data/dinghong/admin/live`

## 9. 验证

| 接口 | 预期 |
|------|------|
| `GET /wechat/callback` | 返回 "wechat callback ok" |
| `POST /admin/login` | 返回 JWT token |
| `GET /admin/check-auth` | 返回 `{"success":true}` |

```bash
# 快速验证
curl -X POST "http://api.example.com/admin/login" \
  -d "username=admin&password=your-admin-password"
```

## 10. 现有 SQL 文件清单

| 文件 | 说明 |
|------|------|
| `顶红体育/dinghong_db.sql` | 基础建库建表（需确认完整性） |
| `顶红体育/greeting_migration.sql` | 欢迎语功能相关表 |

> **已知缺失：** 没有完整 schema 文件。如需确切表结构，从旧服务器 `mysqldump --no-data dinghong` 导出。
