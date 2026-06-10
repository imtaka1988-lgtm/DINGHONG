#!/usr/bin/env python3
import paramiko, time

HOST, USER, PASS = "8.210.102.206", "root", "Taka888."
PROJ, JDK17 = "/data/dinghong", "/usr/lib/jvm/java-17-openjdk-17.0.19.0.10-1.0.2.1.al8.x86_64"
SRC = PROJ + "/app/dinghong-api/src/main/java/com/dinghong/controller/wechat/WechatController.java"

def run(ssh, cmd, t=120):
    print("  >>> " + cmd[:140])
    c = ssh.get_transport().open_session()
    c.exec_command(cmd); c.settimeout(t)
    out, err = b"", b""
    while not c.exit_status_ready():
        if c.recv_ready(): out += c.recv(65536)
        if c.recv_stderr_ready(): err += c.recv_stderr(65536)
    while c.recv_ready(): out += c.recv(65536)
    while c.recv_stderr_ready(): err += c.recv_stderr(65536)
    o = out.decode('utf-8', errors='replace')
    e = err.decode('utf-8', errors='replace')
    if o.strip(): print(o.strip())
    if e.strip(): print(e.strip())
    return o, e

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(HOST, 22, USER, PASS, timeout=20)
print(f"Connected to {HOST}\n")

# 1. See what's at line 246-252
print("===== 当前服务器第246-252行 =====")
run(ssh, f"sed -n '246,252p' {SRC}")

# 2. Fix: replace the lambda line using sed
print("\n===== 修复中 =====")
cmd = f"""
sed -i 's/new Thread(() -> sendDailyGreeting(openid, config)).start();/String _oid = openid;\\n        GreetingConfig _cfg = config;\\n        new Thread(() -> sendDailyGreeting(_oid, _cfg)).start();/' {SRC}
echo "SED_DONE"
"""
run(ssh, cmd)

# 3. Verify
print("\n===== 修复后第246-255行 =====")
run(ssh, f"sed -n '246,255p' {SRC}")

# 4. Compile
print("\n===== 编译 (JDK17) =====")
out, _ = run(ssh, f"cd {PROJ}/app/dinghong-api && export JAVA_HOME={JDK17} && export PATH={JDK17}/bin:$PATH && mvn clean package -DskipTests 2>&1", t=300)

if "BUILD SUCCESS" not in out:
    print("\n*** 编译失败 ***")
    lines = out.split('\n')
    for l in lines[-10:]:
        print(f"  {l}")
    ssh.close(); exit(1)

print("\n>> BUILD SUCCESS!")

# 5. Restart
print("\n===== 重启服务 =====")
out, _ = run(ssh, "ps aux | grep 'dinghong-api-1.0.0.jar' | grep -v grep | awk '{print $2}'")
for pid in out.strip().split('\n'):
    pid = pid.strip()
    if pid:
        print(f"  停止 PID={pid}")
        run(ssh, f"kill -15 {pid}; sleep 1; kill -9 {pid} 2>/dev/null; echo OK")
time.sleep(2)
run(ssh, f"cd {PROJ}/app/dinghong-api && source {PROJ}/.env 2>/dev/null; source {PROJ}/env.sh 2>/dev/null; nohup java -jar target/dinghong-api-1.0.0.jar > /tmp/dinghong-api.log 2>&1 & echo NEW_PID=$!")
time.sleep(5)
out, _ = run(ssh, "ps aux | grep 'dinghong-api' | grep -v grep")

if out.strip():
    print("\n" + "="*60)
    print("  *** 部署成功! ***")
    print("="*60)
    print(f"  进程: {out.strip()}")
    print("  管理后台: http://admin.5q.lol/wechat-greeting.html")
else:
    print("\n  启动失败，日志:")
    out, _ = run(ssh, "tail -15 /tmp/dinghong-api.log")
    print(out)

ssh.close()