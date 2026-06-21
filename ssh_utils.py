# -*- coding: utf-8 -*-
"""
顶红公众号 — SSH 公共工具模块
提供统一的 SSH 连接和远程命令执行能力。
"""

import paramiko

from deploy_config import HOST, PORT, USER, PASS, SSH_TIMEOUT, SSH_LONG_TIMEOUT


def connect_ssh(timeout=None):
    """创建并返回已认证的 SSH 连接。"""
    if timeout is None:
        timeout = SSH_TIMEOUT
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, PORT, USER, PASS, timeout=timeout)
    return ssh


def run_cmd(ssh, cmd, timeout=None, echo=True):
    """在远程执行一条命令，返回 (stdout_str, stderr_str)。
    - timeout: 默认使用 SSH_LONG_TIMEOUT。
    - echo: 是否在本地打印命令和输出。
    """
    if timeout is None:
        timeout = SSH_LONG_TIMEOUT

    if echo:
        preview = cmd[:140]
        print(f"  >>> {preview}")

    chan = ssh.get_transport().open_session()
    chan.exec_command(cmd)
    chan.settimeout(timeout)

    out_bytes = b""
    err_bytes = b""
    while not chan.exit_status_ready():
        if chan.recv_ready():
            out_bytes += chan.recv(65536)
        if chan.recv_stderr_ready():
            err_bytes += chan.recv_stderr(65536)
    while chan.recv_ready():
        out_bytes += chan.recv(65536)
    while chan.recv_stderr_ready():
        err_bytes += chan.recv_stderr(65536)

    stdout = out_bytes.decode("utf-8", errors="replace")
    stderr = err_bytes.decode("utf-8", errors="replace")
    if echo:
        if stdout.strip():
            print(stdout.strip())
        if stderr.strip():
            print(stderr.strip())
    return stdout, stderr


def kill_process(ssh, pattern):
    """按进程名杀掉匹配的进程，返回被杀的 PID 列表。"""
    stdout, _ = run_cmd(
        ssh,
        f"ps aux | grep '{pattern}' | grep -v grep | awk '{{print $2}}'",
        echo=False
    )
    pids = [p.strip() for p in stdout.strip().split('\n') if p.strip()]
    for pid in pids:
        run_cmd(ssh, f"kill -15 {pid} 2>/dev/null", echo=False)
        run_cmd(ssh, f"sleep 1; kill -9 {pid} 2>/dev/null", echo=False)
    return pids


def upload_file(ssh, local_path, remote_path):
    """通过 SFTP 上传单个文件。"""
    sftp = ssh.open_sftp()
    try:
        sftp.put(local_path, remote_path)
        print(f"  UPLOAD: {local_path} -> {remote_path}")
    finally:
        sftp.close()
