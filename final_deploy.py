import paramiko, os, base64, time

s = paramiko.SSHClient()
s.set_missing_host_key_policy(paramiko.AutoAddPolicy())
s.connect('8.210.102.206', username='root', password='Taka888.', timeout=15)

local = r'c:\Users\Administrator\Desktop\顶红公众号'

# Upload BaiduSearchService
f1 = 'app\\dinghong-api\\src\\main\\java\\com\\dinghong\\service\\search\\BaiduSearchService.java'
c1 = open(os.path.join(local, f1), 'rb').read()
s.exec_command(
    'echo "' + base64.b64encode(c1).decode() +
    '" | base64 -d > /data/dinghong/' + f1
)

# Upload MatchResearchService
f2 = 'app\\dinghong-api\\src\\main\\java\\com\\dinghong\\service\\research\\MatchResearchService.java'
c2 = open(os.path.join(local, f2), 'rb').read()
s.exec_command(
    'echo "' + base64.b64encode(c2).decode() +
    '" | base64 -d > /data/dinghong/' + f2
)

print('Upload done. Building...')

# Kill + Build + Start
s.exec_command('pkill -f dinghong-api 2>/dev/null; sleep 2')
s.exec_command(
    'cd /data/dinghong/app/dinghong-api '
    '&& source /data/dinghong/env.sh '
    '&& JAVA_HOME=/usr/lib/jvm/java-17-openjdk-17.0.19.0.10-1.0.2.1.al8.x86_64 '
    'mvn clean package -DskipTests -q 2>&1'
)
time.sleep(8)

# Check jar
stdin, stdout, stderr = s.exec_command(
    'ls /data/dinghong/app/dinghong-api/target/dinghong-api-1.0.0.jar 2>/dev/null'
)
jar = stdout.read().decode().strip()
if jar:
    print('Build OK, starting...')
    s.exec_command(
        'cd /data/dinghong/app/dinghong-api '
        '&& source /data/dinghong/env.sh '
        '&& nohup java -jar target/dinghong-api-1.0.0.jar > api.log 2>&1 &'
    )
    time.sleep(5)
    stdin2, stdout2, stderr2 = s.exec_command(
        'grep "Key未配置" /data/dinghong/app/dinghong-api/src/main/java/com/dinghong/service/search/BaiduSearchService.java | head -2'
    )
    print(stdout2.read().decode())
    stdin3, stdout3, stderr3 = s.exec_command(
        'tail -1 /data/dinghong/app/dinghong-api/api.log'
    )
    print(stdout3.read().decode()[:150])
else:
    print('Build FAILED')

s.close()