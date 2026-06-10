import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("8.210.102.206", 22, "root", "Taka888.", timeout=15)

SRC = "/data/dinghong/app/dinghong-api/src/main/java/com/dinghong/service/MatchDbService.java"

# Read current file
sftp = ssh.open_sftp()
content = sftp.file(SRC, "r").read().decode("utf-8")
sftp.close()

# Find the keyword QR matching block in getImageMediaId() and comment it out
# The section to remove: // 3. User sends team name → match QR
old_block = """        // 3. 用户直接回复球队名 / 联赛名 / 关键词，直接返回匹配比赛二维码海报
        if (!isMenuCommand(content) && !isLiveIndexCommand(content)) {
            String mediaId = getMatchMediaIdByKeyword(content);
            if (!empty(mediaId)) return mediaId;
        }"""

new_block = """        // 3. [已移除] 用户直接回复球队名/联赛名的关键词二维码匹配功能"""

if old_block in content:
    content = content.replace(old_block, new_block)
    print("KEYWORD_QR_BLOCK removed")
else:
    # Try without the comment line
    old2 = """        if (!isMenuCommand(content) && !isLiveIndexCommand(content)) {
            String mediaId = getMatchMediaIdByKeyword(content);
            if (!empty(mediaId)) return mediaId;
        }"""
    if old2 in content:
        content = content.replace(old2, "        // [keyword qr removed]")
        print("KEYWORD_QR removed (alt)")
    else:
        print("NOT FOUND in file, searching...")
        for i, line in enumerate(content.split('\n')):
            if 'getMatchMediaIdByKeyword' in line:
                print(f"  line {i+1}: {line.strip()}")
            if 'isMenuCommand' in line and 'isLiveIndexCommand' in line:
                print(f"  line {i+1}: {line.strip()}")

# Write back
sftp = ssh.open_sftp()
with sftp.file(SRC, "w") as f:
    f.write(content)
sftp.close()
print("File saved")

ssh.close()
print("Done")