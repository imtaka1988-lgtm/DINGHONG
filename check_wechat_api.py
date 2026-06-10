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

# 1. WeChat 相关文件
print('=== 1. access_token 相关文件 ===')
out = run(
    'grep -rn "access_token" '
    '/data/dinghong/app/dinghong-api/src/main/java/ '
    '2>/dev/null | grep -v ".bak" | head -20'
)
print(out)

# 2. WechatPublishController (draft)
print('\n=== 2. WechatPublishController ===')
out = run(
    'cat /data/dinghong/app/dinghong-api/src/main/java/com/dinghong/'
    'controller/editor/WechatPublishController.java 2>/dev/null'
)
print(out)

# 3. WechatDraftService 缩略了解 access_token 部分
print('\n=== 3. WechatDraftService getAccessToken ===')
out = run(
    'grep -n -A20 "getAccessToken\\|access_token" '
    '/data/dinghong/app/dinghong-api/src/main/java/com/dinghong/'
    'service/wechat/WechatDraftService.java 2>/dev/null | head -60'
)
print(out)

# 4. 检查 env.sh 中微信配置
print('\n=== 4. env.sh 微信相关 ===')
out = run('cat /data/dinghong/env.sh')
print(out)

# 5. 检查 WechatDraftService 中 AppID/Secret 怎么获取
print('\n=== 5. WechatDraftService AppID/Secret ===')
out = run(
    'grep -n "APPID\\|AppId\\|appid\\|WECHAT\\|wechat" '
    '/data/dinghong/app/dinghong-api/src/main/java/com/dinghong/'
    'service/wechat/WechatDraftService.java 2>/dev/null | head -30'
)
print(out)

# 6. 看原来 WechatController 中访问 access_token
print('\n=== 6. WechatController access_token ===')
out = run(
    'grep -n "access_token\\|WECHAT_APPID\\|WECHAT_SECRET" '
    '/data/dinghong/app/dinghong-api/src/main/java/com/dinghong/'
    'controller/wechat/WechatController.java 2>/dev/null | head -20'
)
print(out)

# 7. 检查是否有 wechat_token.json
print('\n=== 7. wechat_token.json ===')
out = run('cat /data/dinghong/wechat_token.json 2>/dev/null')
print(out)

s.close()