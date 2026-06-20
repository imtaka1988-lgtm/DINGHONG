# SQL 运维脚本

本目录用于保存需要人工执行的数据库修正脚本。

## 20260620_author_prompt_diversity.sql

用途：更新 `ai_prompt` 表中四位作者的提示词，让“每日推荐 / 每日复盘”减少同质化。

涉及 prompt_code：

```text
chief_editor       老周｜稳健主编
basketball_editor  阿凯｜篮球节奏派
football_editor    老唐｜足球盘口派
analyst_editor     小北｜数据观察派
```

执行前建议备份：

```sql
CREATE TABLE ai_prompt_backup_20260620 AS SELECT * FROM ai_prompt;
```

执行方式示例：

```bash
mysql -u dinghong -p dinghong < app/dinghong-api/sql/20260620_author_prompt_diversity.sql
```

注意：

```text
1. 该 SQL 不改文章历史数据，只改后续生成时使用的作者提示词。
2. 当前 Java 代码会按 author 映射到 prompt_code，所以执行后立即影响新生成文章。
3. 这不是最终完整方案；后续还应继续改 EditorService，让作者参数进入推荐后处理。
```
