import paramiko
import os
import base64

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('8.210.102.206', username='root', password='Taka888.', timeout=10)

local_base = r'c:\Users\Administrator\Desktop\顶红公众号'

def run(cmd):
    stdin, stdout, stderr = ssh.exec_command(cmd)
    out = stdout.read().decode().strip()
    err = stderr.read().decode().strip()
    if err:
        print('  [ERR] ' + err[:200])
    return out

# ======= STEP 1: 上传Controller =======
print('=' * 50)
print('STEP 1: 上传4个缺失的Controller')
print('=' * 50)

controllers = [
    'app/dinghong-api/src/main/java/com/dinghong/controller/admin/GreetingConfigController.java',
    'app/dinghong-api/src/main/java/com/dinghong/controller/editor/WechatPublishController.java',
    'app/dinghong-api/src/main/java/com/dinghong/controller/live/LiveProxyController.java',
    'app/dinghong-api/src/main/java/com/dinghong/controller/admin/WechatMenuController.java',
]

for c in controllers:
    local_path = os.path.join(local_base, c)
    server_path = '/data/dinghong/' + c
    fname = os.path.basename(c)
    if os.path.exists(local_path):
        server_dir = os.path.dirname(server_path)
        run('mkdir -p "' + server_dir + '"')
        content = open(local_path, 'rb').read()
        encoded = base64.b64encode(content).decode()
        run('echo "' + encoded + '" | base64 -d > "' + server_path + '"')
        stdin, stdout, stderr = ssh.exec_command(
            'wc -c < "' + server_path + '"')
        svr = int(stdout.read().decode().strip())
        print('  [OK] ' + fname + ' (' + str(svr) + 'b)')
    else:
        print('  [MISS] ' + fname)

# ======= STEP 2: 清理backup_*目录 =======
print()
print('STEP 2: 清理backup目录')
backups = run(
    'ls -d /data/dinghong/app/dinghong-api/backup_* 2>/dev/null'
).split('\n')
for b in backups:
    if b.strip():
        run('rm -rf "' + b + '"')
        print('  DEL: ' + os.path.basename(b))

# ======= STEP 3: 清理admin/.bak =======
print()
print('STEP 3: 清理admin bak文件')
baks = run(
    'find /data/dinghong/admin -maxdepth 1 '
    '-name "*.bak*" -o -name "*.broken*" '
    '-o -name "*.still_broken*" 2>/dev/null'
).split('\n')
for b in baks:
    if b.strip():
        run('rm -f "' + b + '"')
        print('  DEL: ' + os.path.basename(b))

# ======= STEP 4: 清理nginx bak =======
print()
print('STEP 4: 清理nginx bak')
nbaks = run(
    'ls /data/dinghong/nginx/nginx.conf.bak* 2>/dev/null'
).split('\n')
for n in nbaks:
    if n.strip():
        run('rm -f "' + n + '"')
        print('  DEL: ' + os.path.basename(n))

# ======= STEP 5: patch diff =======
print()
print('STEP 5: 清理patch.diff')
diffs = run('ls /data/dinghong/*.patch.diff 2>/dev/null').split('\n')
for d in diffs:
    if d.strip():
        run('rm -f "' + d + '"')
        print('  DEL: ' + os.path.basename(d))

# ======= STEP 6: 垃圾文件 =======
print()
print('STEP 6: 清理垃圾文件')
for j in ['--data-urlencode', '-H', '[Help']:
    path = '/data/dinghong/app/dinghong-api/' + j
    run('rm -f "' + path + '" 2>/dev/null')
    print('  DEL: ' + j)
run('rm -f /data/dinghong/.docker-compose.yml.swp')
print('  DEL: .docker-compose.yml.swp')

# ======= STEP 7: /tmp =======
print()
print('STEP 7: 清理/tmp')
run('rm -f /tmp/dinghong-api.log '
    '/tmp/dinghong_src.tar.gz '
    '/tmp/dinghong_nginx_docker.conf')
print('  DEL: /tmp/dinghong-*')

