# 迁服检查清单

> 基于稳定版本 `be7048a`
> ⚠️ 不包含任何真实 IP、密码、Token

## 一、旧服务器备份

```bash
# 1. 数据库完整备份
mysqldump -u root -p --all-databases > /tmp/dinghong_backup_$(date +%Y%m%d).sql

# 2. 上传目录打包
tar czf /tmp/dinghong_upload_$(date +%Y%m%d).tar.gz /data/dinghong/upload/

# 3. 复制到本地
scp root@旧服务器IP:/tmp/dinghong_backup_*.sql ./
scp root@旧服务器IP:/tmp/dinghong_upload_*.tar.gz ./
```

## 二、新服务器环境

| 项目 | 命令 |
|------|------|
| JDK 21 | `yum install java-21-openjdk` |
| Maven 3.8+ | `yum install maven` |
| MySQL 8.0 | `yum install mysql-server` |
| Nginx | `yum install nginx` |
| 中文字体 | `yum install wqy-microhei-fonts` |
| ImageMagick | `yum install ImageMagick` |

```bash
# 防火墙开放
firewall-cmd --add-port=80/tcp --permanent
firewall-cmd --add-port=8080/tcp --permanent
firewall-cmd --reload
```

## 三、部署步骤

```bash
# 1. 克隆项目
git clone https://github.com/imtaka1988-lgtm/DINGHONG.git /data/dinghong
cd /data/dinghong

# 2. 创建配置
cp deploy_config.py.example deploy_config.py
cp 顶红体育/deploy_config.sh.example 顶红体育/deploy_config.sh
cp .env.example .env

# 3. 编辑 deploy_config.py — 填入：
#    - HOST = "新服务器IP"
#    - PASS = "ssh密码"

# 4. 编辑 顶红体育/deploy_config.sh — 填入：
#    - API_DOMAIN / ADMIN_DOMAIN / LIVE_DOMAIN

# 5. 编辑 .env — 填入（详见 .env.example）：
#    - DB_URL / DB_USER / DB_PASSWORD
#    - ADMIN_USER / ADMIN_PASS（≥6位）
#    - JWT_SECRET（≥32字符）
#    - WECHAT_VERIFY_TOKEN（≥8位）
#    - WECHAT_APPID / WECHAT_SECRET
#    - UPLOAD_DIR = /data/dinghong/upload
#    - UPLOAD_PUBLIC_BASE_URL = https://api.你的域名/upload

# 6. 创建数据库并导入
mysql -u root -p -e "CREATE DATABASE dinghong CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p dinghong < dinghong_backup_XXXXXX.sql

# 7. 创建上传目录
mkdir -p /data/dinghong/upload/live_qr
tar xzf dinghong_upload_XXXXXX.tar.gz -C /
chmod 755 /data/dinghong/upload

# 8. 编译
cd /data/dinghong/app/dinghong-api
mvn clean package -DskipTests

# 9. systemd 启动
cp /data/dinghong/deploy/systemd/dinghong-api.service.example /etc/systemd/system/dinghong-api.service
systemctl daemon-reload
systemctl enable dinghong-api
systemctl start dinghong-api

# 10. Nginx 配置
cp /data/dinghong/顶红体育/nginx_bt.conf /etc/nginx/conf.d/dinghong.conf
# 编辑域名后：
nginx -t && nginx -s reload
```

## 四、验证清单

| # | 检查项 | 预期结果 |
|---|--------|---------|
| 1 | `systemctl status dinghong-api` | active (running) |
| 2 | `curl http://localhost:8080/wechat/callback` | "wechat callback ok" |
| 3 | 微信公众号后台 - 服务器配置 | Token 验证通过 |
| 4 | 微信公众号后台 - 回调域名 | 与 Nginx server_name 一致 |
| 5 | `curl -X POST "http://api.域名/admin/login" -d "username=admin&password=你的密码"` | 返回 JWT token |
| 6 | 浏览器访问 `http://admin.域名` | 后台登录页 |
| 7 | 后台 → 上传图片 | 返回图片 URL |
| 8 | `curl 返回的图片URL` | 图片可访问 |
| 9 | 后台 → 生成直播二维码 | 返回 "ok\|url\|mediaId" |
| 10 | 浏览器访问 `http://live.域名/play.html?key=live_1` | 直播落地页 |
| 11 | 公众号发消息测试 | 正常回复 |
| 12 | 检查日志 `/tmp/dinghong-api.log` | 无异常 |

## 五、回滚方案

```bash
systemctl stop dinghong-api
# 将旧服务器恢复运行
# 将 DNS 解析切回旧服务器
```

---

> 基于 `be7048a` | 不包含真实凭证
