#!/usr/bin/env python3
"""继续部署：数据库迁移 + JDK17编译 + 重启"""
import paramiko
import time

HOST = "8.210.102.206"
USER = "root"
PASS = "Taka888."
PROJECT = "/data/dinghong"
JDK17 = "/usr/lib/jvm/java-17-openjdk-17.0.19.0.10-1.0.2.1.al8.x86_64"

def run(ssh, cmd, timeout=120):
    print(f"  >>> {cmd}")
    chan = ssh.get_transport().open_session()
    chan.exec_command(cmd)
    chan.settimeout(timeout)
    stdout = b""
    stderr = b""
    try:
        while not chan.exit_status_ready():
            if chan.recv_ready():
                stdout += chan.recv(65536)
            if chan.recv_stderr_ready():
                stderr += chan.recv_stderr(65536)
        while chan.recv_ready():
            stdout += chan.recv(65536)
        while chan.recv_stderr_ready():
            stderr += chan.recv_stderr(65536)
    except Exception:
        pass
    out = stdout.decode('utf-8', errors='replace')
    err = stderr.decode('utf-8', errors='replace')
    if out.strip(): print(out.strip())
    if err.strip(): print(err.strip())
    return chan.recv_exit_status() if not chan.closed else -1, out, err

def main():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, 22, USER, PASS, timeout=20)
    print(f"Connected to {HOST}")

    # Step 1: Database migration via docker
    print("\n===== 步骤1: 数据库迁移 =====")
    code, out, err = run(ssh,
        "docker exec dinghong-mysql mysql -uroot -p'DingHong@2026' dinghong < /tmp/greeting_migration.sql 2>&1; echo EXIT:$?", timeout=30)

    if "EXIT:0" in out:
        print("  >> 数据库迁移成功")
    elif "already exists" in out.lower() or "duplicate" in out.lower():
        print("  >> 表已存在，跳过")
    else:
        print(f"  >> 迁移输出: {out[:300]}")

    # Step 2: Compile with JDK 17
    print("\n===== 步骤2: Maven编译 (JDK17) =====")
    print("  编译中，约1-2分钟...")
    code, out, err = run(ssh,
        f"""cd {PROJECT}/app/dinghong-api && export JAVA_HOME={JDK17} && export PATH={JDK17}/bin:$PATH && java -version && mvn clean package -DskipTests 2>&1""",
        timeout=300)

    if "BUILD SUCCESS" in out:
        print("  >> 编译成功")
    else:
        print("  >> 编译失败，检查输出:")
        print(out[-800:])
        ssh.close()
        return

    # Step 3: Restart service
    print("\n===== 步骤3: 重启服务 =====")
    code, out, err = run(ssh, "ps aux | grep 'dinghong-api-1.0.0.jar' | grep -v grep | awk '{print $2}'")
    for pid in out.strip().split('\n'):
        pid = pid.strip()
        if pid:
            print(f"  停止旧进程 PID={pid}")
            run(ssh, f"kill -15 {pid}; sleep 1; kill -9 {pid} 2>/dev/null; echo DONE")

    time.sleep(2)

    run(ssh, f"""cd {PROJECT}/app/dinghong-api && source {PROJECT}/.env 2>/dev/null; nohup java -jar target/dinghong-api-1.0.0.jar > /tmp/dinghong-api.log 2>&1 & echo NEW_PID=$!""")

    time.sleep(5)

    code, out, err = run(ssh, "ps aux | grep 'dinghong-api' | grep -v grep")
    if out.strip():
        print("\n" + "="*60)
        print("  YYY 部署成功！")
        print("="*60)
        print(f"  运行进程: {out.strip()}")
        print("  管理后台: http://admin.5q.lol/wechat-greeting.html")
    else:
        print("  XXX 服务未启动，检查日志:")
        code, out, err = run(ssh, "tail -30 /tmp/dinghong-api.log")
        print(out)

    ssh.close()

if __name__ == "__main__":
    main()