#!/bin/bash
# ============================================
# 顶红体育 — Docker 一键部署（修复版）
# 已解决 limit_req_zone 必须在 http {} 块内的问题
# ============================================
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/deploy_config.sh"
BASE_DIR="${PROJECT_DIR:-/data/dinghong}"

echo "=========================================="
echo "  顶红体育 — Docker 环境部署"
echo "  宿主机根目录: ${BASE_DIR}/admin"
echo "  Nginx 容器:   dinghong-nginx"
echo "=========================================="
echo ""

# 备份
echo "[1/5] 备份 Nginx 配置..."
cp ${BASE_DIR}/nginx/nginx.conf ${BASE_DIR}/nginx/nginx.conf.bak.$(date +%Y%m%d%H%M%S)
echo "  ✅ 已备份"

# 部署前端文件
echo "[2/5] 部署前端文件..."
mkdir -p ${BASE_DIR}/admin/css ${BASE_DIR}/admin/js ${BASE_DIR}/admin/live

cp -f admin_pages/login.html      ${BASE_DIR}/admin/login.html
cp -f admin_pages/index.html      ${BASE_DIR}/admin/index.html
cp -f admin_pages/matches.html    ${BASE_DIR}/admin/matches.html
cp -f admin_pages/articles.html   ${BASE_DIR}/admin/articles.html
cp -f admin_pages/ad-config.html  ${BASE_DIR}/admin/ad-config.html
cp -f admin_pages/prompts.html    ${BASE_DIR}/admin/prompts.html
cp -f admin_pages/live.html       ${BASE_DIR}/admin/live.html
cp -f admin_pages/live_play.html  ${BASE_DIR}/admin/live/play.html
cp -f admin_pages/live_ad_config.json ${BASE_DIR}/admin/live/ad_config.json
cp -f admin_pages/live_banner.txt     ${BASE_DIR}/admin/live/banner.txt
cp -f admin_pages/css/common.css  ${BASE_DIR}/admin/css/common.css
cp -f admin_pages/js/common.js    ${BASE_DIR}/admin/js/common.js
echo "  ✅ 已部署 12 个文件"

# 替换 Nginx 配置（用修复版的 nginx_docker.conf，包含完整 http 块）
echo "[3/5] 更新 Nginx 配置..."
cp nginx_docker.conf ${BASE_DIR}/nginx/nginx.conf
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
echo "  后台: http://${ADMIN_DOMAIN}"
echo "  直播: http://${LIVE_DOMAIN}/play.html?key=live_39"
echo "=========================================="
