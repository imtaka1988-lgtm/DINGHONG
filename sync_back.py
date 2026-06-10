import paramiko
import os
import hashlib
import base64

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('8.210.102.206', username='root', password='Taka888.', timeout=15)

local_base = r'c:\Users\Administrator\Desktop\顶红公众号'
server_base = '/data/dinghong'

def run(cmd):
    stdin, stdout, stderr = ssh.exec_command(cmd)
    return stdout.read().decode().strip(), stderr.read().decode().strip()

# 列出服务器所有Java文件
print('=== 服务器Java文件列表 ===')
out, _ = run(
    'find /data/dinghong/app/dinghong-api/src/main/java '
    '-name "*.java" 2>/dev/null | sort'
)
server_java_files = [f.strip() for f in out.split('\n') if f.strip()]

# 列出本地所有Java文件
local_java_map = {}
for root, dirs, files in os.walk(os.path.join(local_base, 'app')):
    for f in files:
        if f.endswith('.java'):
            full_path = os.path.join(root, f)
            rel = os.path.relpath(full_path, local_base).replace('\\', '/')
            local_java_map[os.path.basename(f)] = full_path

# 列出服务器admin文件
out, _ = run(
    'find /data/dinghong/admin -maxdepth 2 '
    '-name "*.html" -o -name "*.css" -o -name "*.js" '
    '-o -name "*.json" -o -name "*.txt" '
    '2>/dev/null | sort'
)
server_admin_files = [f.strip() for f in out.split('\n') if f.strip()]

# 本地admin_pages
local_admin_dir = os.path.join(local_base, '顶红体育', 'admin_pages')

pulled = 0

print()
print('=== 检查服务器有但本地缺失的Java文件 ===')
for sp in server_java_files:
    rel = sp.replace('/data/dinghong/', '')
    local_path = os.path.join(local_base, rel)
    if not os.path.exists(local_path):
        # 下载
        server_dir = os.path.dirname(sp)
        fname = os.path.basename(sp)
        local_dir = os.path.dirname(local_path)
        os.makedirs(local_dir, exist_ok=True)

        out, _ = run(f'base64 "{sp}"')
        if out:
            content = base64.b64decode(out)
            with open(local_path, 'wb') as f:
                f.write(content)
            print(f'  [拉取] {rel}')
            pulled += 1

print()
print('=== 检查Admin文件 ===')
for sp in server_admin_files:
    rel_parts = sp.replace('/data/dinghong/admin/', '')
    if '/' in rel_parts:
        # live/play.html -> live_play.html
        parts = rel_parts.split('/')
        local_name = '_'.join(parts)
    else:
        local_name = rel_parts

    local_path = os.path.join(local_admin_dir, local_name)
    if not os.path.exists(local_path):
        fname = os.path.basename(sp)
        out, _ = run(f'base64 "{sp}"')
        if out:
            content = base64.b64decode(out)
            with open(local_path, 'wb') as f:
                f.write(content)
            print(f'  [拉取] admin_pages/{local_name}')
            pulled += 1

# 检查其他关键文件
print()
print('=== 检查其他关键文件 ===')
other_files = [
    ('/data/dinghong/docker-compose.yml',
     '顶红体育/docker-compose.yml'),
    ('/data/dinghong/env.sh',
     '顶红体育/env.sh'),
    ('/data/dinghong/start-api.sh',
     '顶红体育/start-api.sh'),
]
for sp, local_rel in other_files:
    local_path = os.path.join(local_base, local_rel)
    if not os.path.exists(local_path):
        out, _ = run(f'base64 "{sp}"')
        if out:
            content = base64.b64decode(out)
            local_dir = os.path.dirname(local_path)
            os.makedirs(local_dir, exist_ok=True)
            with open(local_path, 'wb') as f:
                f.write(content)
            print(f'  [拉取] {local_rel}')
            pulled += 1

# 检查pom.xml和application.yml是否需要更新
print()
print('=== 检查pom.xml差异 ===')
out, _ = run('md5sum /data/dinghong/app/dinghong-api/pom.xml')
svr_pom_md5 = out.split()[0] if out else ''
local_pom = os.path.join(local_base, 'app', 'dinghong-api', 'pom.xml')
if os.path.exists(local_pom):
    local_md5 = hashlib.md5(open(local_pom, 'rb').read()).hexdigest()
    if svr_pom_md5 != local_md5:
        print(f'  pom.xml 不同 (服务器={svr_pom_md5[:8]} 本地={local_md5[:8]})')
        out, _ = run('base64 /data/dinghong/app/dinghong-api/pom.xml')
        if out:
            content = base64.b64decode(out)
            with open(local_pom, 'wb') as f:
                f.write(content)
            print(f'  [拉取] pom.xml')
            pulled += 1

print()
print(f'总计拉取 {pulled} 个文件到本地')
ssh.close()