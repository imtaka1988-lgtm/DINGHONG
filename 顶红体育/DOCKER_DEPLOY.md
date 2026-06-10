# 顶红体育 — Docker 环境部署指南

## 部署方式（二选一）

### 方式 A：一键脚本部署（推荐）

将整个 `顶红公众号` 文件夹上传到服务器（如 `/home/dinghong/`），然后：

```bash
cd /home/dinghong
chmod +x deploy.sh

# 如果你的网站根目录不是 /www/wwwroot/dinghong/admin，通过环境变量指定：
export WEB_ROOT=/你的网站根目录

bash deploy.sh
```

### 方式 B：手动部署

```bash
# 1. 确认你的 Nginx 静态文件根目录（通常是 docker-compose 里 volumes 映射的目标）
#    常见路径：/usr/share/nginx/html 或 /www/wwwroot/dinghong/admin
WEB_ROOT="/www/wwwroot/dinghong/admin"

# 2. 创建目录
mkdir -p $WEB_ROOT/css $WEB_ROOT/js $WEB_ROOT/live

# 3. 复制所有管理后台页面
cp admin_pages/login.html      $WEB_ROOT/login.html
cp admin_pages/index.html      $WEB_ROOT/index.html
cp admin_pages/matches.html    $WEB_ROOT/matches.html
cp admin_pages/articles.html   $WEB_ROOT/articles.html
cp admin_pages/ad-config.html  $WEB_ROOT/ad-config.html
cp admin_pages/prompts.html    $WEB_ROOT/prompts.html
cp admin_pages/live.html       $WEB_ROOT/live.html

# 4. 复制直播落地页（注意：live_play.html 上传后需命名为 play.html）
cp admin_pages/live_play.html  $WEB_ROOT/live/play.html
cp admin_pages/live_ad_config.json $WEB_ROOT/live/ad_config.json
cp admin_pages/live_banner.txt     $WEB_ROOT/live/banner.txt

# 5. 复制 CSS/JS 模块
cp admin_pages/css/common.css  $WEB_ROOT/css/common.css
cp admin_pages/js/common.js    $WEB_ROOT/js/common.js

# 6. 更新 Nginx 配置
cp nginx_bt.conf /etc/nginx/conf.d/dinghong.conf
# 或根据你的 Docker Nginx 配置路径调整

# 7. 重载 Nginx
nginx -t && nginx -s reload
# 或 docker exec <nginx容器名> nginx -s reload
```

## Nginx 配置更新

用 `nginx_bt.conf` 的内容替换你的 Nginx 配置。如果你用的是 Docker Nginx 容器，配置文件通常在：

- **路径**：`/etc/nginx/conf.d/default.conf` 或自定义挂载的目录
- **重载**：`docker exec <容器名> nginx -s reload`

### Docker Compose 示例参考

如果你的 `docker-compose.yml` 类似这样：

```yaml
services:
  nginx:
    image: nginx:1.25
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/conf.d/default.conf
      - ./html:/usr/share/nginx/html
```

那么：
- 把前端文件放到 `./html/` 目录
- 把 `nginx_bt.conf` 覆盖为 `./nginx.conf`
- `docker compose restart nginx`

## 验证

部署后访问以下地址确认：

| 地址 | 预期结果 |
|------|---------|
| `http://你的域名/login.html` | 登录页 |
| `http://你的域名/matches.html` | 自动跳转登录（未登录时） |
| `http://你的域名/live/play.html?key=live_39` | 直播播放页 |

## 常见问题

### Q: Nginx 重载报错 `unknown directive "limit_req_zone"`
需要在 Nginx 主配置 `nginx.conf` 的 `http {}` 块里加上：
```
limit_req_zone $binary_remote_addr zone=admin:10m rate=10r/s;
limit_req_zone $binary_remote_addr zone=api:10m rate=30r/s;
```
然后把 `nginx_bt.conf` 里最上面的那两行 `limit_req_zone` 删掉（因为要写在 http 块里，不能写在 server 块里）。

### Q: 页面样式错乱
确认 CSS/JS 路径正确：页面引用的是 `css/common.css` 和 `js/common.js`（相对路径），确保这两个文件已上传。

### Q: 找不到 play.html
Nginx 配置里 `live.5q.lol` 的 root 指向 `$WEB_ROOT/live`，index 是 `play.html`。确保 `live_play.html` 上传后命名为 `play.html`。