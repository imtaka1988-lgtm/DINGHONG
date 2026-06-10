import paramiko

s = paramiko.SSHClient()
s.set_missing_host_key_policy(paramiko.AutoAddPolicy())
s.connect('8.210.102.206', username='root', password='Taka888.', timeout=15)

def run(cmd):
    stdin, stdout, stderr = s.exec_command(cmd)
    return stdout.read().decode().strip()

print('=== 1. env.sh ===')
print(run('cat /data/dinghong/env.sh'))

print()
print('=== 2. Process ===')
print(run('ps aux | grep java | grep -v grep'))

print()
print('=== 3. Log tail ===')
print(run('tail -3 /data/dinghong/app/dinghong-api/api.log'))

print()
print('=== 4. WECHAT env in process ===')
pid = run('ps aux | grep "dinghong-api-1.0.0.jar" | grep -v grep | awk \'{print $2}\' | head -1')
if pid:
    print('PID: ' + pid)
    out = run('cat /proc/' + pid + '/environ 2>/dev/null | tr "\\000" "\\n" | grep WECHAT')
    print(out if out else '(not inherited)')
else:
    print('No Java process')

# 5. Test access_token fetch
print()
print('=== 5. Test WECHAT API ===')
out = run(
    'curl -s "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=wx02b7b2822291875d&secret=d27eef30f6a7bd26088b047a5738f5b9"'
)
print(out[:200])

s.close()