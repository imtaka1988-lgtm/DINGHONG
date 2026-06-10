import paramiko, os, base64, time

s = paramiko.SSHClient()
s.set_missing_host_key_policy(paramiko.AutoAddPolicy())
s.connect('8.210.102.206', username='root', password='Taka888.', timeout=15)

def run(cmd):
    stdin, stdout, stderr = s.exec_command(cmd)
    return stdout.read().decode().strip()

local = os.path.join(
    r'c:\Users\Administrator\Desktop\顶红公众号',
    'app', 'dinghong-api', 'src', 'main', 'java',
    'com', 'dinghong', 'controller', 'editor', 'ArticleController.java'
)
svr = '/data/dinghong/app/dinghong-api/src/main/java/com/dinghong/controller/editor/ArticleController.java'

c = open(local, 'rb').read()
enc = base64.b64encode(c).decode()
run('echo "' + enc + '" | base64 -d > "' + svr + '"')

# Verify
stdin, stdout, stderr = s.exec_command('wc -c < "' + svr + '"')
s_size = int(stdout.read().decode().strip())
ok = abs(s_size - len(c)) < 5
print('Upload: ' + ('OK' if ok else 'FAIL') + ' local=' + str(len(c)) + ' svr=' + str(s_size))

# Verify code order
content = run('sed -n "230,240p" ' + svr)
print('Server lines 230-240:')
print(content)
has_db_first = 'dbSport = findSportTypeFromMatchLive' in content
print('match_live BEFORE keyword: ' + ('YES' if has_db_first else 'NO'))

# Rebuild and restart
print()
print('Rebuilding...')
run('pkill -9 -f dinghong-api 2>/dev/null')
time.sleep(2)
run(
    'cd /data/dinghong/app/dinghong-api '
    '&& export $(cat /data/dinghong/env.sh | xargs) '
    '&& JAVA_HOME=/usr/lib/jvm/'
    'java-17-openjdk-17.0.19.0.10-1.0.2.1.al8.x86_64 '
    'mvn clean package -DskipTests -q 2>&1'
)
time.sleep(6)
s.exec_command('bash /data/dinghong/start-api.sh')
time.sleep(5)
print('PID: ' + run('ps aux | grep dinghong-api-1.0.0.jar | grep -v grep | awk \'{print $2}\' | head -1'))
print('Log: ' + run('tail -1 /data/dinghong/app/dinghong-api/api.log')[:180])
s.close()