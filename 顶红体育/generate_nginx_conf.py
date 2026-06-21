#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
从 deploy_config.py 读取域名，生成 Nginx 配置文件。
换域名后运行本脚本即可自动更新 nginx_bt.conf 和 nginx_docker.conf。

用法：
    python generate_nginx_conf.py
"""

import os
import sys

# 添加父目录到 sys.path 以便导入 deploy_config
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PARENT_DIR = os.path.dirname(SCRIPT_DIR)
sys.path.insert(0, PARENT_DIR)

from deploy_config import API_DOMAIN, ADMIN_DOMAIN, LIVE_DOMAIN

# 预计算正则转义域名（Nginx 中 . 需要转义）
_LIVE_RE = LIVE_DOMAIN.replace('.', r'\.')
_ADMIN_RE = ADMIN_DOMAIN.replace('.', r'\.')

# Nginx 配置模板 - 宝塔版
NGINX_BT_TEMPLATE = """# ===== 安全基础配置（所有 server 共用） =====
# 访问频率限制
limit_req_zone $binary_remote_addr zone=admin:10m rate=10r/s;
limit_req_zone $binary_remote_addr zone=api:10m rate=30r/s;

# ===== {API} =====
server {{
    listen 80;
    server_name {API};
    # 生产环境建议开启 HTTPS 并做 301 跳转：
    # return 301 https://$host$request_uri;

    # 安全头
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    location / {{
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }}

    location /admin/ {{
        limit_req zone=admin burst=20 nodelay;
        proxy_pass http://127.0.0.1:8080/admin/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }}

    location /editor/ {{
        limit_req zone=admin burst=20 nodelay;
        proxy_pass http://127.0.0.1:8080/editor/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }}

    location /upload/ {{
        alias /www/wwwroot/dinghong/upload/;
    }}
}}

# ===== {ADMIN} =====
server {{
    listen 80;
    server_name {ADMIN};
    # 生产环境建议开启 HTTPS 并做 301 跳转：
    # return 301 https://$host$request_uri;

    root /www/wwwroot/dinghong/admin;
    index login.html;

    # 安全头
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    location /admin/ {{
        proxy_pass http://127.0.0.1:8080/admin/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }}

    location /editor/ {{
        proxy_pass http://127.0.0.1:8080/editor/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }}

    location /upload/ {{
        alias /www/wwwroot/dinghong/upload/;
    }}

    location / {{
        try_files $uri $uri/ /login.html;
    }}
}}

# ===== {LIVE} =====
server {{
    listen 80;
    server_name {LIVE};
    # 生产环境建议开启 HTTPS 并做 301 跳转：
    # return 301 https://$host$request_uri;

    root /www/wwwroot/dinghong/admin/live;
    index play.html;

    # 安全头
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    location /live/ {{
        proxy_pass http://127.0.0.1:8080/live/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;

        proxy_buffering off;
        proxy_request_buffering off;
        proxy_cache off;
        proxy_read_timeout 3600s;
        proxy_send_timeout 3600s;

        # CORS: 仅允许来自自己域名的跨域请求
        set $cors_origin "";
        if ($http_origin ~* "^https?://({LIVE_RE}|{ADMIN_RE})$") {{
            set $cors_origin $http_origin;
        }}
        add_header Access-Control-Allow-Origin $cors_origin always;
        add_header Access-Control-Allow-Methods 'GET,OPTIONS' always;
        add_header Access-Control-Allow-Headers 'Content-Type,Authorization' always;
    }}

    location / {{
        try_files $uri $uri/ /play.html;
    }}
}}
"""

NGINX_DOCKER_TEMPLATE = NGINX_BT_TEMPLATE  # Docker 版与宝塔版配置相同，只是部署路径不同


def main():
    bt_path = os.path.join(SCRIPT_DIR, "nginx_bt.conf")
    docker_path = os.path.join(SCRIPT_DIR, "nginx_docker.conf")

    # 生成宝塔版
    with open(bt_path, "w", encoding="utf-8") as f:
        f.write(NGINX_BT_TEMPLATE.format(
            API=API_DOMAIN, ADMIN=ADMIN_DOMAIN, LIVE=LIVE_DOMAIN,
            LIVE_RE=_LIVE_RE, ADMIN_RE=_ADMIN_RE
        ))
    print(f"[OK] 已生成 {bt_path}")
    print(f"     server_name: {API_DOMAIN}, {ADMIN_DOMAIN}, {LIVE_DOMAIN}")

    # 生成 Docker 版
    with open(docker_path, "w", encoding="utf-8") as f:
        f.write(NGINX_DOCKER_TEMPLATE.format(
            API=API_DOMAIN, ADMIN=ADMIN_DOMAIN, LIVE=LIVE_DOMAIN,
            LIVE_RE=_LIVE_RE, ADMIN_RE=_ADMIN_RE
        ))
    print(f"[OK] 已生成 {docker_path}")

    print()
    print("Nginx 配置已根据 deploy_config.py 中的域名自动生成。")
    print(f"换域名后重新运行: python generate_nginx_conf.py")


if __name__ == "__main__":
    main()
