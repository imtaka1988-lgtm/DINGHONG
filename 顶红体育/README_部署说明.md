# 顶红体育 — 宝塔面板部署包

## 一、环境要求
- 宝塔面板已安装
- Java 17 (宝塔 Java 项目管理器)
- MySQL 8.0 (宝塔数据库)
- Redis 7.x (宝塔 Redis 管理器)
- Nginx 1.x (宝塔网站管理)
- Maven 3.8+ (宝塔插件或手动安装)
- ImageMagick (`yum install -y ImageMagick` 用于封面生成)

## 二、部署步骤

### 1. 导入数据库
宝塔 → 数据库 → 添加数据库
- 数据库名: dinghong
- 用户名: dinghong
- 密码: (自己设一个强密码)
→ 导入 dinghong_db.sql

### 2. 解压后端代码
```bash
mkdir -p /www/wwwroot/dinghong
tar xzf dinghong_src.tar.gz -C /www/wwwroot/dinghong
```

### 3. 修改 application.yml
编辑 `/www/wwwroot/dinghong/app/dinghong-api/src/main/resources/application.yml`
把数据库密码改成你刚刚在宝塔设的密码。

### 4. 设置环境变量（重要！）
在 `/www/wwwroot/dinghong/` 下创建 `.env` 文件:
```bash
export DEEPSEEK_API_KEY=(从原服务器 env.sh 获取)
export WECHAT_APPID=(从原服务器 env.sh 获取)
export WECHAT_SECRET=(从原服务器 env.sh 获取)
export BAIDU_SEARCH_KEY=(从原服务器 env.sh 获取)
```

### 5. 安装中文字体（封面生成必须）
```bash
yum install -y wqy-microhei-fonts
# 或手动下载放到 /usr/share/fonts/
```

### 6. 编译打包
```bash
cd /www/wwwroot/dinghong/app/dinghong-api
mvn clean package -DskipTests
```

### 7. 宝塔 Java 项目管理器
- 添加 Java 项目
- 项目路径: /www/wwwroot/dinghong/app/dinghong-api
- 运行命令: java -jar target/dinghong-api-1.0.0.jar
- 端口: 8080
- 启动前加载: source /www/wwwroot/dinghong/.env

### 8. 配置 Nginx（宝塔网站管理）
创建 3 个网站:
- api.5q.lol → 反向代理到 127.0.0.1:8080
- admin.5q.lol → 根目录指向 /www/wwwroot/dinghong/admin
- live.5q.lol → 根目录指向 /www/wwwroot/dinghong/admin/live

具体 Nginx 配置见 nginx_bt.conf

### 9. 上传文件目录
确保以下目录有写入权限:
```bash
mkdir -p /www/wwwroot/dinghong/upload/live_qr
mkdir -p /www/wwwroot/dinghong/cover
mkdir -p /www/wwwroot/dinghong/article_images
chmod 755 /www/wwwroot/dinghong/upload
chmod 755 /www/wwwroot/dinghong/cover
chmod 755 /www/wwwroot/dinghong/article_images
```

## 三、验证
1. 访问 http://admin.5q.lol 看后台登录页
2. 访问 http://api.5q.lol/wechat/callback 看是否返回微信回调页面
3. 访问 http://live.5q.lol 看直播落地页

## 四、与原服务器的差异
- 去掉了 Docker，直接运行 Jar
- MySQL/Redis 改用宝塔管理
- 确保先设置环境变量再启动
