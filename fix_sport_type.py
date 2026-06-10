import paramiko

s = paramiko.SSHClient()
s.set_missing_host_key_policy(paramiko.AutoAddPolicy())
s.connect('8.210.102.206', username='root', password='Taka888.', timeout=15)

def run(cmd):
    stdin, stdout, stderr = s.exec_command(cmd)
    return stdout.read().decode().strip()

# 1. Check match_live for Barcelona / Tenerife
print('1. match_live table:')
out = run(
    "docker exec dinghong-mysql mysql -uroot -p'DingHong@2026' dinghong "
    '-e "SELECT id, home_team, away_team, league_name FROM match_live '
    "WHERE home_team LIKE '%巴塞罗那%' OR away_team LIKE '%巴塞罗那%' "
    "OR home_team LIKE '%特内%' OR away_team LIKE '%特内%' LIMIT 10;\" 2>&1"
)
print(out)

# 2. Check match_live table has sport_type column
print('\n2. match_live columns:')
out = run(
    "docker exec dinghong-mysql mysql -uroot -p'DingHong@2026' dinghong "
    '-e "DESCRIBE match_live;" 2>&1'
)
print(out)

# 3. Read current detectSportType code
print('\n3. Current detectSportType:')
out = run(
    'sed -n "211,242p" '
    '/data/dinghong/app/dinghong-api/src/main/java/com/dinghong/'
    'controller/editor/ArticleController.java'
)
print(out)

# 4. Read resolveSportType 
print('\n4. Current resolveSportType:')
out = run(
    'sed -n "184,209p" '
    '/data/dinghong/app/dinghong-api/src/main/java/com/dinghong/'
    'controller/editor/ArticleController.java'
)
print(out)

# 5. Check what the generated article sport_type was
print('\n5. Recent articles sport_type:')
out = run(
    "docker exec dinghong-mysql mysql -uroot -p'DingHong@2026' dinghong "
    '-e "SELECT id, title, article_category, sport_type FROM article_task '
    'WHERE sport_type=\'football\' '
    'AND (title LIKE \'%巴塞%\' OR title LIKE \'%特内%\') '
    'ORDER BY id DESC LIMIT 5;" 2>&1'
)
print(out)

s.close()