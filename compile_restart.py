import paramiko, time

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("8.210.102.206", 22, "root", "Taka888.", timeout=20)
print("Connected")

JDK17 = "/usr/lib/jvm/java-17-openjdk-17.0.19.0.10-1.0.2.1.al8.x86_64"

def run(cmd, t=300):
    print(f"  >>> {cmd[:140]}")
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

# Compile
print("\n===== Compile =====")
out, _ = run(f"cd /data/dinghong/app/dinghong-api && export JAVA_HOME={JDK17} && export PATH={JDK17}/bin:$PATH && mvn clean package -DskipTests 2>&1")

if "BUILD SUCCESS" not in out:
    print("COMPILE FAILED")
    lines = out.split('\n')
    for l in lines[-8:]: print(f"  {l}")
    ssh.close(); exit(1)

print("\n>> BUILD SUCCESS")

# Restart
print("\n===== Restart =====")
out, _ = run("ps aux | grep 'dinghong-api-1.0.0.jar' | grep -v grep | awk '{print $2}'")
for pid in out.strip().split('\n'):
    pid = pid.strip()
    if pid:
        print(f"  Stop PID={pid}")
        run(f"kill -15 {pid}; sleep 1; kill -9 {pid} 2>/dev/null; echo OK")

time.sleep(2)
run("cd /data/dinghong/app/dinghong-api && source /data/dinghong/.env 2>/dev/null; source /data/dinghong/env.sh 2>/dev/null; nohup java -jar target/dinghong-api-1.0.0.jar > /tmp/dinghong-api.log 2>&1 & echo NEW_PID=$!")
time.sleep(5)

out, _ = run("ps aux | grep 'dinghong-api' | grep -v grep")
if out.strip():
    print("\n===== DEPLOY OK =====")
    print(f"  Process: {out.strip()}")
else:
    print("\nSTART FAILED")
    out, _ = run("tail -15 /tmp/dinghong-api.log")
    print(out)

ssh.close()