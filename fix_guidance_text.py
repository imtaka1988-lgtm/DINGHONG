#!/usr/bin/env python3
"""批量修改公众号引导话术 - 移除关键词搜索相关引导"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("8.210.102.206", 22, "root", "Taka888.", timeout=20)
print("Connected\n")

WSRC = "/data/dinghong/app/dinghong-api/src/main/java/com/dinghong/controller/wechat/WechatController.java"
MSRC = "/data/dinghong/app/dinghong-api/src/main/java/com/dinghong/service/MatchDbService.java"

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=30)
    out = o.read().decode(errors='replace')
    err = e.read().decode(errors='replace')
    if out.strip(): print("  OUT:", out.strip()[:300])
    if err.strip(): print("  ERR:", err.strip()[:300])
    return out, err

# ==========================================
# 1. Fix WechatController.java subscribe welcome
# ==========================================
print("===== Fix WechatController subscribe message =====")
# Old: includes "查赛事：回复球队名、联赛名"
# New: simplified without keyword search
cmd = f'''python3 -c "
import re
with open('{WSRC}', 'r') as f: c = f.read()
old = '欢迎关注顶红体育。\\\\\\\\n\\\\\\\\n' + '看直播：点击【看直播】→【最近直播】，或直接回复"最近直播"。' + '\\\\\\\\n' + '查赛事：回复球队名、联赛名，例如：阿森纳、巴黎、湖人。' + '\\\\\\\\n' + '看内容：点击【看内容】查看今日推荐和昨日复盘。' + '\\\\\\\\n\\\\\\\\n' + '如直播页加载较慢，请进入页面后点击"刷新直播"。'
new = '欢迎关注顶红体育。\\\\\\\\n\\\\\\\\n' + '看直播：点击【看直播】→【最近直播】，或直接回复"最近直播"。' + '\\\\\\\\n' + '看内容：点击【看内容】查看今日推荐和昨日复盘。' + '\\\\\\\\n\\\\\\\\n' + '如直播页加载较慢，请进入页面后点击"刷新直播"。'
if old in c:
    c = c.replace(old, new)
    with open('{WSRC}', 'w') as f: f.write(c)
    print('OK_subscribe_fixed')
else:
    print('NOT_FOUND_subscribe_old_text')
"
'''
run(cmd)

# ==========================================
# 2. Fix MatchDbService.java - multiple spots
# ==========================================
print("\n===== Fix MatchDbService =====")

# Read the file first
sftp = ssh.open_sftp()
with sftp.file(MSRC, "r") as f:
    content = f.read().decode("utf-8")
sftp.close()

changes = 0

# 2a. Remove searchHelp / liveHelp calls in reply()
old = '''        if ("查找比赛".equals(content) || "查直播".equals(content)) return searchHelp();
        if ("直播说明".equals(content)) return liveHelp();'''
new = '''        // 查找比赛/直播说明 已移除'''
if old in content:
    content = content.replace(old, new)
    changes += 1

# 2b. Remove searchMatch fallback, keep only aiCustomer
old = '''        String match = searchMatch(content);
        if (!empty(match)) return match;

        return aiCustomer(content);'''
new = '''        return aiCustomer(content);'''
if old in content:
    content = content.replace(old, new)
    changes += 1

# 2c. Fix recentLiveList - remove "回复数字即可获取二维码"
old = '            sb.append("\\u56de\\u590d\\u6570\\u5b57\\u5373\\u53ef\\u83b7\\u53d6\\u4e8c\\u7ef4\\u7801\\uff1a");'
# That's the unicode escape for "回复数字即可获取二维码："
# Actually just check for the actual Chinese text
old2 = '            sb.append("回复数字即可获取二维码：");'
new2 = '            sb.append("回复数字获取对应直播二维码：");'
if old2 in content:
    content = content.replace(old2, new2)
    changes += 1

# Also fix the "也可以直接回复球队名" line
old3 = '            sb.append("\\n");\n            sb.append("\\u4e5f\\u53ef\\u4ee5\\u76f4\\u63a5\\u56de\\u590d\\u7403\\u961f\\u540d\\u3002");'
if old3 in content:
    content = content.replace(old3, '')
    changes += 1

# Try direct Chinese match
old3b = 'sb.append("\\n");\n            sb.append("也可以直接回复球队名。");'
if old3b in content:
    content = content.replace(old3b, '')
    changes += 1

# 2d. Fix welcome()
old = '''private String welcome() {
        return "欢迎来到顶红体育。\\n\\n"
                + "看直播：发送"最近直播"，查看当前可观看赛事。\\n"
                + "查比赛：发送球队名、联赛名，查询相关赛事。\\n"
                + "看内容：点击"今日推荐 / 昨日复盘"，查看最新观点与赛后复盘。\\n\\n"
                + "常用指令：最近直播、今日足球、今日篮球、查找比赛、直播说明。";
    }'''
new = '''private String welcome() {
        return "欢迎来到顶红体育。\\n\\n"
                + "看直播：发送"最近直播"，查看当前可观看赛事。\\n"
                + "看内容：点击菜单【看内容】，查看今日推荐与昨日复盘。\\n\\n"
                + "常用指令：最近直播、今日推荐、昨日复盘。";
    }'''
if old in content:
    content = content.replace(old, new)
    changes += 1

# 2e. Fix defaultNotFound()
old = '''    private String defaultNotFound() {
        return "暂未找到相关内容。\\n\\n你可以发送"最近直播"查看直播入口，或发送球队名 / 联赛名查询赛事。";
    }'''
new = '''    private String defaultNotFound() {
        return "暂未找到相关内容。\\n\\n你可以发送"最近直播"查看直播入口，或点击菜单查看今日推荐与昨日复盘。";
    }'''
if old in content:
    content = content.replace(old, new)
    changes += 1

# 2f. Fix aiCustomer prompt
old = '''                    + "如果用户询问具体直播，提醒他发送球队名或点击【看直播】菜单。"
                    + "如果用户询问预测，只能说可以查看【看内容】里的今日推荐和昨日复盘。";'''
new = '''                    + "如果用户询问具体直播，提醒他点击【看直播】菜单查看最近直播。"
                    + "如果用户询问预测，提醒他点击【看内容】里的今日推荐和昨日复盘。";'''
if old in content:
    content = content.replace(old, new)
    changes += 1

# 2g. Keep the recent live list text about numbers but remove keyword reference
# The "也可以直接回复球队名" might need to be found differently
# Let me search for the exact text
for i, line in enumerate(content.split('\n')):
    if '也可以直接回复球队名' in line:
        print(f"  Found keyword-ref line {i+1}: {line.strip()}")
        # Replace the whole line
        old_line = line
        new_line = ''
        content = content.replace(old_line, new_line)
        changes += 1
        break

print(f"  Total changes in MatchDbService: {changes}")

# Write back
sftp = ssh.open_sftp()
with sftp.file(MSRC, "w") as f:
    f.write(content)
sftp.close()
print("  MatchDbService.java saved")

ssh.close()
print("\nAll done - now compile and restart")