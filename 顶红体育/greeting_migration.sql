-- 每日欢迎语配置表
DROP TABLE IF EXISTS `wechat_greeting_config`;
CREATE TABLE `wechat_greeting_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `qr_image_url` varchar(500) DEFAULT NULL COMMENT '二维码图片链接',
  `greeting_text` varchar(500) DEFAULT NULL COMMENT '欢迎文字',
  `enabled` tinyint DEFAULT 1 COMMENT '是否启用 0关闭 1开启',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 默认记录
INSERT INTO `wechat_greeting_config` (`id`, `qr_image_url`, `greeting_text`, `enabled`) VALUES
(1, '', '欢迎加入顶红体育交流群，扫码入群领取每日精选赛事分析', 1);

-- 用户每日互动记录表
DROP TABLE IF EXISTS `user_daily_greeting`;
CREATE TABLE `user_daily_greeting` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `openid` varchar(128) NOT NULL COMMENT '微信用户OpenID',
  `greeting_date` date NOT NULL COMMENT '触发日期',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_openid_date` (`openid`, `greeting_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;