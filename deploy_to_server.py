#!/usr/bin/env python3
"""顶红体育 - 每日欢迎语功能 一键部署到服务器"""
import paramiko
import os
import sys
import time

HOST = "8.210.102.206"
PORT = 22
USER = "root"
PASS = "Taka888."
PROJECT_DIR = "/data/dinghong"
SRC_CONTROLLER = PROJECT_DIR + "/app/dinghong-api/src/main/java/com/dinghong/controller"
ADMIN_DIR = PROJECT_DIR + "/admin"

# 本地文件路径
BASE = r"c:\Users\Administrator\Desktop\顶红公众号"
LOCAL_FILES = {
    "WechatController.java": os.path.join(BASE, "app", "dinghong-api", "src", "main", "java", "com", "dinghong", "controller", "wechat", "WechatController.java"),
    "GreetingConfigController.java": os.path.join(BASE, "app", "dinghong-api", "src", "main", "java", "com", "dinghong", "controller", "admin", "GreetingConfigController.java"),
    "greeting_migration.sql": os.path.join(BASE, "顶红体育", "greeting_migration.sql"),
    "wechat-greeting.html": os.path.join(BASE, "顶红体育", "admin_pages", "wechat-greeting.html"),
}

def connect():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, PORT, USER, PASS, timeout=20)
    return ssh

def run(ssh, cmd, timeout=120):
    """执行命令并打印输出"""
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
    # 收尾
    while chan.recv_ready():
        stdout += chan.recv(4096)
    while chan.recv_stderr_ready():
        stderr += chan.recv_stderr(4096)
    out = stdout.decode('utf-8', errors='replace')
    err = stderr.decode('utf-8', errors='replace')
    if out:
        print(f"  STDOUT: {out.strip()}")
    if err:
        print(f"  STDERR: {err.strip()}")
    return chan.recv_exit_status(), out, err

def upload_file(ssh, local_path, remote_path):
    """通过SFTP上传文件"""
    sftp = ssh.open_sftp()
    try:
        sftp.put(local_path, remote_path)
        print(f"  UPLOAD: {local_path} -> {remote_path}")
    finally:
        sftp.close()

def step1_verify(ssh):
    print("\n===== 步骤1: 验证服务器目录结构 =====")
    code, out, err = run(ssh, f"ls {SRC_CONTROLLER}/wechat/WechatController.java 2>&1 && echo OK")
    if "OK" not in out:
        print("*** 错误: 服务器控制器目录不存在 ***")
        return False
    return True

def step2_backup_and_prepare(ssh):
    print("\n===== 步骤2: 备份原文件并上传新文件 =====")
    
    # 获取当前日期作为备份标记
    code, out, err = run(ssh, "date +%Y%m%d_%H%M%S")
    backup_ts = out.strip()
    
    # 创建备份目录
    backup_dir = f"{PROJECT_DIR}/backup/greeting_{backup_ts}"
    run(ssh, f"mkdir -p {backup_dir}")
    
    # 备份原 WechatController.java
    wechat_src = f"{SRC_CONTROLLER}/wechat/WechatController.java"
    run(ssh, f"cp {wechat_src} {backup_dir}/WechatController.java.bak 2>/dev/null; echo DONE")
    print(f"  已备份 WechatController.java 到 {backup_dir}")
    
    # 上传新文件到服务器的 /tmp 目录
    for name, local_path in LOCAL_FILES.items():
        if os.path.exists(local_path):
            upload_file(ssh, local_path, f"/tmp/{name}")
        else:
            print(f"  *** 警告: 本地文件不存在 {local_path}")
    
    return True

def step3_move_files(ssh):
    print("\n===== 步骤3: 替换服务器文件 =====")
    
    # 替换 WechatController.java
    run(ssh, f"cp /tmp/WechatController.java {SRC_CONTROLLER}/wechat/WechatController.java && echo 'OK'")
    
    # 放置 GreetingConfigController.java
    run(ssh, f"cp /tmp/GreetingConfigController.java {SRC_CONTROLLER}/admin/GreetingConfigController.java && echo 'OK'")
    
    # 放置后台页面
    run(ssh, f"cp /tmp/wechat-greeting.html {ADMIN_DIR}/wechat-greeting.html && echo 'OK'")
    
    print("  所有文件已替换完成")

