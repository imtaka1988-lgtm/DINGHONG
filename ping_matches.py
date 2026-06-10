import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("8.210.102.206", 22, "root", "Taka888.", timeout=15)

sftp = ssh.open_sftp()
content = sftp.file("/data/dinghong/admin/matches.html", "r").read().decode("utf-8")
sftp.close()

# Save locally for inspection
with open("server_matches.html", "w", encoding="utf-8") as f:
    f.write(content)

# Check for common JS issues
lines = content.split('\n')
print(f"Lines: {len(lines)}")

# Check script tag starts/ends
script_starts = [i for i, l in enumerate(lines) if '<script>' in l]
script_ends = [i for i, l in enumerate(lines) if '</script>' in l]
print(f"<script> at lines: {script_starts}")
print(f"</script> at lines: {script_ends}")

# Check for unclosed strings or syntax issues in JS section
in_script = False
js_lines = []
for i, line in enumerate(lines):
    if '<script>' in line:
        in_script = True
        continue
    if '</script>' in line:
        in_script = False
        continue
    if in_script:
        js_lines.append((i+1, line))

# Look for potential issues
print(f"\nJS lines count: {len(js_lines)}")
for num, line in js_lines:
    stripped = line.strip()
    # Check for single backslash issues
    if stripped.count('\\') > 0 and '\\' not in stripped:
        print(f"  WARN line {num}: suspicious backslash: {stripped[:80]}")

# Print last 20 JS lines
print("\n=== Last 20 JS lines ===")
for num, line in js_lines[-20:]:
    print(f"  {num}: {line.rstrip()}")

# Print first 30 JS lines
print("\n=== First 30 JS lines ===")
for num, line in js_lines[:30]:
    print(f"  {num}: {line.rstrip()}")

ssh.close()