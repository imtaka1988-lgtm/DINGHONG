-- 顶红公众号：数据库结构初始化（附加统计表）
-- 运行方式示例：
--   mysql -u root -p dinghong < migrations/001_init_match_extra.sql

-- 比赛数据扩展表：球队/赛事近况统计
CREATE TABLE IF NOT EXISTS match_stats (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  match_id BIGINT UNSIGNED NOT NULL,
  league VARCHAR(120) NOT NULL DEFAULT '',
  season VARCHAR(32) NOT NULL DEFAULT '',
  home_team VARCHAR(120) NOT NULL DEFAULT '',
  away_team VARCHAR(120) NOT NULL DEFAULT '',
  home_rating DECIMAL(5,2) NOT NULL DEFAULT 0,
  away_rating DECIMAL(5,2) NOT NULL DEFAULT 0,
  home_attack_eff DECIMAL(6,3) NOT NULL DEFAULT 0,
  away_attack_eff DECIMAL(6,3) NOT NULL DEFAULT 0,
  home_defense_eff DECIMAL(6,3) NOT NULL DEFAULT 0,
  away_defense_eff DECIMAL(6,3) NOT NULL DEFAULT 0,
  home_last5_win TINYINT UNSIGNED NOT NULL DEFAULT 0,
  away_last5_win TINYINT UNSIGNED NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_match_stats_match (match_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 市场动态：盘口 / 赔率变化
CREATE TABLE IF NOT EXISTS market_moves (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  match_id BIGINT UNSIGNED NOT NULL,
  market VARCHAR(32) NOT NULL DEFAULT '',       -- h2h / spreads / totals
  bookmaker VARCHAR(120) NOT NULL DEFAULT '',
  open_price_home DECIMAL(8,3) DEFAULT NULL,
  open_price_away DECIMAL(8,3) DEFAULT NULL,
  open_point_home DECIMAL(8,3) DEFAULT NULL,
  open_point_away DECIMAL(8,3) DEFAULT NULL,
  live_price_home DECIMAL(8,3) DEFAULT NULL,
  live_price_away DECIMAL(8,3) DEFAULT NULL,
  live_point_home DECIMAL(8,3) DEFAULT NULL,
  live_point_away DECIMAL(8,3) DEFAULT NULL,
  last_update TIMESTAMP NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  KEY idx_market_moves_match (match_id),
  KEY idx_market_moves_last_update (last_update)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 伤停信息
CREATE TABLE IF NOT EXISTS injuries (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  match_id BIGINT UNSIGNED NOT NULL,
  team VARCHAR(120) NOT NULL DEFAULT '',
  player_name VARCHAR(120) NOT NULL DEFAULT '',
  position VARCHAR(60) NOT NULL DEFAULT '',
  status VARCHAR(60) NOT NULL DEFAULT '',
  note VARCHAR(255) NOT NULL DEFAULT '',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_injuries_match (match_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
