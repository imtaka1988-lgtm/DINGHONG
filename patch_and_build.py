#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""顶红体育 — 在服务器上修复源码 + 编译 + 重启"""
import time

from deploy_config import (REMOTE_PROJECT_DIR as PROJ,
                           JDK17_PATH as JDK17,
                           ADMIN_DOMAIN)
from ssh_utils import connect_ssh, run_cmd, kill_process

SRC = f"{PROJ}/app/dinghong-api/src/main/java/com/dinghong/controller/wechat/WechatController.java"
JAR_NAME = "dinghong-api-1.0.0.jar"
API_DIR = f"{PROJ}/app/dinghong-api"


def show_source_lines(ssh, start, end, label=""):
    """查看源码指定行。"""
    if label:
        print(label)
    run_cmd(ssh, f"sed -n '{start},{end}p' {SRC}")


def fix_lambda(ssh):
    """修复 WechatController.java 中的 lambda 编译错误。"""
    print("\n===== 修复 lambda 编译错误 =====")
    show_source_lines(ssh, 246, 252, "修复前 (246-252):")

    fix_script = (
        "python3 -c \"\n"
        "content = open('\" + SRC + \"', 'r').read();\n"
        "old = 'new Thread(() -> sendDailyGreeting(openid, config)).start();';\n"
        "new = 'String _oid = openid;\\\\n        GreetingConfig _cfg = config;\\\\n        new Thread(() -> sendDailyGreeting(_oid, _cfg)).start();';\n"
        "if old in content:\n"
        "    content = content.replace(old, new);\n"
        "    open('\" + SRC + \"', 'w').write(content);\n"
        "    print('FIXED');\n"
        "else:\n"
        "    print('NOT_FOUND');\n"
        "    lines = content.split('\\\\n');\n"
        "    for i in range(244, min(252, len(lines))):\n"
        "        print(f'{i+1}: {lines[i]}');\n"
        "\""
    )
    run_cmd(ssh, fix_script)

    show_source_lines(ssh, 246, 255, "修复后 (246-255):")


def compile_project(ssh):
    """编译项目。"""
    print("\n===== Maven 编译 (JDK17) =====")
    stdout, _ = run_cmd(
        ssh,
        f"cd {API_DIR} && export JAVA_HOME={JDK17} && export PATH={JDK17}/bin:$PATH && mvn clean package -DskipTests 2>&1",
        timeout=300
    )
    if "BUILD SUCCESS" not in stdout:
        print("编译失败，最后 15 行:")
        for line in stdout.split('\n')[-15:]:
            print(f"    {line}" if line else "")
        return False
    print("  >> BUILD SUCCESS!")
    return True


def restart_service(ssh):
    """停旧进程，启新进程。"""
    print("\n===== 重启服务 =====")
    kill_process(ssh, JAR_NAME)
    time.sleep(2)

    run_cmd(
        ssh,
        f"cd {API_DIR} && source {PROJ}/.env 2>/dev/null; source {PROJ}/env.sh 2>/dev/null; nohup java -jar target/{JAR_NAME} > /tmp/dinghong-api.log 2>&1 & echo NEW_PID=$!"
    )
    time.sleep(5)

    stdout, _ = run_cmd(ssh, "ps aux | grep 'dinghong-api' | grep -v grep")
    if stdout.strip():
        print("\n" + "=" * 60)
        print("  *** 部署成功! ***")
        print("=" * 60)
        print(f"  进程: {stdout.strip()}")
        print(f"  管理后台: http://{ADMIN_DOMAIN}/wechat-greeting.html")
    else:
        print("\n启动失败，日志尾行:")
        log, _ = run_cmd(ssh, "tail -20 /tmp/dinghong-api.log")
        print(log)


def main():
    ssh = connect_ssh()
    print("已连接到服务器\n")

    fix_lambda(ssh)
    if not compile_project(ssh):
        ssh.close()
        exit(1)
    restart_service(ssh)
    ssh.close()


if __name__ == "__main__":
    main()
