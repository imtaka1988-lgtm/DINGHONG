import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('8.210.102.206', username='root', password='Taka888.', timeout=10)

# 查看 /data/dinghong 完整目录树
stdin, stdout, stderr = ssh.exec_command('find /data/dinghong -maxdepth 5 -not -path "*/logs/*" -not -path "*/node_modules/*" -not -path "*/.git/*" | sort')
out = stdout.read().decode()
print('=== /data/dinghong 目录树 ===')
print(out)

print('\n=== /data/顶红公众号 目录树 ===')
stdin, stdout, stderr = ssh.exec_command('find /data/顶红公众号 -maxdepth 5 2>/dev/null | sort')
out = stdout.read().decode()
print(out)

# 查看 /data/dinghong/app 详细
print('\n=== /data/dinghong/app 详细 ===')
stdin, stdout, stderr = ssh.exec_command('ls -laR /data/dinghong/app/ 2>/dev/null')
out = stdout.read().decode()
print(out)

# 查看 /data/dinghong/nginx
print('\n=== /data/dinghong/nginx ===')
stdin, stdout, stderr = ssh.exec_command('ls -la /data/dinghong/nginx/ 2>/dev/null')
out = stdout.read().decode()
print(out)

# 查看运行的jar/进程
print('\n=== Java进程 ===')
stdin, stdout, stderr = ssh.exec_command('ps aux | grep java | grep -v grep')
out = stdout.read().decode()
print(out)

# 查看docker compose或部署方式
print('\n=== Docker compose查找 ===')
stdin, stdout, stderr = ssh.exec_command('find / -maxdepth 5 -name "docker-compose*" -o -name "Dockerfile" 2>/dev/null | head -20')
out = stdout.read().decode()
print(out)

ssh.close()