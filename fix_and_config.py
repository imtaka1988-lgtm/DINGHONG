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

# ======= 1. 恢复 BaiduSearchService.java =======
print('=== 1. Restore BaiduSearchService.java ===')
baidu_path_server = '/data/dinghong/app/dinghong-api/src/main/java/com/dinghong/service/search/BaiduSearchService.java'
# Read current server file
current = run('cat "' + baidu_path_server + '"')
if 'Key未配置，跳过百度搜索' in current:
    # Restore: change "跳过百度搜索" back to original
    run(
        "sed -i 's/Key未配置，跳过百度搜索/Key未配置/g' " + baidu_path_server
    )
    # Restore: change 'return "";' back to 'return "百度搜索Key未配置。";' in the key check block
    run(
        "sed -i '/Key未配置$/s/return \"\";/return \"百度搜索Key未配置。\";/' "
        + baidu_path_server
    )
    print('  Restored BaiduSearchService.java')
else:
    print('  Already restored or unchanged')

# ======= 2. 恢复 MatchResearchService.java =======
print('\n=== 2. Restore MatchResearchService.java ===')
research_path_server = '/data/dinghong/app/dinghong-api/src/main/java/com/dinghong/service/research/MatchResearchService.java'
current2 = run('grep "if (raw == null) return true;" "' + research_path_server + '"')
if 'raw == null' in current2:
    # Change back to original: if (raw == null || raw.trim().isEmpty()) return true;
    run(
        "sed -i 's/if (raw == null) return true;/if (raw == null || raw.trim().isEmpty()) return true;/' "
        + research_path_server
    )
    print('  Restored isSearchUnavailable')
else:
    print('  Already restored or unchanged')

# ======= 3. 配置 API Keys 到环境变量 =======
print('\n=== 3. Configure API Keys ===')

# Check current env.sh
print('Current env.sh:')
print(run('cat /data/dinghong/env.sh'))

# Write new env.sh with all three API keys
env_content = """export BAIDU_SEARCH_KEY="BAIDU_SEARCH_KEY_PLACEHOLDER"
export DEEPSEEK_API_KEY="DEEPSEEK_API_KEY_PLACEHOLDER"
export WECHAT_APP_SECRET="d27eef30f6a7bd26088b047a5738f5b9"
"""
encoded = base64.b64encode(env_content.encode()).decode()
run('echo "' + encoded + '" | base64 -d > /data/dinghong/env.sh')
print('  Updated /data/dinghong/env.sh')

# Also source it in start-api.sh
print('\nCurrent start-api.sh:')
start_content = run('cat /data/dinghong/start-api.sh')

# Update start-api.sh to source env.sh before java
new_start = "#!/bin/bash\nsource /data/dinghong/env.sh\ncd /data/dinghong/app/dinghong-api\nnohup java -jar target/dinghong-api-1.0.0.jar > api.log 2>&1 &"
encoded2 = base64.b64encode(new_start.encode()).decode()
run('echo "' + encoded2 + '" | base64 -d > /data/dinghong/start-api.sh')
run('chmod +x /data/dinghong/start-api.sh')
print('  Updated start-api.sh')

# ======= 4. Check application.yml for WeChat and Odds =======
print('\n=== 4. Check application.yml ===')
yml = run('cat /data/dinghong/app/dinghong-api/src/main/resources/application.yml')
print(yml)

# Check if WeChat config needs WeChatAppSecret
print('\n=== 5. Check WeChat config usage ===')
wechat_config = run(
    'grep -rn "WECHAT_APP_SECRET\\|wechat.*secret\\|appSecret" '
    '/data/dinghong/app/dinghong-api/src/main/java/ '
    '2>/dev/null | grep -v ".bak" | head -10'
)
print(wechat_config if wechat_config else '  No WECHAT_APP_SECRET env var usage found')

# ======= 6. Verify restorations =======
print('\n=== 6. Verify ===')
print('BaiduSearchService key check:')
print(run('grep -A2 "Key未配置" ' + baidu_path_server + ' | head -5'))
print()
print('MatchResearchService null check:')
print(run('grep "if (raw" ' + research_path_server + ' | head -3'))

# ======= 7. Rebuild and restart =======
print('\n=== 7. Rebuild & Restart ===')
# Kill all
run('pkill -f dinghong-api 2>/dev/null || true')
time.sleep(2)

# Build with env
run(
    'cd /data/dinghong/app/dinghong-api '
    '&& source /data/dinghong/env.sh '
    '&& JAVA_HOME=/usr/lib/jvm/java-17-openjdk-17.0.19.0.10-1.0.2.1.al8.x86_64 '
    'mvn clean package -DskipTests -q 2>&1'
)

jar = run('ls /data/dinghong/app/dinghong-api/target/dinghong-api-1.0.0.jar 2>/dev/null')
if jar:
    print('  Build OK')
    # Start with env
    s.exec_command(
        'cd /data/dinghong/app/dinghong-api '
        '&& source /data/dinghong/env.sh '
        '&& nohup java -jar target/dinghong-api-1.0.0.jar > api.log 2>&1 &'
    )
    time.sleep(5)

    # Verify env is loaded
    proc = run('ps aux | grep dinghong-api | grep -v grep')
    print('  Process: ' + (proc[:120] if proc else 'NOT DETECTED'))

    # Check log for Baidu key
    baidu_check = run(
        'grep "BAIDU_SEARCH" '
        '/data/dinghong/app/dinghong-api/api.log 2>/dev/null | head -3'
    )
    print('  Baidu search logs:')
    print(' ' + (baidu_check if baidu_check else '(no requests yet)'))

    # Tail log
    log = run('tail -3 /data/dinghong/app/dinghong-api/api.log 2>/dev/null')
    print('  Log tail:')
    for line in log.split('\n'):
        print(' ' + line[:120])
else:
    print('  Build FAILED')

s.close()
print('\nDone!')