def step4_migration(ssh):
    print("\n===== 步骤4: 执行数据库迁移 =====")
    code, out, err = run(ssh, "mysql -uroot -p'DingHong@2026' dinghong < /tmp/greeting_migration.sql 2>&1; echo 'EXIT:'$?")
    if "EXIT:0" in out or "EXIT:0" in err:
        print("  数据库迁移成功")
        return True
    else:
        print(f"  数据库迁移输出: {out}")
        print(f"  错误: {err}")
        # 尝试继续，可能表已存在
        return True

def step5_compile(ssh):
    print("\n===== 步骤5: 编译打包 =====")
    print("  正在编译... (可能需要1-2分钟)")
    code, out, err = run(ssh, f"cd {PROJECT_DIR}/app/dinghong-api && mvn clean package -DskipTests 2>&1", timeout=300)
    
    if code == 0 or "BUILD SUCCESS" in out:
        print("  编译成功")
        return True
    else:
        print(f"  编译输出: {out[:500]}")
        print(f"  编译错误: {err[:500]}")
        return False

def step6_restart(ssh):
    print("\n===== 步骤6: 重启服务 =====")
    
    # 查找并杀掉旧进程
    code, out, err = run(ssh, "ps aux | grep 'dinghong-api' | grep -v grep | awk '{print $2}'")
    old_pids = out.strip().split('\n')
    
    for pid in old_pids:
        pid = pid.strip()
        if pid:
            print(f"  正在停止旧进程 PID={pid}")
            run(ssh, f"kill -15 {pid} 2>/dev/null; echo KILLED")
    
    time.sleep(3)
    
    # 加载环境变量并启动
    run(ssh, f"""
        cd {PROJECT_DIR}/app/dinghong-api
        source {PROJECT_DIR}/.env 2>/dev/null
        nohup java -jar target/dinghong-api-1.0.0.jar > /tmp/dinghong-api.log 2>&1 &
        echo "NEW_PID=$!"
        sleep 4
        ps aux | grep 'dinghong-api' | grep -v grep
    """)
    
    # 验证
    code, out, err = run(ssh, "ps aux | grep 'dinghong-api' | grep -v grep")
    if out.strip():
        print(f"  服务已启动: {out.strip()}")
        return True
    else:
        print("  *** 服务启动失败，检查日志 ***")
        code, out, err = run(ssh, "tail -50 /tmp/dinghong-api.log")
        print(f"  日志: {out}")
        return False

def main():
    print("=" * 60)
    print("顶红体育 - 每日欢迎语功能部署工具")
    print("=" * 60)
    
    # 验证本地文件
    missing = []
    for name, path in LOCAL_FILES.items():
        if not os.path.exists(path):
            missing.append(path)
    if missing:
        print(f"*** 错误: 以下本地文件不存在: {missing}")
        sys.exit(1)
    
    ssh = connect()
    print(f"已连接到 {HOST}")
    
    try:
        if not step1_verify(ssh):
            sys.exit(1)
        step2_backup_and_prepare(ssh)
        step3_move_files(ssh)
        step4_migration(ssh)
        
        if not step5_compile(ssh):
            print("\n*** 编译失败，请检查上面的错误信息 ***")
            sys.exit(1)
        
        step6_restart(ssh)
        
        print("\n" + "=" * 60)
        print("部署完成！")
        print("=" * 60)
        print("配置后台: http://admin.5q.lol/wechat-greeting.html")
        print("API 测试: http://api.5q.lol/wechat/callback")
        print("")
        print("下一步:")
        print("1. 访问后台页面，填入微信群二维码链接和欢迎文字")
        print("2. 在公众号发消息测试每日欢迎语")
        
    finally:
        ssh.close()

if __name__ == "__main__":
    main()