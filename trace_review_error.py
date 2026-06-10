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

# 1. 搜索"百度联网搜索Key"这个精确错误消息
print('=== 1. 搜索错误消息来源 ===')
out = run(
    'grep -rn "百度联网" '
    '/data/dinghong/app/dinghong-api/src/main/java/ 2>/dev/null'
)
print(out)

# 2. 读 MatchResearchService.java 全文
print('\n=== 2. MatchResearchService.java ===')
out = run(
    'cat /data/dinghong/app/dinghong-api/src/main/java/com/dinghong/'
    'service/research/MatchResearchService.java 2>/dev/null'
)
print(out[:3000])

# 3. 读 EditorService.java 中 REVIEW 相关部分
print('\n=== 3. EditorService writeArticle REVIEW分支 ===')
out = run(
    'grep -n -A5 "REVIEW" '
    '/data/dinghong/app/dinghong-api/src/main/java/com/dinghong/'
    'service/editor/EditorService.java 2>/dev/null | head -40'
)
print(out)

# 4. application.yml
print('\n=== 4. application.yml ===')
out = run(
    'cat /data/dinghong/app/dinghong-api/src/main/resources/application.yml'
)
print(out)

s.close()