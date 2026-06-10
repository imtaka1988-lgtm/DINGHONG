import paramiko

s = paramiko.SSHClient()
s.set_missing_host_key_policy(paramiko.AutoAddPolicy())
s.connect('8.210.102.206', username='root', password='Taka888.', timeout=15)

def run(cmd):
    stdin, stdout, stderr = s.exec_command(cmd)
    return stdout.read().decode().strip()

# 1. Verify Baidu key check
print('1. BaiduSearchService key check:')
out = run('grep -A2 "Key未配置" '
          '/data/dinghong/app/dinghong-api/src/main/java/com/dinghong/'
          'service/search/BaiduSearchService.java | head -5')
print(out)

# 2. Verify env.sh
print('\n2. env.sh:')
print(run('cat /data/dinghong/env.sh'))

# 3. Verify process env
print('\n3. Java process env vars:')
pid = run('ps aux | grep "dinghong-api-1.0.0.jar" | grep -v grep | '
          'awk \'{print $2}\' | head -1')
if pid:
    env_out = run('cat /proc/' + pid + '/environ 2>/dev/null | '
                  'tr "\\000" "\\n" | grep -E "BAIDU|DEEPSEEK|WECHAT"')
    print(env_out if env_out else '(env vars may not be inherited)')
else:
    print('No process found')

# 4. Verify Odds API (盘口) config in application.yml
print('\n4. Odds API config:')
print(run('grep -A10 "odds:" '
          '/data/dinghong/app/dinghong-api/src/main/resources/application.yml'))

# 5. process status
print('\n5. Service status:')
print(run('tail -2 /data/dinghong/app/dinghong-api/api.log'))

s.close()
print('\nAll checks done.')