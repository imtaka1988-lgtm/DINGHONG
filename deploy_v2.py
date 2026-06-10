import paramiko
import os
import base64

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('8.210.102.206', username='root', password='Taka888.', timeout=15)
local_base = r'c:\Users\Administrator\Desktop\顶红公众号'

def run(cmd):
    stdin, stdout, stderr = ssh.exec_command(cmd)
    out = stdout.read().decode().strip()
    err = stderr.read().decode().strip()
    if err:
        print('  ERR: ' + err[:150])
    return out

def upload_b64(local_path, server_path):
    """Upload file via base64 encoding"""
    content = open(local_path, 'rb').read()
    encoded = base64.b64encode(content).decode()
    server_dir = os.path.dirname(server_path)
    run('mkdir -p "' + server_dir + '"')
    run('echo "' + encoded + '" | base64 -d > "' + server_path + '"')
    fname = os.path.basename(local_path)
    stdin, stdout, stderr = ssh.exec_command('wc -c < "' + server_path + '"')
    svr_size = int(stdout.read().decode().strip())
    local_size = len(content)
    ok = abs(svr_size - local_size) < 5
    print(f'  {"[OK]" if ok else "[FAIL]"} {fname} local={local_size} svr={svr_size}')
    return ok

# ======= 1. Upload modified articles.html =======
print('=== 1. Upload articles.html ===')
local_html = os.path.join(local_base, '顶红体育', 'admin_pages', 'articles.html')
if os.path.exists(local_html):
    upload_b64(local_html, '/data/dinghong/admin/articles.html')
else:
    print('  Local articles.html not found!')

# ======= 2. Upload modified ArticleController.java =======
print('\n=== 2. Upload ArticleController.java ===')
local_ctrl = os.path.join(
    local_base, 'app', 'dinghong-api', 'src', 'main', 'java',
    'com', 'dinghong', 'controller', 'editor', 'ArticleController.java'
)
if os.path.exists(local_ctrl):
    upload_b64(local_ctrl,
               '/data/dinghong/app/dinghong-api/src/main/java/com/'
               'dinghong/controller/editor/ArticleController.java')
else:
    print('  Local ArticleController.java not found!')

# ======= 3. Build & restart =======
print('\n=== 3. Build & restart ===')
result = run(
    'cd /data/dinghong/app/dinghong-api '
    '&& JAVA_HOME=/usr/lib/jvm/java-17-openjdk-17.0.19.0.10-1.0.2.1.al8.x86_64 '
    'mvn clean package -DskipTests -q 2>&1'
)
# Check jar
jar_out = run(
    'ls -la '
    '/data/dinghong/app/dinghong-api/target/dinghong-api-1.0.0.jar '
    '2>/dev/null'
)
if 'dinghong-api' in jar_out:
    print('  Build OK: ' + jar_out.split()[-1] if jar_out.split() else '')
    # Restart
    run('kill $(ps aux | grep dinghong-api-1.0.0.jar | '
        'grep -v grep | awk \'{print $2}\') 2>/dev/null')
    import time
    time.sleep(2)
    ssh.exec_command(
        'cd /data/dinghong/app/dinghong-api '
        '&& nohup java -jar target/dinghong-api-1.0.0.jar '
        '> api.log 2>&1 &'
    )
    time.sleep(4)
    proc = run('ps aux | grep dinghong-api | grep -v grep')
    print('  Process: ' + (proc[:120] if proc else 'NOT DETECTED'))
else:
    print('  Build FAILED!')
    # Show error
    err_out = run(
        'cd /data/dinghong/app/dinghong-api '
        '&& JAVA_HOME=/usr/lib/jvm/java-17-openjdk-17.0.19.0.10-1.0.2.1.al8.x86_64 '
        'mvn compile 2>&1 | tail -20'
    )
    print('  Maven output:')
    print(err_out)

ssh.close()
print('\nDone!')