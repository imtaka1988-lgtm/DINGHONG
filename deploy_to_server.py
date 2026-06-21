#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""顶红体育 — 欢迎语功能一键部署"""
import os
import sys
import time

from deploy_config import (REMOTE_PROJECT_DIR as PROJECT_DIR,
                           REMOTE_SRC_CONTROLLER as SRC_CONTROLLER,
                           LOCAL_BASE, API_DOMAIN, ADMIN_DOMAIN)
from ssh_utils import connect_ssh, run_cmd, kill_process, upload_file

ADMIN_DIR = PROJECT_DIR + "/admin"
JAR_NAME = "dinghong-api-1.0.0.jar"
API_DIR = f"{PROJECT_DIR}/app/dinghong-api"

LOCAL_FILES = {
    "WechatController.java": os.path.join(
        LOCAL_BASE, "app", "dinghong-api", "src", "main", "java",
        "com", "dinghong", "controller", "wechat", "WechatController.java"
    ),
    "GreetingConfigController.java": os.path.join(
        LOCAL_BASE, "app", "dinghong-api", "src", "main", "java",
        "com", "dinghong", "controller", "admin", "GreetingConfigController.java"
    ),
    "greeting_migration.sql": os.path.join(
        LOCAL_BASE, "顶红体育", "greeting_migration.sql"
    ),
    "wechat-greeting.html": os.path.join(
        LOCAL_BASE, "顶红体育", "admin_pages", "wechat-greeting.html"
    ),
}


def verify_directory(ssh):
    print("\n===== 步骤1: 验证服务器目录 =====")
    _, stderr = run_cmd(
        ssh,
        f"ls {SRC_CONTROLLER}/wechat/WechatController.java 2>&1 && echo OK || echo FAIL"
    )
    if "FAIL" in stderr:
        print("*** 服务器控制器目录不存在 ***")
        return False
    return True


def backup_and_upload(ssh):
    print("\n===== 步骤2: 备份并上传 =====")
    _, ts = run_cmd(ssh, "date +%Y%m%d_%H%M%S", echo=False)
    ts = ts.strip()
    backup_dir = f"{PROJECT_DIR}/backup/greeting_{ts}"
    run_cmd(ssh, f"mkdir -p {backup_dir}")

    wechat_src = f"{SRC_CONTROLLER}/wechat/WechatController.java"
    run_cmd(ssh, f"cp {wechat_src} {backup_dir}/WechatController.java.bak 2>/dev/null; echo DONE")
    print(f"  已备份到 {backup_dir}")

    for name, local_path in LOCAL_FILES.items():
        if os.path.exists(local_path):
            upload_file(ssh, local_path, f"/tmp/{name}")
        else:
            print(f"  *** 警告: 本地文件不存在 {local_path}")

    return True


def move_files(ssh):
    print("\n===== 步骤3: 替换服务器文件 =====")
    run_cmd(ssh, f"cp /tmp/WechatController.java {SRC_CONTROLLER}/wechat/WechatController.java")
    run_cmd(ssh, f"cp /tmp/GreetingConfigController.java {SRC_CONTROLLER}/admin/GreetingConfigController.java")
    run_cmd(ssh, f"cp /tmp/wechat-greeting.html {ADMIN_DIR}/wechat-greeting.html")
    print("  所有文件已替换")


def migrate_db(ssh):
    print("\n===== 步骤4: 数据库迁移 =====")
    stdout, _ = run_cmd(
        ssh,
        "mysql -uroot -p'DingHong@2026' dinghong < /tmp/greeting_migration.sql 2>&1; echo EXIT:$?"
    )
    if "EXIT:0" in stdout:
        print("  数据库迁移成功")
    else:
        print("  (可能表已存在，继续)")


def compile_project(ssh):
    print("\n===== 步骤5: 编译 =====")
    stdout, _ = run_cmd(
        ssh,
        f"cd {API_DIR} && mvn clean package -DskipTests 2>&1",
        timeout=300
    )
    if "BUILD SUCCESS" in stdout:
        print("  编译成功")
        return True
    else:
        print(f"  编译失败: {stdout[:500]}")
        return False


def restart_service(ssh):
    print("\n===== 步骤6: 重启服务 =====")
    kill_process(ssh, "dinghong-api")
    time.sleep(3)

    run_cmd(
        ssh,
        f"cd {API_DIR} && source {PROJECT_DIR}/.env 2>/dev/null; nohup java -jar target/{JAR_NAME} > /tmp/dinghong-api.log 2>&1 & sleep 4"
    )

    stdout, _ = run_cmd(ssh, "ps aux | grep 'dinghong-api' | grep -v grep")
    if stdout.strip():
        print(f"  服务已启动: {stdout.strip()[:150]}")
        return True
    else:
        print("  启动失败，日志:")
        log, _ = run_cmd(ssh, "tail -50 /tmp/dinghong-api.log")
        print(log)
        return False


def main():
    print("=" * 60)
    print("顶红体育 - 每日欢迎语功能部署工具")
    print("=" * 60)

    missing = [p for p in LOCAL_FILES.values() if not os.path.exists(p)]
    if missing:
        print(f"*** 本地文件缺失: {missing}")
        sys.exit(1)

    ssh = connect_ssh()
    print("已连接到服务器")

    try:
        if not verify_directory(ssh):
            sys.exit(1)
        backup_and_upload(ssh)
        move_files(ssh)
        migrate_db(ssh)
        if not compile_project(ssh):
            print("\n*** 编译失败 ***")
            sys.exit(1)
        restart_service(ssh)

        print("\n" + "=" * 60)
        print("部署完成！")
        print("=" * 60)
        print(f"配置后台: http://{ADMIN_DOMAIN}/wechat-greeting.html")
        print(f"API 测试: http://{API_DOMAIN}/wechat/callback")
    finally:
        ssh.close()


if __name__ == "__main__":
    main()
