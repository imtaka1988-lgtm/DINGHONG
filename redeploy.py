import paramiko
import os
import base64
import time

s = paramiko.SSHClient()
s.set_missing_host_key_policy(paramiko.AutoAddPolicy())
s.connect('8.210.102.206', username='root', password='Taka888.', timeout=15)

def run(cmd):
    stdin, stdout, stderr = s.exec_command(cmd)
    out = stdout.read().decode().strip()
    err = stderr.read().decode().strip()
    if err:
        print('  ERR: ' + err[:200])
    return out

local_base = r'c:\Users\Administrator\Desktop\顶红公众号'

# Upload files
files = [
    ('app\\dinghong-api\\src\\main\\java\\com\\dinghong\\service\\search\\BaiduSearchService.java',
     '/data/dinghong/app/dinghong-api/src/main/java/com/dinghong/service/search/BaiduSearchService.java'),
    ('app\\dinghong-api\\src\\main\\java\\com\\dinghong\\service\\research\\MatchResearchService.java',
     '/data/dinghong/app/dinghong-api/src/main/java/com/dinghong/service/research/MatchResearchService.java'),
]

for local_rel, server_path in files:
    local_path = os.path.join(local_base, local_rel)
    content = open(local_path, 'rb').read()
    encoded = base64.b64encode(content).decode()
    d = os.path.dirname(server_path)
    run('mkdir -p "' + d + '"')
    run('echo "' + encoded + '" | base64 -d > "' + server_path + '"')
    fname = os.path.basename(local_path)
    stdin, stdout, stderr = s.exec_command('wc -c < "' + server_path + '"')
    svr_size = int(stdout.read().decode().strip())
    ok = abs(svr_size - len(content)) < 5
    print('  ' + ('[OK]' if ok else '[FAIL]') + ' ' + fname
          + ' local=' + str(len(content)) + ' svr=' + str(svr_size))

# Kill all old processes
print()
print('Killing old Java processes...')
run('pkill -f dinghong-api 2>/dev/null || true')
time.sleep(3)

# Check no java running
proc = run('ps aux | grep java | grep -v grep')
print('Java after kill: ' + (proc[:100] if proc else 'NONE (clean)'))

# Build
print()
print('Building...')
build_out = run(
    'cd /data/dinghong/app/dinghong-api '
    '&& JAVA_HOME=/usr/lib/jvm/java-17-openjdk-17.0.19.0.10-1.0.2.1.al8.x86_64 '
    'mvn clean package -DskipTests 2>&1 | tail -5'
)
print(build_out)

# Check jar
jar = run(
    'ls -la '
    '/data/dinghong/app/dinghong-api/target/dinghong-api-1.0.0.jar '
    '2>/dev/null'
)
if 'dinghong-api' in jar:
    print('JAR OK: ' + jar.split()[-1] if jar.split() else '')

    # Start new
    print('Starting...')
    s.exec_command(
        'cd /data/dinghong/app/dinghong-api '
        '&& nohup java -jar target/dinghong-api-1.0.0.jar '
        '> api.log 2>&1 &'
    )
    time.sleep(6)

    # Verify
    proc = run('ps aux | grep dinghong-api | grep -v grep')
    print('Process: ' + (proc[:120] if proc else 'NOT DETECTED'))

    # Log tail
    log = run('tail -5 /data/dinghong/app/dinghong-api/api.log 2>/dev/null')
    print('Log tail:')
    print(log)
else:
    print('BUILD FAILED')

s.close()