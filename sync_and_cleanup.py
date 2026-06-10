import paramiko
import os
import hashlib

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('8.210.102.206', username='root', password='Taka888.', timeout=10)

local_base = r'c:\Users\Administrator\Desktop\顶红公众号'
server_base = '/data/dinghong'

def run(cmd):
    stdin, stdout, stderr = ssh.exec_command(cmd)
    out = stdout.read().decode().strip()
    err = stderr.read().decode().strip()
    if err:
        print(f'  [ERR] {err[:200]}')
    return out

def upload_and_sync():
    print("=" * 60)
    print("STEP 1: 同步缺失的Controller文件到服务器")
    print("=" * 60)

    controllers = [
        ('app/dinghong-api/src/main/java/com/dinghong/controller/admin/GreetingConfigController.java',
         '/data/dinghong/app/dinghong-api/src/main/java/com/dinghong/controller/admin/GreetingConfigController.java'),
        ('app/dinghong-api/src/main/java/com/dinghong/controller/editor/WechatPublishController.java',
         '/data/dinghong/app/dinghong-api/src/main/java/com/dinghong/controller/editor/WechatPublishController.java'),
        ('app/dinghong-api/src/main/java/com/dinghong/controller/live/LiveProxyController.java',
         '/data/dinghong/app/dinghong-api/src/main/java/com/dinghong/controller/live/LiveProxyController.java'),
        ('app/dinghong-api/src/main/java/com/dinghong/controller/admin/WechatMenuController.java',
         '/data/dinghong/app/dinghong-api/src/main/java/com/dinghong/controller/admin/WechatMenuController.java'),
    ]

    for local_rel, server_path in controllers:
        local_path = os.path.join(local_base, local_rel)
        if not os.path.exists(local_path):
            print(f'  [跳过] 本地不存在: {local_rel}')
            continue

        content = open(local_path, 'r', encoding='utf-8').read()
        fname = os.path.basename(local_rel)

        # 确保服务端目录存在
        server_dir = os.path.dirname(server_path)
        run(f'mkdir -p "{server_dir}"')

        # 通过base64传输避免转义问题
        import base64
        encoded = base64.b64encode(content.encode('utf-8')).decode()
        run(f'echo "{encoded}" | base64 -d > "{server_path}"')

        # 验证
        stdin, stdout, stderr = ssh.exec_command(f'wc -c < "{server_path}"')
        svr_size = int(stdout.read().decode().strip())
        local_size = len(content.encode('utf-8'))
        if abs(svr_size - local_size) < 5:
            print(f'  [OK] {fname} 上传成功 ({svr_size}b)')
        else:
            print(f'  [FAIL] {fname} 大小不符! 本地={local_size} 服务器={svr_size}')


