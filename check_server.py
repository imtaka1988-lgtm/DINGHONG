import paramiko, warnings
warnings.filterwarnings('ignore')

c = paramiko.SSHClient()
c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
c.connect('8.210.102.206', 22, 'root', 'Taka888.', timeout=15, look_for_keys=False, allow_agent=False)

cmds = [
    ("/root 目录", "ls -la /root/"),
    ("Docker 容器", "docker ps -a --format 'table {{.Names}}\t{{.Status}}\t{{.Image}}\t{{.Ports}}' 2>/dev/null || echo 'no-docker'"),
    ("docker-compose", "find /root -name 'docker-compose.yml' -type f 2>/dev/null"),
    ("项目目录", "find /root/dinghong -maxdepth 2 -type d 2>/dev/null || echo 'no dinghong'"),
]

for title, cmd in cmds:
    print(f"\n=== {title} ===")
    stdin, stdout, stderr = c.exec_command(cmd)
    out = stdout.read().decode()
    err = stderr.read().decode()
    if out.strip():
        print(out)
    if err.strip():
        print("ERR:", err)

c.close()
print("\nDone.")
