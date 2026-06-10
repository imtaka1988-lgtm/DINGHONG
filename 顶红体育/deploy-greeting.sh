#!/bin/bash
# ============================================
# 顶红体育 - 每日欢迎语功能部署脚本
# 在 FinalShell 中执行: bash deploy-greeting.sh
# ============================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="/www/wwwroot/dinghong"
SRC_DIR="$PROJECT_DIR/app/dinghong-api/src/main/java/com/dinghong"
CONTROLLER_DIR="$SRC_DIR/controller"
ADMIN_DIR="$PROJECT_DIR/admin"

echo "===== 1. 备份原文件 ====="
BACKUP_DIR="$PROJECT_DIR/backup/greeting_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"
cp "$CONTROLLER_DIR/wechat/WechatController.java" "$BACKUP_DIR/" 2>/dev/null || true
echo "备份完成: $BACKUP_DIR"

echo ""
echo "===== 2. 执行数据库迁移 ====="
mysql -u root -p'DingHong@2026' dinghong < "$SCRIPT_DIR/greeting_migration.sql" 2>&1
echo "数据库迁移完成"

echo ""
echo "===== 3. 替换 Java 源码 ====="

# WechatController.java
cp "$SCRIPT_DIR/WechatController_modified.java" "$CONTROLLER_DIR/wechat/WechatController.java" 2>/dev/null || {
    # 如果不在同一目录，从同级读取
    if [ -f "$CONTROLLER_DIR/wechat/WechatController.java.bak_greeting_$(date +%Y%m%d)" ]; then
        echo "检测到已有备份，跳过备份步骤"
    else
        cp "$CONTROLLER_DIR/wechat/WechatController.java" "$CONTROLLER_DIR/wechat/WechatController.java.bak_greeting_$(date +%Y%m%d)"
    fi
    echo "WechatController.java 已备份"
}

# 查找并复制修改后的文件
if [ -f "./WechatController.java" ]; then
    cp "./WechatController.java" "$CONTROLLER_DIR/wechat/WechatController.java"
    echo "已复制 WechatController.java"
else
    echo "*** 错误: 找不到 WechatController.java，请确保文件在当前目录 ***"
    exit 1
fi

# GreetingConfigController.java
if [ -f "./GreetingConfigController.java" ]; then
    mkdir -p "$CONTROLLER_DIR/admin"
    cp "./GreetingConfigController.java" "$CONTROLLER_DIR/admin/GreetingConfigController.java"
    echo "已复制 GreetingConfigController.java"
else
    echo "*** 错误: 找不到 GreetingConfigController.java ***"
    exit 1
fi

# 管理后台页面
if [ -f "./wechat-greeting.html" ]; then
    cp "./wechat-greeting.html" "$ADMIN_DIR/wechat-greeting.html"
    chown www:www "$ADMIN_DIR/wechat-greeting.html" 2>/dev/null || chmod 644 "$ADMIN_DIR/wechat-greeting.html"
    echo "已复制 wechat-greeting.html 到 $ADMIN_DIR"
fi

echo ""
echo "===== 4. 编译打包 ====="
cd "$PROJECT_DIR/app/dinghong-api"
mvn clean package -DskipTests 2>&1
echo "编译完成"

echo ""
echo "===== 5. 重启服务 ====="
# 宝塔 Java 项目管理器方式重启
APP_NAME="dinghong-api-1.0.0"
PID=$(ps aux | grep "$APP_NAME" | grep -v grep | awk '{print $2}')

if [ -n "$PID" ]; then
    echo "正在杀死旧进程 PID=$PID ..."
    kill -15 $PID
    sleep 5

    # 确保进程已停止
    if ps -p $PID > /dev/null 2>&1; then
        kill -9 $PID
        sleep 2
    fi
    echo "旧进程已停止"
fi

# 加载环境变量
if [ -f "$PROJECT_DIR/.env" ]; then
    source "$PROJECT_DIR/.env"
fi

# 启动新进程
cd "$PROJECT_DIR/app/dinghong-api"
nohup java -jar target/dinghong-api-1.0.0.jar > /dev/null 2>&1 &
NEW_PID=$!
echo "新进程已启动 PID=$NEW_PID"

sleep 3

# 验证
if ps -p $NEW_PID > /dev/null 2>&1; then
    echo "===== 部署成功 ====="
    echo "PID: $NEW_PID"
    echo "API 地址: http://api.5q.lol"
    echo "后台配置页面: http://admin.5q.lol/wechat-greeting.html"

    echo ""
    echo "===== 后续步骤 ====="
    echo "1. 在微信后台设置客服消息服务器（如未设置）"
    echo "2. 访问 http://admin.5q.lol/wechat-greeting.html 配置二维码和欢迎文字"
    echo "3. 测试：在公众号发消息，观察是否收到欢迎语"
else
    echo "===== 启动失败，请检查日志 ====="
    exit 1
fi