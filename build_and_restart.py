#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""顶红体育 — 编译 + 重启服务（统一版本）"""
import time

from deploy_config import REMOTE_PROJECT_DIR as PROJ, JDK17_PATH as JDK17
from ssh_utils import connect_ssh, run_cmd, kill_process

JAR_NAME = "dinghong-api-1.0.0.jar"
API_DIR = f"{PROJ}/app/dinghong-api"


def compile_project(ssh):
    """在服务器上编译打包。成功返回 True，失败返回 False。"""
    print("\n===== 编译 =====")
    build_cmd = (
        f"cd {API_DIR} && "
        f"export JAVA_HOME={JDK17} && "
        f"export PATH={JDK17}/bin:$PATH && "
        f"mvn clean package -DskipTests 2>&1"
    )
    stdout, _ = run_cmd(ssh, build_cmd, timeout=300)
    if "BUILD SUCCESS" not in stdout:
        print("编译失败，最后 8 行:")
        for line in stdout.split('\n')[-8:]:
            print(f"  {line}" if line else "")
        return False
    print("  >> BUILD SUCCESS")
    return True


def restart_service(ssh):
    """停掉旧进程，启动新 jar。返回 True 表示启动成功。"""
    print("\n===== 重启服务 =====")
    kill_process(ssh, JAR_NAME)
    time.sleep(2)

    start_cmd = (
        f"cd {API_DIR} && "
        f"source {PROJ}/.env 2>/dev/null; "
        f"source {PROJ}/env.sh 2>/dev/null; "
        f"nohup java -jar target/{JAR_NAME} > /tmp/dinghong-api.log 2>&1 & "
        f"echo NEW_PID=$!"
    )
    run_cmd(ssh, start_cmd)
    time.sleep(5)

    stdout, _ = run_cmd(ssh, "ps aux | grep 'dinghong-api' | grep -v grep")
    if stdout.strip():
        print("\n===== 部署成功 =====")
        print(f"  进程: {stdout.strip()}")
        return True
    else:
        print("\n启动失败，日志尾行:")
        log, _ = run_cmd(ssh, "tail -15 /tmp/dinghong-api.log")
        print(log)
        return False


def main():
    ssh = connect_ssh()
    print("已连接到服务器\n")

    if not compile_project(ssh):
        ssh.close()
        exit(1)

    restart_service(ssh)
    ssh.close()


if __name__ == "__main__":
    main()
