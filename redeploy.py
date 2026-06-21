#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""顶红体育 — 快速重部署：上传文件 + 编译 + 重启"""
import os
import base64
import time

from deploy_config import (REMOTE_PROJECT_DIR as PROJ,
                           JDK17_PATH as JDK17,
                           LOCAL_BASE)
from ssh_utils import connect_ssh, run_cmd, kill_process

JAR_NAME = "dinghong-api-1.0.0.jar"
API_DIR = f"{PROJ}/app/dinghong-api"


def upload_text_file(ssh, local_path, remote_path):
    """通过 base64 编码上传文本文件。"""
    content = open(local_path, "rb").read()
    encoded = base64.b64encode(content).decode()
    remote_dir = os.path.dirname(remote_path)
    run_cmd(ssh, f'mkdir -p "{remote_dir}"', echo=False)
    run_cmd(ssh, f'echo "{encoded}" | base64 -d > "{remote_path}"', echo=False)

    stdin, stdout, stderr = ssh.exec_command(f'wc -c < "{remote_path}"')
    remote_size = int(stdout.read().decode().strip())
    ok = abs(remote_size - len(content)) < 5
    status = "[OK]" if ok else "[FAIL]"
    print(
        f"  {status} {os.path.basename(local_path)} "
        f"local={len(content)} remote={remote_size}"
    )


def deploy_files(ssh, file_map):
    """上传 file_map 中指定的文件到服务器。"""
    print("\n===== 上传文件 =====")
    for local_rel, remote_path in file_map:
        local_path = os.path.join(LOCAL_BASE, local_rel)
        if os.path.exists(local_path):
            upload_text_file(ssh, local_path, remote_path)
        else:
            print(f"  [MISSING] {local_path}")


def compile_project(ssh):
    """编译 API 项目。"""
    print("\n===== 编译 =====")
    stdout, _ = run_cmd(
        ssh,
        f"cd {API_DIR} && export JAVA_HOME={JDK17} && export PATH={JDK17}/bin:$PATH && mvn clean package -DskipTests 2>&1 | tail -10",
        timeout=300
    )
    if "BUILD SUCCESS" not in stdout:
        print("编译失败")
        print(stdout[-500:])
        return False
    print("  >> BUILD SUCCESS")
    return True


def restart_service(ssh):
    """停旧进程，启新进程。"""
    print("\n===== 重启服务 =====")
    kill_process(ssh, "dinghong-api")
    time.sleep(3)

    run_cmd(
        ssh,
        f"cd {API_DIR} && nohup java -jar target/{JAR_NAME} > api.log 2>&1 &"
    )
    time.sleep(6)

    stdout, _ = run_cmd(ssh, "ps aux | grep dinghong-api | grep -v grep")
    if stdout.strip():
        print(f"  进程: {stdout.strip()[:120]}")
        return True
    else:
        print("服务未检测到，查看日志:")
        log, _ = run_cmd(ssh, f"tail -5 {API_DIR}/api.log")
        print(log)
        return False


def main(file_map=None):
    """
    file_map: 可选，格式 [(local_relative_path, remote_absolute_path), ...]
              如果不传，只做编译 + 重启。
    """
    ssh = connect_ssh()
    print("已连接到服务器\n")

    if file_map:
        deploy_files(ssh, file_map)

    if not compile_project(ssh):
        ssh.close()
        exit(1)

    restart_service(ssh)
    ssh.close()


if __name__ == "__main__":
    main()
