import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("8.210.102.206", 22, "root", "Taka888.", timeout=15)

sftp = ssh.open_sftp()
content = sftp.file("/data/dinghong/admin/matches.html", "r").read().decode("utf-8")
sftp.close()

print(f"Total lines: {len(content.splitlines())}")
print(f"Contains 'val(': {'val(' in content}")
print(f"Contains 'DH_API': {'DH_API' in content}")
print(f"Contains 'load()': {'load()' in content}")
print(f"Contains '<script>': {'<script>' in content}")
print(f"Contains 'js/common.js': {'js/common.js' in content}")

# Show script section
idx = content.find("<script>")
if idx >= 0:
    print("\n=== Script section (first 800 chars) ===")
    print(content[idx:idx+800])

ssh.close()