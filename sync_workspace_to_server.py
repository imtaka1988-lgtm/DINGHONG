"""同步工作区文件到服务器 8.210.102.206"""
import paramiko
import os
import sys
import stat
import warnings

warnings.filterwarnings('ignore')

HOST = "8.210.102.206"
PORT = 22
USER = "root"
PASS = "Taka888."
LOCAL_DIR = r"c:\Users\Administrator\Desktop\HOTY-相关\顶红公众号"
REMOTE_DIR = "/root/dinghong"

# 要同步的子目录（排除.git, node_modules等）
SYNC_DIRS = ["顶红体育", "app"]

def connect_sftp():
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(HOST, PORT, USER, PASS, timeout=15, look_for_keys=False, allow_agent=False)
    sftp = client.open_sftp()
    return client, sftp

def ensure_remote_dir(sftp, remote_path):
    """递归创建远程目录"""
    parts = remote_path.replace('\\', '/').strip('/').split('/')
    current = ''
    for p in parts:
        current += '/' + p
        try:
            sftp.stat(current)
        except FileNotFoundError:
            sftp.mkdir(current)
            print(f"  创建目录: {current}")

def upload_file(sftp, local_path, remote_path):
    """上传单个文件"""
    try:
        sftp.put(local_path, remote_path)
        return True
    except Exception as e:
        print(f"  上传失败 {local_path}: {e}")
        return False

def sync_directory(sftp, local_dir, remote_dir):
    """递归同步目录"""
    count = 0
    for root, dirs, files in os.walk(local_dir):
        # 跳过不需要同步的目录
        dirs[:] = [d for d in dirs if d not in ['.git', '__pycache__', 'node_modules', '.vscode']]

        rel_path = os.path.relpath(root, local_dir)
        if rel_path == '.':
            remote_sub = remote_dir
        else:
            remote_sub = os.path.join(remote_dir, rel_path).replace('\\', '/')

        ensure_remote_dir(sftp, remote_sub)

        for f in files:
            if f.endswith(('.pyc', '.log')):
                continue
            local_file = os.path.join(root, f)
            remote_file = os.path.join(remote_sub, f).replace('\\', '/')
            try:
                # 检查远程文件是否更新
                need_upload = True
                try:
                    remote_stat = sftp.stat(remote_file)
                    local_mtime = os.path.getmtime(local_file)
                    if abs(remote_stat.st_mtime - local_mtime) < 1:
                        need_upload = False
                except FileNotFoundError:
                    pass

                if need_upload:
                    if upload_file(sftp, local_file, remote_file):
                        count += 1
                        print(f"  [OK] {rel_path}/{f}")
                else:
                    # 跳过未修改的文件，减少输出
                    pass
            except Exception as e:
                print(f"  [FAIL] {rel_path}/{f}: {e}")
    return count

def main():
    print(f"\n{'='*60}")
    print(f"同步工作区到服务器 {HOST}")
    print(f"本地: {LOCAL_DIR}")
    print(f"远程: {REMOTE_DIR}")
    print(f"{'='*60}\n")

    try:
        client, sftp = connect_sftp()
        print("SSH 连接成功!\n")

        # 确保远程根目录存在
        ensure_remote_dir(sftp, REMOTE_DIR)

        total = 0
        # 同步根目录文件（Python脚本等）
        print("--- 同步根目录文件 ---")
        for f in os.listdir(LOCAL_DIR):
            local_path = os.path.join(LOCAL_DIR, f)
            if os.path.isfile(local_path) and not f.startswith('.'):
                remote_path = f"{REMOTE_DIR}/{f}"
                if upload_file(sftp, local_path, remote_path):
                    total += 1
                    print(f"  [OK] {f}")

        # 同步子目录
        for d in SYNC_DIRS:
            local_sub = os.path.join(LOCAL_DIR, d)
            if os.path.isdir(local_sub):
                print(f"\n--- 同步目录: {d} ---")
                try:
                    cnt = sync_directory(sftp, local_sub, f"{REMOTE_DIR}/{d}")
                    total += cnt
                except Exception as e:
                    print(f"  同步 {d} 失败: {e}")

        print(f"\n{'='*60}")
        print(f"同步完成! 共上传 {total} 个文件")
        print(f"{'='*60}")

        # 检查服务器上的项目
        print("\n--- 服务器状态 ---")
        stdin, stdout, stderr = client.exec_command(
            f'echo "远程目录:" && ls -la {REMOTE_DIR}/ && echo "" && '
            f'echo "Docker容器:" && docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || echo "无Docker"'
        )
        print(stdout.read().decode())

        sftp.close()
        client.close()

    except Exception as e:
        print(f"连接失败: {e}")
        sys.exit(1)

if __name__ == '__main__':
    main()
