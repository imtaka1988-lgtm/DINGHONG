#!/bin/bash
# ============================================
# 顶红体育 — 一键部署脚本（兼容 Docker / 宝塔）
#
# 使用方法：
#   chmod +x deploy.sh
#
#   # Docker 环境（Nginx 在宿主机）：
#   WEB_ROOT=/usr/share/nginx/html bash deploy.sh
#
#   # Docker 环境（Nginx 在容器里）：
#   WEB_ROOT=/home/dinghong/html NGINX_CONTAINER=dinghong-nginx bash deploy.sh
#
#   # 宝塔环境（默认）：
#   bash deploy.sh
# ============================================

set -e

WEB_ROOT="${WEB_ROOT:-/www/wwwroot/dinghong/admin}"
NGINX_CONTAINER="${NGINX_CONTAINER:-}"

echo "=========================================="
echo "  顶红体育 — 前端文件部署"
echo "  目标目录: $WEB_ROOT"
[ -n "$NGINX_CONTAINER" ] && echo "  Nginx 容器: $NGINX_CONTAINER"
echo "=========================================="

# 1. 确保目录存在
echo "[1/4] 创建目录结构..."
mkdir -p "$WEB_ROOT/css"
mkdir -p "$WEB_ROOT/js"
mkdir -p "$WEB_ROOT/live"

# 2. 部署 HTML 文件
echo "[2/4] 部署后台管理页面..."
cp -f admin_pages/login.html      "$WEB_ROOT/login.html"
cp -f admin_pages/index.html      "$WEB_ROOT/index.html"
cp -f admin_pages/matches.html    "$WEB_ROOT/matches.html"
cp -f admin_pages/articles.html   "$WEB_ROOT/articles.html"
cp -f admin_pages/ad-config.html  "$WEB_ROOT/ad-config.html"
cp -f admin_pages/prompts.html    "$WEB_ROOT/prompts.html"
cp -f admin_pages/live.html       "$WEB_ROOT/live.html"

# 3. 部署直播落地页（live_play.html → play.html）
echo "[3/4] 部署直播落地页..."
cp -f admin_pages/live_play.html       "$WEB_ROOT/live/play.html"
cp -f admin_pages/live_ad_config.json  "$WEB_ROOT/live/ad_config.json"
cp -f admin_pages/live_banner.txt      "$WEB_ROOT/live/banner.txt"

# 4. 部署 CSS / JS 模块
echo "[4/4] 部署 CSS/JS 模块..."
cp -f admin_pages/css/common.css  "$WEB_ROOT/css/common.css"
cp -f admin_pages/js/common.js    "$WEB_ROOT/js/common.js"

# 5. 设置权限
echo ""
echo "设置目录权限..."
chmod 755 "$WEB_ROOT"          2>/dev/null || true
chmod 755 "$WEB_ROOT/css"      2>/dev/null || true
chmod 755 "$WEB_ROOT/js"       2>/dev/null || true
chmod 755 "$WEB_ROOT/live"     2>/dev/null || true

echo ""
echo "=========================================="
echo "  ✅ 前端文件部署完成！"
echo "=========================================="
echo ""
echo "══════════════════════════════════════════"
echo "  接下来更新 Nginx 配置"
echo "══════════════════════════════════════════"
echo ""

if [ -n "$NGINX_CONTAINER" ]; then
    echo "检测到 Nginx 在 Docker 容器中运行"
    echo ""
    echo "1. 复制 nginx_bt.conf 到容器内："
    echo "   docker cp nginx_bt.conf $NGINX_CONTAINER:/etc/nginx/conf.d/dinghong.conf"
    echo ""
    echo "2. 重载 Nginx："
    echo "   docker exec $NGINX_CONTAINER nginx -t && docker exec $NGINX_CONTAINER nginx -s reload"
    echo ""
elif [ -f /www/server/panel/vhost/nginx ]; then
    echo "检测到宝塔面板环境"
    echo ""
    echo "  cp nginx_bt.conf /www/server/panel/vhost/nginx/dinghong.conf"
    echo "  nginx -t && nginx -s reload"
    echo ""
else
    echo "标准 Linux Nginx 环境"
    echo ""
    echo "  cp nginx_bt.conf /etc/nginx/conf.d/dinghong.conf"
    echo "  nginx -t && nginx -s reload"
    echo ""
fi

echo ""
echo "══════════════════════════════════════════"
echo "  验证地址"
echo "══════════════════════════════════════════"
echo ""
echo "  后台: http://admin.5q.lol"
echo "  直播: http://live.5q.lol/play.html?key=live_39"
echo "  API:  http://api.5q.lol/wechat/callback"