import paramiko

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

# 1. 检查百度API文件
print('1. 文件存在检查')
server_files = run(
    'find /data/dinghong/app/dinghong-api/src/main/java '
    '-name "*.java" 2>/dev/null | sort'
)
# Check key files
for keyword in ['Baidu', 'MatchResearch', 'EditorService',
                'RulePrompt', 'WechatDraft', 'OddsFetch']:
    found = False
    for line in server_files.split('\n'):
        if keyword in line:
            if not found:
                print(f'  [{keyword}] 存在')
                found = True
            print(f'    {line}')

# 2. application.yml
print('\n2. application.yml 内容')
yml = run(
    'cat /data/dinghong/app/dinghong-api/src/main/resources/application.yml'
)
print(yml)

# 3. 查看review_blocked相关代码
print('\n3. review_blocked 报错位置')
out = run(
    'grep -rn "review_blocked" '
    '/data/dinghong/app/dinghong-api/src/main/java/ 2>/dev/null'
)
print(out)

# 4. 查看ArticleController中的复盘相关逻辑
print('\n4. 复盘生成相关controller')
out = run(
    'grep -n "复盘\|review\|Review" '
    '/data/dinghong/app/dinghong-api/src/main/java/com/dinghong/'
    'controller/editor/ArticleController.java 2>/dev/null'
    ' | head -30'
)
print(out)

# 5. 检查本地文件
print('\n5. 本地BaiduSearchService.java')
import os
local_baidu = r'c:\Users\Administrator\Desktop\顶红公众号\app\dinghong-api\src\main\java\com\dinghong\service\search\BaiduSearchService.java'
if os.path.exists(local_baidu):
    print(f'  本地存在 ({os.path.getsize(local_baidu)} bytes)')
else:
    print('  本地缺失!')

s.close()