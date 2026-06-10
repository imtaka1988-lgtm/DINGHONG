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

# 1. 后端 articleUrl 相关
print('=== 1. 后端 ArticleUrl 代码 ===')
out = run(
    'grep -rn "articleUrl\\|saveArticleUrl\\|ArticleUrl" '
    '/data/dinghong/app/dinghong-api/src/main/java/ 2>/dev/null'
)
print(out if out else '  无匹配')

# 2. DB 表结构
print('\n=== 2. article_task 表 article_url 列 ===')
out = run(
    "docker exec dinghong-mysql mysql -uroot "
    "-p'DingHong@2026' dinghong "
    '-e "DESCRIBE article_task;" 2>&1'
)
for line in out.split('\n'):
    if 'article_url' in line.lower() or 'url' in line.lower() or 'Field' in line:
        print('  ' + line)

# 3. DB 中 article_url 是否有值
print('\n=== 3. 有 article_url 的记录数 ===')
out = run(
    "docker exec dinghong-mysql mysql -uroot "
    "-p'DingHong@2026' dinghong "
    '-e "SELECT COUNT(*) FROM article_task WHERE article_url IS NOT NULL AND article_url != \'\';" 2>&1'
)
print('  ' + out)

# 4. 前端代码
print('\n=== 4. articles.html 中 articleUrl 相关 ===')
out = run(
    'cat -n /data/dinghong/admin/articles.html '
    '2>/dev/null | grep -i "articleurl\\|savearticle\\|article url" '
    '| head -20'
)
print(out if out else '  无匹配')

s.close()