import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("8.210.102.206", 22, "root", "Taka888.", timeout=15)

# Check article status
cmd = r'docker exec dinghong-mysql mysql -uroot -pDingHong@2026 dinghong -e "SELECT id,title,status,wechat_draft_media_id FROM article_task ORDER BY id DESC LIMIT 5" 2>&1'
i, o, e = ssh.exec_command(cmd, timeout=10)
print("=== Articles ===")
print(o.read().decode(errors='replace'))
err = e.read().decode(errors='replace')
if err: print("ERR:", err[:300])

# Test publish endpoint
cmd2 = 'curl -s -X POST http://127.0.0.1:8080/editor/wechat-publish/103 2>&1'
i2, o2, e2 = ssh.exec_command(cmd2, timeout=15)
print("\n=== Publish Test (id=103) ===")
print(o2.read().decode(errors='replace'))

ssh.close()