#!/usr/bin/env python3
"""直接在服务器上修复lambda编译错误，然后编译+重启"""
import paramiko
import time

HOST = "8.210.102.206"
USER = "root"
PASS = "Taka888."
PROJECT = "/data/dinghong"
JDK17 = "/usr/lib/jvm/java-17-openjdk-17.0.19.0.10-1.0.2.1.al8.x86_64"
SRC = PROJECT + "/app/dinghong-api/src/main/java/com/dinghong/controller/wechat/WechatController.java"

def run(ssh, cmd, timeout=120):
    print(f"  >>> {cmd[:150]}")
    chan = ssh.get_transport().open_session()
    chan.exec_command(cmd)
    chan.settimeout(timeout)
    stdout = b""
    stderr = b""
    try:
        while not chan.exit_status_ready():
            if chan.recv_ready(): stdout += chan.recv(65536)
            if chan.recv_stderr_ready(): stderr += chan.recv_stderr(65536)
        while chan.recv_ready(): stdout += chan.recv(65536)
        while chan.recv_stderr_ready(): stderr += chan.recv_stderr(65536)
    except: pass
    out = stdout.decode('utf-8', errors='replace')
    err = stderr.decode('utf-8', errors='replace')
    if out.strip(): print(out.strip())
    if err.strip(): print(err.strip())
    return out, err

def main():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, 22, USER, PASS, timeout=20)
    print(f"Connected to {HOST}\n")

    # Step 1: Check line 248 on server
    print("===== 检查服务器上第247-250行 =====")
    run(ssh, f"sed -n '246,252p' {SRC}")
    
    # Step 2: Fix on server directly using sed
    print("\n===== 修复: 把 lambda 中的变量替换为 final 副本 =====")
    # Replace: new Thread(() -> sendDailyGreeting(openid, config)).start();
    # With:    String _oid = openid; GreetingConfig _cfg = config; new Thread(() -> sendDailyGreeting(_oid, _cfg)).start();
    
    # 更稳健的做法：用python在服务器上修复
    fix_script = '''
import re
with open("''' + SRC + '''", "r") as f:
    content = f.read()

old = 'new Thread(() -> sendDailyGreeting(openid, config)).start();'
new = '''String _oid = openid;
        GreetingConfig _cfg = config;
        new Thread(() -> sendDailyGreeting(_oid, _cfg)).start();'''

if old in content:
    content = content.replace(old, new)
    with open("''' + SRC + '''", "w") as f:
        f.write(content)
    print("FIXED")
else:
    print("NOT_FOUND - checking content around line 248:")
    lines = content.split("\\n")
    for i in range(244, min(252, len(lines))):
        print(f"  {i+1}: {lines[i]}")
'''
    run(ssh, f"python3 -c '{fix_script}'")
    
    # Step 3: Verify fix
    print("\n===== 验证修复 =====")
    run(ssh, f"sed -n '246,255p' {SRC}")
    
    # Step 4: Compile
    print("\n===== Maven编译 (JDK17) =====")
    print("  编译中...")
    out, _ = run(ssh,
        f"cd {PROJECT}/app/dinghong-api && export JAVA_HOME={JDK17} && export PATH={JDK17}/bin:$PATH && mvn clean package -DskipTests 2>&1", timeout=300)

    if "BUILD SUCCESS" not in out:
        print("  编译失败，最后15行:")
        lines = out.split('\n')
        for l in lines[-15:]:
            print(f"    {l}")
        ssh.close()
        return
    
    print("  >> BUILD SUCCESS!")

    # Step 5: Restart
    print("\n===== 重启服务 =====")
    out, _ = run(ssh, "ps aux | grep 'dinghong-api-1.0.0.jar' | grep -v grep | awk '{print $2}'")
    for pid in out.strip().split('\n'):
        pid = pid.strip()
        if pid:
            print(f"  停止 PID={pid}")
            run(ssh, f"kill -15 {pid}; sleep 1; kill -9 {pid} 2>/dev/null; echo DONE")

    time.sleep(2)
    run(ssh, f"cd {PROJECT}/app/dinghong-api && source {PROJECT}/.env 2>/dev/null; source {PROJECT}/env.sh 2>/dev/null; nohup java -jar target/dinghong-api-1.0.0.jar > /tmp/dinghong-api.log 2>&1 & echo NEW_PID=$!")
    time.sleep(5)

    out, _ = run(ssh, "ps aux | grep 'dinghong-api' | grep -v grep")
    if out.strip():
        print("\n" + "="*60)
        print("  *** 部署成功！***")
        print("="*60)
        print(f"  进程: {out.strip()}")
        print("  管理后台: http://admin.5q.lol/wechat-greeting.html")
    else:
        print("  启动失败，日志:")
        out, _ = run(ssh, "tail -20 /tmp/dinghong-api.log")
        print(out)

    ssh.close()

if __name__ == "__main__":
    main()