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

# 1. 检查admin端复盘相关页面
print('=== 1. 复盘相关前端代码 ===')
for page in ['wechat-greeting.html', 'articles.html']:
    out = run(
        'grep -n "relatedArticleId\\|related_article\\|复盘" '
        f'/data/dinghong/admin/{page} 2>/dev/null | head -15'
    )
    print(f'\n{page}:')
    print(out if out else '  无匹配')

# 2. 查数据库最近复盘和预测
print('\n=== 2. 数据库最近文章 ===')

sql1 = (
    "SELECT id, title, article_category, related_article_id "
    "FROM article_task "
    "ORDER BY id DESC LIMIT 10;"
)
out = run(
    'docker exec dinghong-mysql mysql -uroot '
    "-p'DingHong@2026' dinghong -e \"" + sql1 + '" 2>&1'
)
print(out)

# 3. 查看复盘相关API端点
print('\n=== 3. API中的复盘端点 ===')
out = run(
    'grep -n "PostMapping\\|RequestMapping\\|REVIEW\" '
    '/data/dinghong/app/dinghong-api/src/main/java/com/dinghong/'
    'controller/editor/ArticleController.java 2>/dev/null | head -15'
)
print(out)

# 4. 检查greeting页面中的generate调用
print('\n=== 4. wechat-greeting.html generate调用 ===')
out = run(
    'sed -n "1,150p" /data/dinghong/admin/wechat-greeting.html 2>/dev/null'
)
print(out[:2000])

s.close()