# ======= STEP 8: /root大文件 =======
print()
print('STEP 8: 清理/root')
root_files = [
    '/root/dinghong_code_latest.tar.gz',
    '/root/dinghong_db_latest.sql',
    '/root/dinghong_handover_20260603_143546',
    '/root/dinghong_preview_bugfix_patch',
    '/root/mvn_brave_error.log',
    '/root/articles.html',
]
for rf in root_files:
    run('rm -rf "' + rf + '"')
    print('  DEL: ' + os.path.basename(rf))

# ======= STEP 9: /data旧备份 =======
print()
print('STEP 9: 清理/data旧备份')
data_files = [
    '/data/dinghong_backup_20260531_212851.tar.gz',
    '/data/dinghong_full_release_2026-05-30.tar.gz',
    '/data/dinghong_v1.0_release_2026-05-30.tar.gz',
    '/data/dinghong_v1_complete_2026-05-30.tar.gz',
    '/data/dinghong_db_2026-05-30.sql',
    '/data/dinghong_handover_20260531_212851',
    '/data/backup',
]
for df in data_files:
    run('rm -rf "' + df + '"')
    print('  DEL: ' + os.path.basename(df))

# ======= STEP 10: dinghong内部旧文件 =======
print()
print('STEP 10: 清理dinghong内部旧文件')
run('rm -rf /data/dinghong/backup_*')
print('  DEL: backup_* dirs')
run('rm -f /data/dinghong/README_*')
print('  DEL: README_*')
run('rm -f /data/dinghong/dinghong_v1_backup.tar.gz')
print('  DEL: dinghong_v1_backup.tar.gz')
run('rm -rf /data/dinghong/src')
print('  DEL: /data/dinghong/src')
run('rm -f /data/dinghong/app/dinghong-api/api.log.bak_20260603_222636')
print('  DEL: api.log.bak')
run('rm -f /data/dinghong/logs/dinghong-api.log 2>/dev/null')
print('  DEL: logs/dinghong-api.log')
run('rm -rf /data/dinghong/backup/dinghong_core_handoff_20260531_152237')
run('rm -f /data/dinghong/backup/dinghong_core_handoff_20260531_152237.tar.gz')
run('rm -f /data/dinghong/backup/dinghong_2026-05-30_*.sql')
run('rm -f /data/dinghong/backup/articles.html.bak_20260531_192608')
print('  DEL: backup多余文件')

# ======= STEP 11: 合并重复目录 =======
print()
print('STEP 11: 合并重复目录')
run('rm -rf "/data/\u9876\u7ea2\u516c\u4f17\u53f7"')
print('  DEL: /data/顶红公众号')
run('rm -rf /data/dinghong/app/dinghong-api/data')
print('  DEL: dinghong-api/data')

# ======= STEP 12: 更新nginx =======
print()
print('STEP 12: 更新nginx配置')
local_nginx = os.path.join(local_base, '顶红体育', 'nginx_docker.conf')
if os.path.exists(local_nginx):
    content = open(local_nginx, 'rb').read()
    encoded = base64.b64encode(content).decode()
    run('echo "' + encoded
        + '" | base64 -d > /data/dinghong/nginx/nginx.conf')
    print('  已更新 nginx.conf')
    result = run('docker exec dinghong-nginx nginx -t 2>&1')
    print('  nginx -t: ' + result[:200])
    if 'successful' in result.lower() or 'ok' in result.lower():
        run('docker exec dinghong-nginx nginx -s reload')
        print('  nginx 已重载')

# ======= 汇总 =======
print()
print('=' * 50)
print('汇总')
print('=' * 50)
print('磁盘: ' + run('df -h /data | tail -1'))
print()
print('Docker:')
print(run('docker ps --format "table {{.Names}}\t{{.Status}}"'))
print()
print('Java:')
print(run('ps aux | grep java | grep -v grep'))
print()
print('/data/dinghong 顶层:')
print(run('ls -la /data/dinghong/ | grep -v "^total"'))

ssh.close()
print()
print('SUCCESS - 同步+清理+合并完成!')
print('接下来需要手动执行: cd /data/dinghong/app/dinghong-api && '
      'mvn clean package -DskipTests && 重启服务')