#!/bin/bash
# ============================================
# 顶红体育 — Docker 一键部署（修复版）
# 已解决 limit_req_zone 必须在 http {} 块内的问题
# ============================================
set -e

echo "=========================================="
echo "  顶红体育 — Docker 环境部署"
echo "  宿主机根目录: /data/dinghong/admin"
echo "  Nginx 容器:   dinghong-nginx"
echo "=========================================="
echo ""

# 备份
echo "[1/5] 备份 Nginx 配置..."
cp /data/dinghong/nginx/nginx.conf /data/dinghong/nginx/nginx.conf.bak.$(date +%Y%m%d%H%M%S)
echo "  ✅ 已备份"

# 部署前端文件
echo "[2/5] 部署前端文件..."
mkdir -p /data/dinghong/admin/css /data/dinghong/admin/js /data/dinghong/admin/live

cp -f admin_pages/login.html      /data/dinghong/admin/login.html
cp -f admin_pages/index.html      /data/dinghong/admin/index.html
cp -f admin_pages/matches.html    /data/dinghong/admin/matches.html
cp -f admin_pages/articles.html   /data/dinghong/admin/articles.html
cp -f admin_pages/ad-config.html  /data/dinghong/admin/ad-config.html
cp -f admin_pages/prompts.html    /data/dinghong/admin/prompts.html
cp -f admin_pages/live.html       /data/dinghong/admin/live.html
cp -f admin_pages/live_play.html  /data/dinghong/admin/live/play.html
cp -f admin_pages/live_ad_config.json /data/dinghong/admin/live/ad_config.json
cp -f admin_pages/live_banner.txt     /data/dinghong/admin/live/banner.txt
cp -f admin_pages/css/common.css  /data/dinghong/admin/css/common.css
cp -f admin_pages/js/common.js    /data/dinghong/admin/js/common.js
echo "  ✅ 已部署 12 个文件"

# 替换 Nginx 配置（用修复版的 nginx_docker.conf，包含完整 http 块）
echo "[3/5] 更新 Nginx 配置..."
cp nginx_docker.conf /data/dinghong/nginx/nginx.conf
echo "  ✅ 已替换"

# 语法检查
echo "[4/5] 检查 Nginx 语法..."
docker exec dinghong-nginx nginx -t
echo "  ✅ 语法正确"

# 热重载
echo "[5/5] 热重载 Nginx..."
docker exec dinghong-nginx nginx -s reload
echo "  ✅ 已重载"

echo ""
echo "=========================================="
echo "  🎉 部署完成！验证："
echo "  后台: http://admin.5q.lol"
echo "  直播: http://live.5q.lol/play.html?key=live_39"
echo "=========================================="