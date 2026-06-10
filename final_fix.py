import paramiko, base64, time

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

# Write correct env.sh with WECHAT_APPID + WECHAT_SECRET
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
enc = base64.b64encode(env_content.encode()).decode()
run('echo "' + enc + '" | base64 -d > /data/dinghong/env.sh')
print('env.sh:')
print(run('cat /data/dinghong/env.sh'))

# Kill old process
run('pkill -9 -f dinghong-api 2>/dev/null')
time.sleep(3)

# Start new process with env vars properly exported
s.exec_command(
    'bash -c "'
    'export BAIDU_SEARCH_KEY='
    'bce-v3/ALTAK-ih3p5X52GOeg3cxjFCiZQ/'
    '072f8ea1531b4b6f339acb1d9c3335c495533bc7 && '
    'export DEEPSEEK_API_KEY='
    'sk-75cf57ad208742a0873a6de80d249c45 && '
    'export WECHAT_APPID=wx02b7b2822291875d && '
    'export WECHAT_SECRET='
    'd27eef30f6a7bd26088b047a5738f5b9 && '
    'cd /data/dinghong/app/dinghong-api && '
    'nohup java -jar target/dinghong-api-1.0.0.jar '
    '> api.log 2>&1 &"'
)
time.sleep(5)

# Verify
print()
pid = run(
    'ps aux | grep dinghong-api-1.0.0.jar | '
    'grep -v grep | awk \'{print $2}\' | head -1'
)
print('PID: ' + (pid if pid else 'NONE'))

# Check env in bash parent
if pid:
    ppid = run(
        'ps -o ppid= -p ' + pid + ' 2>/dev/null | tr -d " "'
    )
    if ppid:
        env_out = run(
            'cat /proc/' + ppid
            + '/environ 2>/dev/null | tr "\\000" "\\n" | '
            'grep WECHAT'
        )
        print('WECHAT env: ' + (env_out if env_out else 'MISSING'))

print()
print('Log: ' + run('tail -1 /data/dinghong/app/dinghong-api/api.log')[:180])

s.close()
print('\nDone')