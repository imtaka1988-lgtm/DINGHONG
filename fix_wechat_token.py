import paramiko
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
        print('  ERR: ' + err[:150])
    return out

# 1. env.sh
print('1. env.sh:')
print(run('cat /data/dinghong/env.sh'))

# 2. Process
pid = run('ps aux | grep "dinghong-api-1.0.0.jar" | grep -v grep | '
          'awk \'{print $2}\' | head -1')
print('\n2. PID: ' + (pid if pid else 'NONE'))

# 3. Kill old
run('pkill -9 -f dinghong-api 2>/dev/null')
time.sleep(3)
print('3. Killed old process')

# 4. Write proper start-api.sh with all env vars inline
start_script = (
    '#!/bin/bash\n'
    'export BAIDU_SEARCH_KEY='
    '"bce-v3/ALTAK-ih3p5X52GOeg3cxjFCiZQ/'
    '072f8ea1531b4b6f339acb1d9c3335c495533bc7"\n'
    'export DEEPSEEK_API_KEY='
    '"sk-75cf57ad208742a0873a6de80d249c45"\n'
    'export WECHAT_APPID="wx02b7b2822291875d"\n'
    'export WECHAT_SECRET='
    '"d27eef30f6a7bd26088b047a5738f5b9"\n'
    'cd /data/dinghong/app/dinghong-api\n'
    'nohup java -jar target/dinghong-api-1.0.0.jar '
    '> api.log 2>&1 &\n'
)
enc = base64.b64encode(start_script.encode()).decode()
run('echo "' + enc + '" | base64 -d > /data/dinghong/start-api.sh')
run('chmod +x /data/dinghong/start-api.sh')

# Also update env.sh consistently
env_content = (
    'export BAIDU_SEARCH_KEY='
    '"bce-v3/ALTAK-ih3p5X52GOeg3cxjFCiZQ/'
    '072f8ea1531b4b6f339acb1d9c3335c495533bc7"\n'
    'export DEEPSEEK_API_KEY='
    '"sk-75cf57ad208742a0873a6de80d249c45"\n'
    'export WECHAT_APPID="wx02b7b2822291875d"\n'
    'export WECHAT_SECRET='
    '"d27eef30f6a7bd26088b047a5738f5b9"\n'
)
enc2 = base64.b64encode(env_content.encode()).decode()
run('echo "' + enc2 + '" | base64 -d > /data/dinghong/env.sh')

print('4. Updated env.sh + start-api.sh')

# 5. Start using start-api.sh
s.exec_command('bash /data/dinghong/start-api.sh')
time.sleep(5)

# 6. Verify
pid2 = run('ps aux | grep "dinghong-api-1.0.0.jar" | grep -v grep | '
           'awk \'{print $2}\' | head -1')
print('\n5. New PID: ' + (pid2 if pid2 else 'NONE'))

if pid2:
    ppid = run('ps -o ppid= -p ' + pid2 + ' | tr -d " "')
    print('   PPID: ' + ppid)
    env_bash = run('cat /proc/' + ppid + '/environ 2>/dev/null | '
                   'tr "\\000" "\\n" | grep WECHAT')
    print('   Bash WECHAT env: ' + (env_bash if env_bash else 'MISSING'))
    env_java = run('cat /proc/' + pid2 + '/environ 2>/dev/null | '
                   'tr "\\000" "\\n" | grep WECHAT')
    print('   Java WECHAT env: ' + (env_java if env_java else 'MISSING'))

# 7. Test WECHAT API directly
print('\n6. Test WECHAT token fetch:')
out = run(
    'curl -s '
    '"https://api.weixin.qq.com/cgi-bin/token'
    '?grant_type=client_credential'
    '&appid=wx02b7b2822291875d'
    '&secret=d27eef30f6a7bd26088b047a5738f5b9"'
)
print('   ' + out[:150])

print('\n7. Log tail:')
print(run('tail -2 /data/dinghong/app/dinghong-api/api.log')[:200])

s.close()
print('\nDone')