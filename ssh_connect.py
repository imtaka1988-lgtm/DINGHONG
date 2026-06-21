import paramiko
import os
import sys
import select
import socket
import termios
import tty

from deploy_config import HOST as hostname, PORT as port, USER as username, PASS as password

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

print(f"正在连接到 {username}@{hostname}:{port} ...")
try:
    client.connect(hostname, port=port, username=username, password=password, timeout=15)
    print("连接成功！\n")
except Exception as e:
    print(f"连接失败: {e}")
    sys.exit(1)

channel = client.invoke_shell()
print("已进入交互式 shell，输入 exit 退出。\n")

# 保存原始终端设置
oldtty = termios.tcgetattr(sys.stdin)
try:
    tty.setraw(sys.stdin.fileno())
    tty.setcbreak(sys.stdin.fileno())
    channel.settimeout(0.0)

    while True:
        r, w, e = select.select([channel, sys.stdin], [], [])
        if channel in r:
            try:
                data = channel.recv(1024)
                if len(data) == 0:
                    print("\n连接已断开。")
                    break
                sys.stdout.write(data.decode('utf-8', errors='replace'))
                sys.stdout.flush()
            except socket.timeout:
                pass
        if sys.stdin in r:
            x = os.read(sys.stdin.fileno(), 1024)
            if len(x) == 0:
                break
            channel.send(x)
finally:
    termios.tcsetattr(sys.stdin, termios.TCSADRAIN, oldtty)
    channel.close()
    client.close()
    print("\n连接已关闭。")