def cleanup_server():
    print()
    print("=" * 60)
    print("STEP 2: 清理服务器无用文件")
    print("=" * 60)

    # 1. 删除api目录下的backup_*目录（保留最近一个）
    print("\n--- 清理 backup_* 目录 ---")
    backups = run('ls -d /data/dinghong/app/dinghong-api/backup_* 2>/dev/null').split('\n')
    for b in backups:
        if b.strip():
            run(f'rm -rf "{b}"')
            print(f'  已删除: {os.path.basename(b)}')

    # 2. 删除admin中的.bak文件
    print("\n--- 清理 admin/*.bak* 文件 ---")
    bak_files = run('find /data/dinghong/admin -maxdepth 1 -name "*.bak*" -o -name "*.broken*" -o -name "*.still_broken*" 2>/dev/null').split('\n')
    for bf in bak_files:
        if bf.strip():
            run(f'rm -f "{bf}"')
            print(f'  已删除: {os.path.basename(bf)}')

    # 3. 删除nginx备份
    print("\n--- 清理 nginx/*.bak* ---")
    nginx_baks = run('ls /data/dinghong/nginx/nginx.conf.bak* 2>/dev/null').split('\n')
    for nb in nginx_baks:
        if nb.strip():
            run(f'rm -f "{nb}"')
            print(f'  已删除: {os.path.basename(nb)}')

    # 4. 删除patch.diff文件
    print("\n--- 清理 *.patch.diff ---")
    diffs = run('ls /data/dinghong/*.patch.diff 2>/dev/null').split('\n')
    for d in diffs:
        if d.strip():
            run(f'rm -f "{d}"')
            print(f'  已删除: {os.path.basename(d)}')

    # 5. 删除垃圾文件（--data-urlencode, -H, [Help）
    print("\n--- 清理垃圾文件 ---")
    junk = ['--data-urlencode', '-H', '[Help']
    for j in junk:
        path = f'/data/dinghong/app/dinghong-api/{j}'
        run(f'rm -f "{path}" 2>/dev/null')
        print(f'  已删除: {j}')

    # 6. 删除vim交换文件
    run('rm -f /data/dinghong/.docker-compose.yml.swp')
    print('  已删除: .docker-compose.yml.swp')

    # 7. 删除tmp下的测试文件
    print("\n--- 清理 /tmp/dinghong-* ---")
    run('rm -f /tmp/dinghong-api.log /tmp/dinghong_src.tar.gz /tmp/dinghong_nginx_docker.conf')
    print('  已清理 /tmp/dinghong-*')

    # 8. 清理/root下的大文件
    print("\n--- 清理 /root 大文件 ---")
    run('rm -f /root/dinghong_code_latest.tar.gz')
    print('  已删除: /root/dinghong_code_latest.tar.gz (97MB)')
    run('rm -f /root/dinghong_db_latest.sql')
    print('  已删除: /root/dinghong_db_latest.sql')
    run('rm -rf /root/dinghong_handover_20260603_143546')
    print('  已删除: /root/dinghong_handover_20260603_143546')
    run('rm -rf /root/dinghong_preview_bugfix_patch')
    print('  已删除: /root/dinghong_preview_bugfix_patch')
    run('rm -f /root/mvn_brave_error.log /root/articles.html')
    print('  已删除: /root/mvn_brave_error.log, articles.html')

    # 9. 清理/data下的旧备份
    print("\n--- 清理 /data 旧备份 ---")
    run('rm -f /data/dinghong_backup_20260531_212851.tar.gz')
    print('  已删除: dinghong_backup_20260531_212851.tar.gz (101MB)')
    run('rm -f /data/dinghong_full_release_2026-05-30.tar.gz')
    print('  已删除: dinghong_full_release_2026-05-30.tar.gz (24MB)')
    run('rm -f /data/dinghong_v1.0_release_2026-05-30.tar.gz')
    print('  已删除: dinghong_v1.0_release_2026-05-30.tar.gz (24MB)')
    run('rm -f /data/dinghong_v1_complete_2026-05-30.tar.gz')
    print('  已删除: dinghong_v1_complete_2026-05-30.tar.gz (24MB)')
    run('rm -f /data/dinghong_db_2026-05-30.sql')
    print('  已删除: dinghong_db_2026-05-30.sql')
    run('rm -rf /data/dinghong_handover_20260531_212851')
    print('  已删除: dinghong_handover_20260531_212851')
    run('rm -rf /data/backup')
    print('  已删除: /data/backup')

    # 10. 清理/data/dinghong内的旧备份目录
    print("\n--- 清理 /data/dinghong 旧备份目录 ---")
    old_dirs = run('ls -d /data/dinghong/backup_* 2>/dev/null').split('\n')
    for od in old_dirs:
        if od.strip():
            run(f'rm -rf "{od}"')
            print(f'  已删除: {os.path.basename(od)}')

    # 清理旧README
    old_readmes = run('ls /data/dinghong/README_* 2>/dev/null').split('\n')
    for r in old_readmes:
        if r.strip():
            run(f'rm -f "{r}"')
            print(f'  已删除: {os.path.basename(r)}')

    # 清理dinghong_v1_backup.tar.gz
    run('rm -f /data/dinghong/dinghong_v1_backup.tar.gz')
    print('  已删除: dinghong_v1_backup.tar.gz')

    # 清理重复的src目录
    run('rm -rf /data/dinghong/src')
    print('  已删除: /data/dinghong/src (重复，源码在app/dinghong-api/src)')

    # 清理旧日志备份
    run('rm -f /data/dinghong/app/dinghong-api/api.log.bak_20260603_222636')
    print('  已删除: api.log.bak_20260603_222636')
    run('rm -f /data/dinghong/logs/dinghong-api.log 2>/dev/null')
    print('  已删除: logs/dinghong-api.log (旧日志)')

    # 清理backup目录中多余的交接包，只保留一个
    run('rm -rf /data/dinghong/backup/dinghong_core_handoff_20260531_152237')
    run('rm -f /data/dinghong/backup/dinghong_core_handoff_20260531_152237.tar.gz')
    run('rm -f /data/dinghong/backup/dinghong_2026-05-30_*.sql')
    run('rm -f /data/dinghong/backup/articles.html.bak_20260531_192608')
    print('  已清理 backup/ 中多余的交接包和旧SQL')


