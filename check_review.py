import paramiko

s = paramiko.SSHClient()
s.set_missing_host_key_policy(paramiko.AutoAddPolicy())
s.connect('8.210.102.206', username='root', password='Taka888.', timeout=15)

def run(cmd):
    stdin, stdout, stderr = s.exec_command(cmd)
    return stdout.read().decode().strip()

# 读取ArticleController.java 第30-60行 (review_blocked逻辑)
print('=== ArticleController.java 第30-120行 ===')
out = run(
    'sed -n "30,120p" '
    '/data/dinghong/app/dinghong-api/src/main/java/com/dinghong/'
    'controller/editor/ArticleController.java'
)
print(out)

print()
print('=== EditorService.java 第70-100行 ===')
out = run(
    'sed -n "70,100p" '
    '/data/dinghong/app/dinghong-api/src/main/java/com/dinghong/'
    'service/editor/EditorService.java'
)
print(out)

# 检查本地application.yml是否有百度key
print()
print('=== 本地 application.yml ===')
import os
local_yml = r'c:\Users\Administrator\Desktop\顶红公众号\app\dinghong-api\src\main\resources\application.yml'
if os.path.exists(local_yml):
    content = open(local_yml, 'r', encoding='utf-8').read()
    print(content)
else:
    print('本地缺失!')

s.close()