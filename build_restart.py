import paramiko
import time

s = paramiko.SSHClient()
s.set_missing_host_key_policy(paramiko.AutoAddPolicy())
s.connect('8.210.102.206', username='root', password='Taka888.', timeout=120)

print('Starting Maven build (may take 1-2 min)...')

stdin, stdout, stderr = s.exec_command(
    'cd /data/dinghong/app/dinghong-api '
    '&& JAVA_HOME=/usr/lib/jvm/java-17-openjdk-17.0.19.0.10-1.0.2.1.al8.x86_64 '
    'mvn clean package -DskipTests -q 2>&1',
    timeout=120
)
exit_code = stdout.channel.recv_exit_status()
out = stdout.read().decode()
err = stderr.read().decode()

print('Exit code: ' + str(exit_code))
if err:
    print('STDERR (last 300 chars):')
    print(err[-300:])
if out:
    print('STDOUT (last 300 chars):')
    print(out[-300:])

# Check jar
stdin2, stdout2, stderr2 = s.exec_command(
    'ls -la /data/dinghong/app/dinghong-api/target/dinghong-api-1.0.0.jar '
    '2>/dev/null'
)
jar_info = stdout2.read().decode().strip()

if jar_info:
    print('\nBuild SUCCESS: ' + jar_info)

    # Kill old process
    s.exec_command(
        'kill $(ps aux | grep dinghong-api-1.0.0.jar '
        '| grep -v grep | awk \'{print $2}\') 2>/dev/null'
    )
    print('Killed old process')
    time.sleep(2)

    # Start new
    s.exec_command(
        'cd /data/dinghong/app/dinghong-api '
        '&& nohup java -jar target/dinghong-api-1.0.0.jar > api.log 2>&1 &'
    )
    time.sleep(5)

    # Verify
    stdin3, stdout3, stderr3 = s.exec_command(
        'ps aux | grep dinghong-api | grep -v grep'
    )
    proc = stdout3.read().decode().strip()
    if proc:
        print('Process running: ' + proc[:150])
    else:
        # Check log for errors
        stdin4, stdout4, stderr4 = s.exec_command(
            'tail -20 /data/dinghong/app/dinghong-api/api.log 2>/dev/null'
        )
        log = stdout4.read().decode().strip()
        print('Process not detected. API log tail:')
        print(log[-500:])
else:
    print('\nBuild FAILED! Checking target dir:')
    stdin5, stdout5, stderr5 = s.exec_command(
        'ls -la /data/dinghong/app/dinghong-api/target/ 2>/dev/null'
    )
    print(stdout5.read().decode().strip())

s.close()