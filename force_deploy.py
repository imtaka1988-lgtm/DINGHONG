import paramiko, os, base64, time

s = paramiko.SSHClient()
s.set_missing_host_key_policy(paramiko.AutoAddPolicy())
s.connect('8.210.102.206', username='root', password='Taka888.', timeout=15)

def run(cmd):
    stdin, stdout, stderr = s.exec_command(cmd)
    out = stdout.read().decode().strip()
    err = stderr.read().decode().strip()
    if err and 'Warning' not in err and 'secure' not in err:
        print('  ERR: ' + err[:150])
    return out

# Upload
local = os.path.join(
    r'c:\Users\Administrator\Desktop\顶红公众号',
    'app', 'dinghong-api', 'src', 'main', 'java',
    'com', 'dinghong', 'controller', 'editor', 'ArticleController.java'
)
sp = '/data/dinghong/app/dinghong-api/src/main/java/com/dinghong/controller/editor/ArticleController.java'
c = open(local, 'rb').read()
run('echo "' + base64.b64encode(c).decode() + '" | base64 -d > "' + sp + '"')
print('Upload: ' + os.path.basename(local) + ' (' + str(len(c)) + 'b)')

# Kill
run('pkill -9 -f dinghong-api 2>/dev/null')
time.sleep(3)
alive = run('ps aux | grep java | grep -v grep')
print('After kill: ' + ('STILL ALIVE!' if alive else 'clean'))

# Build
run(
    'cd /data/dinghong/app/dinghong-api '
    '&& export $(cat /data/dinghong/env.sh | xargs) '
    '&& JAVA_HOME=/usr/lib/jvm/java-17-openjdk-17.0.19.0.10-1.0.2.1.al8.x86_64 '
    'mvn clean package -DskipTests -q 2>&1'
)
time.sleep(7)
jar = run('ls /data/dinghong/app/dinghong-api/target/dinghong-api-1.0.0.jar 2>/dev/null')
if not jar:
    print('BUILD FAILED')
    s.close()
    exit()

print('Build OK')

# Verify new method in compiled class
has_new = run(
    'strings /data/dinghong/app/dinghong-api/target/dinghong-api-1.0.0.jar '
    '2>/dev/null | grep "findSportTypeFromMatchLive" | head -1'
)
print('New code in JAR: ' + ('YES' if has_new else 'NO'))

# Start
s.exec_command('bash /data/dinghong/start-api.sh')
time.sleep(5)

pid = run(
    'ps aux | grep dinghong-api-1.0.0.jar | grep -v grep '
    '| awk \'{print $2}\' | head -1'
)
print('New PID: ' + (pid if pid else 'NONE'))
if pid:
    log_tail = run('tail -2 /data/dinghong/app/dinghong-api/api.log')
    print('Log: ' + log_tail[:180])

s.close()