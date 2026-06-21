# 数据库初始化说明

## 已知表清单

| 表名 | 用途 | 来源 |
|------|------|------|
| match_live | 直播比赛信息 | dinghong_db.sql |
| wechat_greeting_config | 欢迎语配置 | greeting_migration.sql |
| user_daily_greeting | 每日欢迎语记录 | greeting_migration.sql |
| articles | 公众号文章 | dinghong_db.sql (待确认) |

## 执行顺序

1. `顶红体育/dinghong_db.sql` — 建库建表
2. `顶红体育/greeting_migration.sql` — 欢迎语功能表

## 已知缺失

- 没有完整 schema。如新增服务器，推荐从旧服务器导出：
  ```bash
  mysqldump -u root -p --no-data dinghong > schema.sql
  ```
- article 相关表结构需确认
