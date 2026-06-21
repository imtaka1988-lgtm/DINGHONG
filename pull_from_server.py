"""从服务器拉取文件到本地工作区"""
import paramiko
import os
import sys
import warnings

warnings.filterwarnings('ignore')

from deploy_config import (HOST, PORT, USER, PASS,
                           REMOTE_PROJECT_DIR as REMOTE_DIR,
                           LOCAL_BASE as LOCAL_DIR,
                           EXCLUDE_DIRS, EXCLUDE_EXT)

def connect_sftp():
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(HOST, PORT, USER, PASS, timeout=15, look_for_keys=False, allow_agent=False)
    sftp = client.open_sftp()
    return client, sftp

def ensure_local_dir(path):
    if not os.path.exists(path):
        os.makedirs(path)

def should_exclude(path, is_dir):
    name = os.path.basename(path)
    if name in EXCLUDE_DIRS:
        return True
    if name.startswith('.'):
        return True
    if not is_dir:
        for ext in EXCLUDE_EXT:
            if name.endswith(ext):
                return True
    return False

def download_file(sftp, remote_path, local_path):
    try:
        sftp.get(remote_path, local_path)
        return True
    except Exception as e:
        print(f"  下载失败 {remote_path}: {e}")
        return False

def pull_directory(sftp, remote_dir, local_dir):
    """递归拉取目录"""
    count = 0
    skipped = 0
    errors = 0

    try:
        items = sftp.listdir_attr(remote_dir)
    except Exception as e:
        print(f"  无法列出目录 {remote_dir}: {e}")
        return 0, 0, 0

    for item in items:
        name = item.filename
        remote_path = f"{remote_dir}/{name}"
        local_path = os.path.join(local_dir, name)

        if should_exclude(name, item.st_mode is not None and (item.st_mode & 0o40000)):
            skipped += 1
            continue

        # 检查是否是目录 (S_IFDIR = 0o40000)
        if item.st_mode is not None and (item.st_mode & 0o40000):
            ensure_local_dir(local_path)
            c, s, e = pull_directory(sftp, remote_path, local_path)
            count += c
            skipped += s
            errors += e
        else:
            # 检查远程文件是否比本地新
            try:
                local_mtime = os.path.getmtime(local_path)
                if abs(item.st_mtime - local_mtime) < 2:
                    skipped += 1
                    continue
            except (OSError, FileNotFoundError):
                pass

            if download_file(sftp, remote_path, local_path):
                count += 1
                rel = os.path.relpath(remote_path, REMOTE_DIR)
                print(f"  [OK] {rel}")
            else:
                errors += 1

    return count, skipped, errors

def main():
    print(f"\n{'='*60}")
    print(f"从服务器拉取文件到本地")
    print(f"远程: {REMOTE_DIR}")
    print(f"本地: {LOCAL_DIR}")
    print(f"排除: {EXCLUDE_DIRS}")
    print(f"{'='*60}\n")

    try:
        client, sftp = connect_sftp()
        print("SSH 连接成功!\n")

        total, skipped, errors = pull_directory(sftp, REMOTE_DIR, LOCAL_DIR)

        print(f"\n{'='*60}")
        print(f"拉取完成!")
        print(f"  下载: {total} 个文件")
        print(f"  跳过: {skipped} 个文件")
        if errors:
            print(f"  失败: {errors} 个文件")
        print(f"{'='*60}")

        sftp.close()
        client.close()

    except Exception as e:
        print(f"连接失败: {e}")
        sys.exit(1)

if __name__ == '__main__':
    main()
