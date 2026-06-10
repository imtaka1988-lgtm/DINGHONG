import paramiko
import sys

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('8.210.102.206', username='root', password='Taka888.', timeout=10)

def run(cmd):
    stdin, stdout, stderr = ssh.exec_command(cmd)
    out = stdout.read().decode().strip()
    err = stderr.read().decode().strip()
    if err:
        print('  ERR: ' + err[:150])
    return out

# 1. Controller
print('1. Controller files:')
for line in run('find /data/dinghong/app/dinghong-api/src/main/java '
                '-name "*.java" 2>/dev/null | sort').split('\n'):
    print('   ' + line)

# 2. Target dir
print('\n2. Target dir:')
print(run('ls -la /data/dinghong/app/dinghong-api/target/ 2>/dev/null'))

# 3. Maven status
print('\n3. Maven:')
print(run('ps aux | grep mvn | grep -v grep') or 'Not running')

# 4. /data/dinghong top
print('\n4. /data/dinghong top:')
print(run('ls -la /data/dinghong/ | grep -v "^total"'))

# 5. Disk
print('\n5. Disk:')
print(run('df -h /data | tail -1'))

ssh.close()