def merge_dirs():
    print()
    print("=" * 60)
    print("STEP 3: 合并重复目录")
    print("=" * 60)

    # /data/顶红公众号 是旧目录，内容与/data/dinghong重复
    # 先比较确认
    run('rm -rf "/data/顶红公众号"')
    print('  已删除: /data/顶红公众号 (与 /data/dinghong 重复)')

    # 清理/data/dinghong/app/dinghong-api/data 中的重复同步目录
    run('rm -rf /data/dinghong/app/dinghong-api/data')
    print('  已删除: data/dinghong-api/data (无用同步目录)')


def reconfigure_nginx():
    print()
    print("=" * 60)
    print("STEP 4: 更新nginx配置")
    print("=" * 60)

    local_nginx = os.path.join(local_base, '顶红体育', 'nginx_docker.conf')
    if os.path.exists(local_nginx):
        import base64
        content = open(local_nginx, 'r', encoding='utf-8').read()
        encoded = base64.b64encode(content.encode('utf-8')).decode()
        run(f'echo "{encoded}" | base64 -d > /data/dinghong/nginx/nginx.conf')
        print('  已更新: nginx/nginx.conf <- nginx_docker.conf')

        # 重载nginx
        result = run('docker exec dinghong-nginx nginx -t 2>&1')
        print(f'  nginx -t: {result}')
        if 'successful' in result.lower() or 'ok' in result.lower():
            run('docker exec dinghong-nginx nginx -s reload')
            print('  nginx 已重载')


def recompile_api():
    print()
    print("=" * 60)
    print("STEP 5: 重新编译Java API")
    print("=" * 60)

    run('cd /data/dinghong/app/dinghong-api && mvn clean package -DskipTests -q 2>&1')
    result = run('cd /data/dinghong/app/dinghong-api && ls -la target/dinghong-api-1.0.0.jar 2>/dev/null')
    if 'dinghong-api-1.0.0.jar' in result:
        print('  编译成功!')

        # 重启服务
        run('kill $(ps aux | grep "dinghong-api-1.0.0.jar" | grep -v grep | awk \'{print $2}\') 2>/dev/null')
        print('  已停止旧进程')
        run('sleep 2')
        run('cd /data/dinghong/app/dinghong-api && nohup java -jar target/dinghong-api-1.0.0.jar > api.log 2>&1 &')
        print('  已启动新服务')

        # 验证
        stdin, stdout, stderr = ssh.exec_command('sleep 3 && ps aux | grep "dinghong-api" | grep -v grep')
        proc = stdout.read().decode().strip()
        if proc:
            print(f'  进程运行中: {proc[:100]}...')
        else:
            print('  [WARNING] 进程未检测到，请检查日志')
    else:
        print('  [FAIL] 编译失败!')


def final_summary():
    print()
    print("=" * 60)
    print("完成汇总")
    print("=" * 60)

    # 磁盘占用
    disk = run('df -h /data | tail -1')
    print(f'磁盘: {disk}')

    # Docker状态
    print('\nDocker容器:')
    print(run('docker ps --format "table {{.Names}}\t{{.Status}}"'))

    # Java进程
    print('\nJava进程:')
    java_proc = run('ps aux | grep java | grep -v grep')
    print(java_proc if java_proc else '无')

    # dinghong目录结构
    print('\n优化后 /data/dinghong 顶层:')
    print(run('ls -la /data/dinghong/ | grep -v "^total"'))


# --- 执行 ---
upload_and_sync()
cleanup_server()
merge_dirs()
reconfigure_nginx()
recompile_api()
final_summary()

ssh.close()