import paramiko, os, base64, time

s = paramiko.SSHClient()
s.set_missing_host_key_policy(paramiko.AutoAddPolicy())
s.connect('8.210.102.206', username='root', password='Taka888.', timeout=15)

def run(cmd):
    stdin, stdout, stderr = s.exec_command(cmd)
    return stdout.read().decode().strip()

local = r'c:\Users\Administrator\Desktop\顶红公众号'

# Correct Linux server paths
uploads = [
    (os.path.join(local, 'app', 'dinghong-api', 'src', 'main', 'java',
                  'com', 'dinghong', 'service', 'search', 'BaiduSearchService.java'),
     '/data/dinghong/app/dinghong-api/src/main/java/com/dinghong/service/search/BaiduSearchService.java'),
    (os.path.join(local, 'app', 'dinghong-api', 'src', 'main', 'java',
                  'com', 'dinghong', 'service', 'research', 'MatchResearchService.java'),
     '/data/dinghong/app/dinghong-api/src/main/java/com/dinghong/service/research/MatchResearchService.java'),
]

for local_path, server_path in uploads:
    content = open(local_path, 'rb').read()
    enc = base64.b64encode(content).decode()
    run('echo "' + enc + '" | base64 -d > "' + server_path + '"')
    stdin, stdout, stderr = s.exec_command('wc -c < "' + server_path + '"')
    svr = int(stdout.read().decode().strip())
    fname = os.path.basename(local_path)
    print('[OK]' if abs(svr - len(content)) < 5 else '[FAIL]',
          fname, 'local=' + str(len(content)), 'svr=' + str(svr))

# Verify server file content
print()
print('Baidu key check on server:')
print(run(
    'grep -A1 "Key未配置" /data/dinghong/app/dinghong-api/src/main/java/'
    'com/dinghong/service/search/BaiduSearchService.java | head -3'))

print()
print('MatchResearch null check on server:')
print(run(
    'grep "if (raw" /data/dinghong/app/dinghong-api/src/main/java/'
    'com/dinghong/service/research/MatchResearchService.java | head -3'))

# Kill, build, restart with env
print()
print('Rebuilding...')
run('pkill -f dinghong-api 2>/dev/null; sleep 2')
run('export $(grep -v "^#" /data/dinghong/env.sh | xargs) && cd /data/dinghong/app/dinghong-api && JAVA_HOME=/usr/lib/jvm/java-17-openjdk-17.0.19.0.10-1.0.2.1.al8.x86_64 mvn clean package -DskipTests -q 2>&1')
time.sleep(8)
jar = run('ls /data/dinghong/app/dinghong-api/target/dinghong-api-1.0.0.jar 2>/dev/null')
if jar:
    print('Build OK, starting...')
    s.exec_command(
        'bash -c "source /data/dinghong/env.sh && '
        'cd /data/dinghong/app/dinghong-api && '
        'nohup java -jar target/dinghong-api-1.0.0.jar > api.log 2>&1 &"'
    )
    time.sleep(5)
    print('Log: ' + run('tail -1 /data/dinghong/app/dinghong-api/api.log')[:180])
else:
    print('Build FAILED')

s.close()