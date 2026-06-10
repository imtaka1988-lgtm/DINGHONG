import paramiko
import os
import hashlib

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('8.210.102.206', username='root', password='Taka888.', timeout=10)

# 本地工作区根目录
local_base = r'c:\Users\Administrator\Desktop\顶红公众号'

# 服务器主要目录
server_base = '/data/dinghong'

# 关键Java文件对比路径
key_files = [
    ('app/dinghong-api/src/main/java/com/dinghong/service/editor/EditorService.java',
     'app/dinghong-api/src/main/java/com/dinghong/service/editor/EditorService.java'),
    ('app/dinghong-api/src/main/java/com/dinghong/service/wechat/WechatDraftService.java',
     'app/dinghong-api/src/main/java/com/dinghong/service/wechat/WechatDraftService.java'),
    ('app/dinghong-api/src/main/java/com/dinghong/service/rule/RulePromptService.java',
     'app/dinghong-api/src/main/java/com/dinghong/service/rule/RulePromptService.java'),
    ('app/dinghong-api/src/main/java/com/dinghong/service/research/MatchResearchService.java',
     'app/dinghong-api/src/main/java/com/dinghong/service/research/MatchResearchService.java'),
    ('app/dinghong-api/src/main/java/com/dinghong/service/search/BaiduSearchService.java',
     'app/dinghong-api/src/main/java/com/dinghong/service/search/BaiduSearchService.java'),
    ('app/dinghong-api/src/main/resources/application.yml',
     'app/dinghong-api/src/main/resources/application.yml'),
    ('app/dinghong-api/pom.xml',
     'app/dinghong-api/pom.xml'),
]

# Admin页面对比
admin_pages = [
    'articles.html', 'matches.html', 'live.html', 'play.html',
    'index.html', 'login.html', 'prompts.html', 'ad-config.html',
    'wechat-greeting.html'
]

print("=" * 60)
print("Java源文件同步检查")
print("=" * 60)

for local_rel, server_rel in key_files:
    local_path = os.path.join(local_base, local_rel)
    server_path = f'{server_base}/{server_rel}'

    local_md5 = 'NOT_FOUND'
    server_md5 = 'NOT_FOUND'
    local_size = 0
    server_size = 0

    if os.path.exists(local_path):
        local_md5 = hashlib.md5(open(local_path, 'rb').read()).hexdigest()
        local_size = os.path.getsize(local_path)
    else:
        print(f'[本地缺失] {local_rel}')

    stdin, stdout, stderr = ssh.exec_command(f'md5sum "{server_path}" 2>/dev/null || echo "NOT_FOUND"')
    out = stdout.read().decode().strip()
    if out != 'NOT_FOUND':
        server_md5 = out.split()[0]
        stdin2, stdout2, stderr2 = ssh.exec_command(f'wc -c < "{server_path}"')
        server_size = int(stdout2.read().decode().strip())
    else:
        print(f'[服务器缺失] {server_rel}')

    fname = os.path.basename(local_rel)
    if local_md5 == server_md5:
        print(f'[同步] {fname} ({local_size}b)')
    else:
        print(f'[不同!] {fname} 本地={local_size}b 服务器={server_size}b MD5不同!')

print()
print("=" * 60)
print("Admin页面同步检查")
print("=" * 60)

for page in admin_pages:
    local_path = os.path.join(local_base, '顶红体育', 'admin_pages', page)
    if page == 'play.html':
        server_path = f'{server_base}/admin/live/play.html'
    elif page == 'live.html':
        server_path = f'{server_base}/admin/live.html'
    elif page == 'ad-config.html':
        server_path = f'{server_base}/admin/ad-config.html'
    elif page == 'live_ad_config.json':
        server_path = f'{server_base}/admin/live/ad_config.json'
    else:
        server_path = f'{server_base}/admin/{page}'

    local_md5 = 'NOT_FOUND'
    server_md5 = 'NOT_FOUND'
    local_size = 0
    server_size = 0

    if os.path.exists(local_path):
        local_md5 = hashlib.md5(open(local_path, 'rb').read()).hexdigest()
        local_size = os.path.getsize(local_path)
    else:
        print(f'[本地缺失] {page}')
        continue

    stdin, stdout, stderr = ssh.exec_command(f'md5sum "{server_path}" 2>/dev/null || echo "NOT_FOUND"')
    out = stdout.read().decode().strip()
    if out != 'NOT_FOUND':
        server_md5 = out.split()[0]
        stdin2, stdout2, stderr2 = ssh.exec_command(f'wc -c < "{server_path}"')
        server_size = int(stdout2.read().decode().strip())
    else:
        print(f'[服务器缺失] {page}')
        continue

    if local_md5 == server_md5:
        print(f'[同步] {page} ({local_size}b)')
    else:
        print(f'[不同!] {page} 本地={local_size}b 服务器={server_size}b')

# 检查本地有但服务器没有的关键文件
print()
print("=" * 60)
print("nginx配置对比")
print("=" * 60)

local_nginx_path = os.path.join(local_base, '顶红体育', 'nginx_docker.conf')
server_nginx_path = f'{server_base}/nginx/nginx.conf'

if os.path.exists(local_nginx_path):
    local_md5 = hashlib.md5(open(local_nginx_path, 'rb').read()).hexdigest()
    stdin, stdout, stderr = ssh.exec_command(f'md5sum "{server_nginx_path}" 2>/dev/null || echo "NOT_FOUND"')
    out = stdout.read().decode().strip()
    if out != 'NOT_FOUND':
        server_md5 = out.split()[0]
        if local_md5 == server_md5:
            print('[同步] nginx配置')
        else:
            print('[不同!] nginx配置不同！')
            print(f'  本地: nginx_docker.conf')
            print(f'  服务器: nginx/nginx.conf')
    else:
        print('[服务器缺失] nginx/nginx.conf')
else:
    print('[本地缺失] nginx_docker.conf')

# 检查新控制器文件
print()
print("=" * 60)
print("新增文件检查（本地有，服务器无）")
print("=" * 60)

new_files = [
    'app/dinghong-api/src/main/java/com/dinghong/controller/admin/GreetingConfigController.java',
    'app/dinghong-api/src/main/java/com/dinghong/controller/editor/WechatPublishController.java',
    'app/dinghong-api/src/main/java/com/dinghong/controller/live/LiveProxyController.java',
    'app/dinghong-api/src/main/java/com/dinghong/controller/admin/WechatMenuController.java',
]

for nf in new_files:
    local_path = os.path.join(local_base, nf)
    server_path = f'{server_base}/{nf}'
    if os.path.exists(local_path):
        stdin, stdout, stderr = ssh.exec_command(f'test -f "{server_path}" && echo "EXISTS" || echo "MISSING"')
        out = stdout.read().decode().strip()
        status = '服务器存在' if out == 'EXISTS' else '服务器缺失!'
        fname = os.path.basename(nf)
        print(f'本地有: {fname} -> {status}')

ssh.close()