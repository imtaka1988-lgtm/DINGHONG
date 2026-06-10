#!/usr/bin/env python3
"""修复mysql/jdk路径问题并完成部署"""
import paramiko
import os
import sys
import time

HOST = "8.210.102.206"
USER = "root"
PASS = "Taka888."
PROJECT = "/data/dinghong"
SRC_CTRL = PROJECT + "/app/dinghong-api/src/main/java/com/dinghong/controller"
ADMIN = PROJECT + "/admin"

BASE = r"c:\Users\Administrator\Desktop\顶红公众号"

def run(ssh, cmd, timeout=120):
    print(f"  >>> {cmd}")
    chan = ssh.get_transport().open_session()
    chan.exec_command(cmd)
    chan.settimeout(timeout)
    stdout = b""
    stderr = b""
    while not chan.exit_status_ready():
        if chan.recv_ready():
            stdout += chan.recv(4096)
        if chan.recv_stderr_ready():
            stderr += chan.recv_stderr(4096)
    while chan.recv_ready():
        stdout += chan.recv(4096)
    while chan.recv_stderr_ready():
        stderr += chan.recv_stderr(4096)
    out = stdout.decode('utf-8', errors='replace')
    err = stderr.decode('utf-8', errors='replace')
    if out.strip(): print(f"  STDOUT: {out.strip()}")
    if err.strip(): print(f"  STDERR: {err.strip()}")
    return out, err

def upload(ssh, local, remote):
    sftp = ssh.open_sftp()
    sftp.put(local, remote)
    sftp.close()
    print(f"  UPLOADED: {local} -> {remote}")

def main():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, 22, USER, PASS, timeout=20)
    print(f"Connected to {HOST}\n")

    # ========== 1. Find mysql ==========
    print("===== 检查 mysql 路径 =====")
    out, _ = run(ssh, "which mysql 2>/dev/null || find / -name 'mysql' -type f 2>/dev/null | head -5")
    
    out2, _ = run(ssh, "docker exec mysql mysql -uroot -p'DingHong@2026' dinghong -e 'SELECT 1' 2>&1 | head -5")
    print(f"  Docker mysql test: {out2.strip()}")

    out3, _ = run(ssh, "ls /data/dinghong/mysql/ 2>/dev/null; which docker 2>/dev/null; docker ps --format '{{.Names}}' 2>/dev/null")
    
    # ========== 2. Check JDK ==========
    print("\n===== 检查 JDK 版本 =====")
    out, _ = run(ssh, "java -version 2>&1; javac -version 2>&1; mvn -version 2>&1 | head -5")
    
    # ========== 3. Find JDK 17 ==========
    out, _ = run(ssh, "ls /usr/lib/jvm/ 2>/dev/null; ls /usr/java/ 2>/dev/null; find / -name 'javac' -type f 2>/dev/null | head -5")
    
    ssh.close()

if __name__ == "__main__":
    main()