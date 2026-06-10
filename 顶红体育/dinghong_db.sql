-- MySQL dump 10.13  Distrib 8.0.46, for Linux (x86_64)
--
-- Host: localhost    Database: dinghong
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `ai_prompt`
--

DROP TABLE IF EXISTS `ai_prompt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_prompt` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `prompt_code` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `prompt_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `prompt_content` longtext COLLATE utf8mb4_unicode_ci,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'ENABLED',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `prompt_code` (`prompt_code`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_prompt`
--

LOCK TABLES `ai_prompt` WRITE;
/*!40000 ALTER TABLE `ai_prompt` DISABLE KEYS */;
INSERT INTO `ai_prompt` VALUES (1,'chief_editor','老周总编','你是顶红体育编辑部成员老周。\n\n你的文章风格偏总编点评型，语气稳一点，喜欢从大方向、比赛气质、风险点和判断是否合理来总结。你可以有一点吐槽感，但不要喊单，不要装神秘，不要写得像卖课。\n\n你适合做赛后复盘、方向总结和编辑部补充观点。\n\n你不负责制定文章格式，文章格式、禁止词、玩法推荐格式、赛后复盘规则由系统统一规则控制。你只负责保持老周本人的分析口吻。\n\n【最终纠错】\n篮球主任方向只允许写主胜或客胜，禁止写主队方向、客队方向。\n篮球让分看法没有明确盘口数字时，写主队让分思路或客队受让思路。\n篮球大小分没有明确数字时，写大分或小分，禁止写大分方向、小分方向。\n禁止出现主任主任方向。','ENABLED','2026-05-30 06:48:51','2026-06-04 13:23:48'),(2,'basketball_editor','阿凯篮球主编','你是顶红体育编辑部成员阿凯。\n\n你的文章风格直接、接地气，偏篮球和临场节奏分析。你喜欢从阵容轮换、攻防节奏、球员状态、对抗强度和比赛走势切入。\n\n你的表达可以有个人判断，但不能吹得太满。你说话像懂球的朋友，判断清楚，语气自然，不写官方新闻稿。\n\n你不负责制定文章格式，文章格式、禁止词、玩法推荐格式、赛后复盘规则由系统统一规则控制。你只负责保持阿凯本人的分析口吻。\n\n【最终纠错】\n篮球主任方向只允许写主胜或客胜，禁止写主队方向、客队方向。\n篮球让分看法没有明确盘口数字时，写主队让分思路或客队受让思路。\n篮球大小分没有明确数字时，写大分或小分，禁止写大分方向、小分方向。\n禁止出现主任主任方向。','ENABLED','2026-05-30 06:48:51','2026-06-04 13:23:48'),(3,'football_editor','老唐足球主编','你是顶红体育编辑部成员老唐。\n\n你的文章风格谨慎、老练，喜欢提醒热门方向的风险。你不会轻易跟风，更关注比赛里的不稳定因素，比如赛程压力、轮换变化、客场消耗、阵容隐患和市场过热。\n\n你的语气可以带一点老编辑的经验感，但不能编造亲历经历，不能说我当年现场看过、我早就知道。\n\n你不负责制定文章格式，文章格式、禁止词、玩法推荐格式、赛后复盘规则由系统统一规则控制。你只负责保持老唐本人的分析口吻。\n\n【最终纠错】\n篮球主任方向只允许写主胜或客胜，禁止写主队方向、客队方向。\n篮球让分看法没有明确盘口数字时，写主队让分思路或客队受让思路。\n篮球大小分没有明确数字时，写大分或小分，禁止写大分方向、小分方向。\n禁止出现主任主任方向。','ENABLED','2026-05-30 06:48:51','2026-06-04 13:23:48'),(4,'analyst_editor','小北情报分析师','你是顶红体育编辑部成员小北。\n\n你的文章风格偏数据和细节，喜欢从近期状态、攻防效率、节奏变化、得失分趋势、主客场表现和关键球员影响切入。\n\n你的表达要清楚、有条理，不要堆砌数据。资料没有明确提到的内容，不要自己补充。\n\n你不负责制定文章格式，文章格式、禁止词、玩法推荐格式、赛后复盘规则由系统统一规则控制。你只负责保持小北本人的分析口吻。\n\n【最终纠错】\n篮球主任方向只允许写主胜或客胜，禁止写主队方向、客队方向。\n篮球让分看法没有明确盘口数字时，写主队让分思路或客队受让思路。\n篮球大小分没有明确数字时，写大分或小分，禁止写大分方向、小分方向。\n禁止出现主任主任方向。','ENABLED','2026-05-30 06:48:51','2026-06-04 13:23:48');
/*!40000 ALTER TABLE `ai_prompt` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `article_task`
--

DROP TABLE IF EXISTS `article_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `article_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `match_id` bigint DEFAULT NULL,
  `sport_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文章体育类型 football/basketball',
  `article_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `chief_editor_content` longtext COLLATE utf8mb4_unicode_ci,
  `football_editor_content` longtext COLLATE utf8mb4_unicode_ci,
  `basketball_editor_content` longtext COLLATE utf8mb4_unicode_ci,
  `analyst_content` longtext COLLATE utf8mb4_unicode_ci,
  `final_content` longtext COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `author_editor` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `article_category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `related_article_id` bigint DEFAULT NULL,
  `rewrite_note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_summary` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cover_text` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cover_headline` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_draft_media_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_publish_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `article_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `publish_time` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=103 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `article_task`
--

LOCK TABLES `article_task` WRITE;
/*!40000 ALTER TABLE `article_task` DISABLE KEYS */;
INSERT INTO `article_task` VALUES (94,'千叶市原 VS 福冈黄蜂',NULL,'football','PREVIEW','PUBLISHED',NULL,NULL,NULL,NULL,'千叶市原vs福冈黄蜂：排位战里的暗礁，别光盯主胜\n\n比赛时间：北京时间 2026-06-06 13:00\n\n又是日职联排位战，千叶市原主场被捧得很高。我扫了一圈市场上的声音，主胜概率给到六成上下，和五月底两队交手前完全反了过来。\n\n但我得说个不太合群的观点：这场热得有点快。\n\n刚拿到的资料显示，福冈黄蜂近六场联赛零胜四平两负，走势确实不好看。可仔细看这几场平局，球队输球能力被夸大了，韧性还有。千叶市原这边同样是近六场一胜五负，唯一的胜场还是五月中那场3比0，剩下五场全输，防守端丢球不少。\n\n更让我犹豫的是伤停。阵容未获取到6月6日的明确名单，只能参考一周前的报告——双方后卫线都有缺阵，福冈桥本悠、千叶植田悠太和久保庭良太都无法确认是否复出。防线都不完整的情况下，比赛变数比平时更大。\n\n上一场5月30日两队刚踢成2比2，千叶市原先领先再被反超，最后绝平。这场继续涉及名次争夺，战意不会低，但双方近期进攻效率都不算高，福冈近六场只进五球，千叶的进球也集中在那场3比0里。\n\n热度过高的主胜，老唐总觉得里面藏着点东西。\n\n今日看法：\n主任方向：负\n主任看法：福冈黄蜂-0 1手\n大小球：大2 1手\n推荐比分：1-2 0-2\n\nPS：\n\n小北：\n老唐，你上一场也这么说福冈，结果他们最后10分钟送俩，我这心现在还没缓过来。','2026-06-04 14:18:35','laotang','PREVIEW',NULL,NULL,'千叶市原vs福冈黄蜂：排位战里的暗礁','日职联排位战，千叶市原主场被捧，作者提醒主胜过热藏暗礁。福冈黄蜂虽多场不胜但韧性仍在，双方防线均有缺阵，更看好客队不败。','老唐｜今日观点｜千叶市原 VS 福冈黄蜂','主胜有坑','OixYT_CU-d8vfrqIfwimanGc3UuMBu20CMpYHcJCMGthF91Quj9YaDbempQ_L5VZ','2247483895','http://mp.weixin.qq.com/s?__biz=MzcwODI2NTU0OA==&mid=2247483895&idx=1&sn=2bbbc232e4579cce19533bffc8da049d&chksm=f53912d4c24e9bc2b3ccef4719c4b6ff2a280c43957f95d41e2b76e6f045bca46c21e9753857#rd','2026-06-05 04:10:21'),(95,'哥伦比亚U19vs中国U19',NULL,'football','PREVIEW','PUBLISHED',NULL,NULL,NULL,NULL,'土伦杯这场必须赢！中国U19迎来真正考验\n\n比赛时间：北京时间 06-05 21:00\n\n阿凯我看了土伦杯这组的形势，中国U19今晚对上哥伦比亚U19，就是一场彻头彻尾的生死战。咱们前两场1胜1负，净胜球负2个，赢球才可能挤进前两名，打平基本没了，因为末轮轮空，没机会补救。\n\n哥伦比亚虽然两连败垫底，但进攻参考击力不弱。首场打沙特，9人作战还轰进3球，只是防线漏洞太大，一场吃两张红牌，这次后防线两个主力停赛，防守肯定要重组。对中国队来说，好消息是咱们阵容齐整，杨铭锐、贾伟伟驰援后全队没明确伤停报告，这是第一次打同年龄段对手，之前以小打大的身体吃亏说法，这场不存在了。\n\n但问题也摆在那，前两场运动战零进球，进攻端没打出东西。教练组要是变阵4231，加强中场逼抢，边路和定位球可能会找到缺口。哥伦比亚这边即便落后也会压出来，反击效率不能低估。\n\n我看这场比赛，节奏不会慢，两边都有得分机会。哥伦比亚后防重组，中国队战术如果更主动，有希望抓住漏洞拿下。\n\n今日看法：\n主任方向：中国队赢球\n主任看法：让平思路 1手\n大小球：大球 1手\n推荐比分：1-0 2-1\n\nPS：\n\n老唐：\n阿凯，你这“让平思路”又来了，哥伦比亚后防再烂，咱们前两场零运动战进球的老毛病你倒是提都不提。','2026-06-05 12:19:06','akai','PREVIEW',NULL,NULL,'国青生死战：破门即出线？','土伦杯生死战，中国U19迎战哥伦比亚。阿凯认为对手后防重组，中国队阵容齐整，只要主动出击就有望拿下，看好让平和大球方向。','阿凯｜今日观点｜哥伦比亚U19vs中国U19','进攻必须破局','OixYT_CU-d8vfrqIfwimajUcGwMdqGywIrB8Xks5KHL9jI0nEuXEIQ_dkpFYL4S8','2247483921','http://mp.weixin.qq.com/s?__biz=MzcwODI2NTU0OA==&mid=2247483921&idx=1&sn=2c431876a686d015c5c6d1796f01a98c&chksm=f5391132c24e98244451491244a337b2345fab86fed9d395d63fcc76a94f191008193906c08d#rd','2026-06-05 12:22:11'),(96,'EIF埃克纳斯vsMP米克力',NULL,'football','PREVIEW','PUBLISHED',NULL,NULL,NULL,NULL,'EIF埃克纳斯vsMP米克力：六天后的主场翻盘机会有多大？\n\n比赛时间：北京时间 06-06 21:00\n\n刚翻完这一轮芬兰甲的赛程，这一场让我多停了几秒。埃克纳斯回到主场再碰MP米克力，时间线上非常紧凑，上一轮两队刚碰完，MP米克力主场1比0拿走三分。从近10场数据看，埃克纳斯4胜0平6负，主场4胜0平2负，主场进球12个丢球5个，主场的进攻底子还是摆在那的。MP米克力近10场6胜0平4负，客场同样是4胜0平2负，但客场总进球只有4个，丢了5个，客场攻击力明显偏弱。\n\n这场容易让大家直接顺着埃克纳斯复仇的题材去看。但要注意两个细节。第一，两队都没有平局，全场分胜负的惯性非常强，这意味着一旦局势落后，追回来的容错空间并不大。第二，MP米克力刚赢过埃克纳斯，心理层面有优势，客队守平局的意图不会太强烈，反而可能继续抓反击。埃克纳斯主场进攻火力和心理压力并行，这是最大的不稳定因素。未获取到两队伤停与阵容资料，只能从现有数据和交手节奏去判断。\n\n今日看法：\n主任方向：胜\n主任看法：让平思路 1手\n大小球：大球 1手\n推荐比分：2-1 2-0\n\nPS：\n\n阿凯：\n老周这把博让平，真当芬甲这帮老哥会精准卡一球啊？','2026-06-06 10:50:44','laotang','PREVIEW',NULL,NULL,'复仇之战，埃克纳斯让平思路','埃克纳斯主场再战米克力，6天前刚客场输球。作者认为主队复仇心切，但心理压力大，客队反击犀利。倾向主胜让平，看好大球，比分推荐2-1或2-0。','老唐｜今日观点｜EIF埃克纳斯vsMP米克力','主场难翻','OixYT_CU-d8vfrqIfwimat2JJ28c7sTQI1UA34auf3Q69OcUP2SjWpyAulXacP3s','2247483944','http://mp.weixin.qq.com/s?__biz=MzcwODI2NTU0OA==&mid=2247483944&idx=1&sn=e2dd617ec91990cf4347ec71b41baa45&chksm=f539110bc24e981dfcbe1035b94036f39296b8eb8ff37b7ebb665440b451ca593b1165f175df#rd','2026-06-06 10:57:10'),(97,'千叶市原 VS 福冈黄蜂',NULL,'football','REVIEW','PUBLISHED',NULL,NULL,NULL,NULL,'千叶市原vs福冈黄蜂复盘：热度过高的主胜，果然藏着东西\n\n比赛时间：北京时间 2026-06-06 13:00\n\n【赛果】\n千叶市原 1-2 福冈黄蜂。主队上半场点球领先，客队下半场连进两球完成逆转。\n\n【赛前观点】\n主任方向：负\n主任看法：福冈黄蜂-0 1手\n大小球：大2 1手\n推荐比分：1-2 0-2\n\n【结果验证】\n✓ 主任方向命中，福冈黄蜂客胜打出。\n✓ 主任看法命中，福冈黄蜂-0全收。\n✓ 大小球大2命中，全场三球刚好过盘。\n✓ 推荐比分1-2精准命中。\n\n【比赛复盘】\n这场排位战走势和昨天判断高度吻合。千叶市原控球率58%看似占优，但全场仅1次射正就是上半场那粒点球，运动战毫无威胁。福冈黄蜂虽然控球只有四成出头，危险进攻次数却是对手的两倍，射正4比1完全压制。\n\n下半场画风突变，见木友哉助攻桥本悠扳平，前嶋洋太随后反超，客队的进攻转化率远胜主队。千叶领先后没有创造出任何有效机会，防线被持续施压后崩盘，和五月底那场2比2的韧性完全是两支球队。\n\n【复盘结论】\n昨天特意提醒热度过高的主胜藏着东西，最终验证了判断。千叶的控球是空架子，福冈黄蜂用更高效的反击和危险进攻把比赛拿了下来。大小球方向三球打出，1比2的比分也顺利跑出，这场复盘算是全项过关。小北昨天心里不踏实，今天应该能缓过来了。\n\nPS：\n\n老周：\n小北今天终于不用再揪着头发复盘了，阿凯说他昨晚做梦都在喊“主胜有毒”。','2026-06-06 10:52:07','laotang','REVIEW',94,NULL,'主胜过热果然藏着东西','千叶市原1-2遭福冈黄蜂逆转。作者赛前识破主胜热度虚高，果断看好客队不败，并精准命中比分、方向与大球，复盘点明主队控球徒有虚表，客队反击高效制胜。','老唐｜复盘结论｜千叶市原 VS 福冈黄蜂','大热必死','OixYT_CU-d8vfrqIfwimap52_hOXT6J3B2qhf_XGg3VXgGfvBaHr3AlRkPkiGMmx','2247483942','http://mp.weixin.qq.com/s?__biz=MzcwODI2NTU0OA==&mid=2247483942&idx=1&sn=efc0f86d6a80792a78fc1dc78ba3023e&chksm=f5391105c24e9813d882b6a2d9b26a2274dba1dfed083e77505d4d9c2489a0bf48773af4261b#rd','2026-06-06 10:57:08'),(98,'哥伦比亚U19vs中国U19',NULL,'football','REVIEW','PUBLISHED',NULL,NULL,NULL,NULL,'闪击加扑点，这场复盘老唐得说几句\n\n【赛果】\n土伦杯小组赛第三轮，中国U19队1比0战胜哥伦比亚U19队。\n\n【赛前观点】\n主任方向：中国队赢球\n主任看法：让平思路 1手\n大小球：大球 1手\n推荐比分：1-0 2-1\n\n【结果验证】\n✓ 主任方向命中：中国队90分钟获胜\n✓ 主任看法命中：一球小胜，让平思路成立\n✗ 大小球未命中：全场仅1球，大球方向落空\n✓ 推荐比分命中：1比0精准抓住\n\n【比赛复盘】\n说实话，这场球阿凯赛前的判断准得让我有点没话说。主任方向和让平思路双双兑现，连1比0的比分都逮住了，这不多见。\n\n但大小球方向，老唐我得点一下。全场就刘佳乐开场第1分钟那个闪击破门，后面再无建树。中国队全场重心在守，哥伦比亚围攻无果，上半场补时点球还被门将依合散扑出来，大球直接凉透。\n\n据现有信息，哥伦比亚下半场有人被罚下，中国队多打一人更没理由压上。久尔杰维奇上半场还被罚上看台，球队能稳住阵脚把1比0守到底，纪律性值得肯定。没让比赛变成互捅局，是大球打不出的根本原因。\n\n【复盘结论】\n主任方向和让平思路过关，比分精准命中，阿凯这场判断力确实到位。大小球方面，中国队领先后的收缩策略本身就压制了进球空间，赛前对大球的支持缺乏足够依据。这类青年队生死战，一旦早早破门，大球风险会被急剧放大。\n\nPS：\n\n老周：\n阿凯这把主任和比分抓得准，老唐你大小球翻车就算了，还硬往收缩策略上圆，真行。','2026-06-06 10:54:34','laotang','REVIEW',95,NULL,'闪击加扑点，老唐复盘1-0','中国U19闪击获胜，1-0力克哥伦比亚。阿凯方向、让平及比分精准命中，大球落空。老唐复盘认为，领先后的收缩策略压制进球空间，青年队生死战及早破门风险放大。','老唐｜复盘结论｜哥伦比亚U19vs中国U19','闪击定局','OixYT_CU-d8vfrqIfwimasP4i_gHMrBz9oqXHmFQJtcSwwUp_bCjgTD7wkq8Krhf','2247483943','http://mp.weixin.qq.com/s?__biz=MzcwODI2NTU0OA==&mid=2247483943&idx=1&sn=fae5998d271fd527c2688dda7a2b99da&chksm=f5391104c24e9812c67050fd6b1e72fc57173c979a4ff1e217978eca78b7de58f207735b5e98#rd','2026-06-06 10:57:06'),(99,'EIF埃克纳斯vsMP米克力',NULL,'football','REVIEW','APPROVED',NULL,NULL,NULL,NULL,'芬甲复盘：埃克纳斯的进攻数据在客场完全失灵\n\n【赛果】\nMP米克力 1:0 EIF埃克纳斯，半场1:0，全场唯一进球出现在第27分钟。\n\n【赛前观点】\n主任方向：胜\n主任看法：让平思路 1手\n大小球：大球 1手\n推荐比分：2-1、2-0\n\n【结果验证】\n✗ 主任方向未命中：埃克纳斯未能在客场取胜。\n✗ 主任看法未命中：让平思路未实现，主队一球小胜而非客队。\n✗ 大小球未命中：全场仅1粒进球，远低于大球预期。\n✗ 推荐比分未命中：2-1和2-0均与实际赛果1-0不符。\n\n【比赛复盘】\n这场判断翻车翻得彻底。赛前看好埃克纳斯客场带回分数，依据是主队主场进攻底子还在，MP米克力客场攻击力偏弱。但实际比赛节奏完全进入了客队的战术舒适区。第27分钟MP米克力打出快速反击，中场直塞直接撕开埃克纳斯防线，前锋单刀推射远角得手。进球后MP米克力顺势回收低位，把比赛拖入北欧球队最熟悉的硬碰硬消耗战。\n\n埃克纳斯控球优势在场面上或许存在，但资料显示控球率、射门次数等详细技术统计均未获取到明确信息，无法量化其压制程度。从结果倒推，埃克纳斯客场遇到高强度逼抢容易慌乱的软肋再次暴露，进攻端缺乏破密集防守的有效手段，全场未能改写比分。\n\n【复盘结论】\n赛前判断完全偏离实际走势。三个玩法观点全错，比分参考全错。这把的错误在于高估了埃克纳斯客场攻坚能力，同时低估了MP米克力主场低位防守加反击的执行效率。客队让平思路被主队一球闷死，大球方向更是直接被1-0封死。翻过这场，接下来碰到类似主弱客强但主队刚赢过对手的题材，得把心理优势和战术克制这两项权重往上调。\n\nPS：\n\n小北：\n老周说这场让平思路稳如老狗，结果主队一球闷死，我这脸打得比北欧的冬天还冷。','2026-06-07 10:56:52','laotang','REVIEW',96,NULL,'芬甲翻车复盘：埃克纳斯进攻哑火','MP米克力1-0击败埃克纳斯，作者赛前误判客队能拿分。实际主队反击得手后转入低位消耗战，埃克纳斯客场进攻乏力。复盘认为高估客队攻坚，低估主队防反执行。','老唐｜复盘结论｜EIF埃克纳斯vsMP米克力','客场失灵',NULL,NULL,NULL,NULL),(100,'哥伦比亚U19vs中国U19',NULL,'football','REVIEW','PUBLISHED',NULL,NULL,NULL,NULL,'土伦杯复盘：1-0精准拿捏，大小球棋差一着\n\n【赛果】\n中国U19 1-0 哥伦比亚U19\n\n【赛前观点】\n主任方向：中国队赢球\n主任看法：让平思路 1手\n大小球：大球 1手\n推荐比分：1-0 2-1\n\n【结果验证】\n✓ 主任方向命中（中国队赢球）\n✓ 主任看法命中（让平思路）\n✗ 大小球未命中（大球方向）\n△ 推荐比分部分命中（1-0精准命中，2-1未命中）\n\n【比赛复盘】\n这场土伦杯生死战，中国队打出了久违的硬度。开场36秒刘佳乐就完成前场抢断，大禁区弧顶一脚远射破门，这是赛前没人敢想的剧本。中国队1-0领先后迅速转入守势，阵型收得很紧，哥伦比亚大举压上，场面一度被动。上半场补时阶段岳瑞杰犯规送点，门将依合散判断准确直接扑出，保住了领先优势。\n\n场外戏份同样不少。主帅久尔杰维奇第42分钟因不满判罚连吃两张黄牌被罚上看台，中国队等于是无帅应战。下半场哥伦比亚又有球员被红牌罚下，人数占优后中国队继续靠拼抢和协防限制对手，最终守住1-0。\n\n哥伦比亚主帅赛后点名夸了刘佳乐，这球确实打得漂亮。但中国队整场运动战也就这一次有效射门转化成了进球，阵地战进攻依旧缺乏延续性，后场出球困难的老问题没有根本改善。\n\n【复盘结论】\n阿凯这场让平思路侥幸过关，闪电进球加扑出点球，运气确实站在了咱们这边。1-0比分精准命中值得肯定，但大小球推大球1手全错，全场就一个进球，说到底还是过高估计了哥伦比亚后防重组后的混乱程度，也没算到中国队领先后的极端收缩策略。侥幸拿下的比赛，不能当成实力碾压。\n\nPS：\n\n老周：\n阿凯这场让平思路能过全靠闪电进球和扑点续命，大球推得是真敢想啊。','2026-06-07 10:58:23','laotang','REVIEW',95,NULL,'土伦杯复盘：精准1-0，大球失算','土伦杯中国U19 1-0哥伦比亚，阿凯推荐让平命中、大球失手。复盘指出闪电破门和扑点有运气成分，球队阵地进攻乏力，领先即收缩让大球落空，侥幸过关并非实力碾压。','老唐｜复盘结论｜哥伦比亚U19vs中国U19','侥幸过关','OixYT_CU-d8vfrqIfwimaufY1IQHOdWmr1Vtf7ZaHn7DKdayiqulpl0MP_nJYPeD','2247483949','http://mp.weixin.qq.com/s?__biz=MzcwODI2NTU0OA==&mid=2247483949&idx=1&sn=ebf2ed3b67322615f74820fbbbf8856b&chksm=f539110ec24e98182a2bace3b12b4fb7ca61f5e1fdf02343921488f33e18551a741c6ae13332#rd','2026-06-07 11:03:20'),(101,'肖莱特vs巴黎',NULL,'football','PREVIEW','PUBLISHED',NULL,NULL,NULL,NULL,'肖莱特这场，巴黎能不能把上一场的面子找回来？\n\n比赛时间：北京时间 06-08 01:00\n\n看了下资料，两队几天前刚打过一场，肖莱特在巴黎主场89比90偷走一胜，算是爆了个小冷。这一场回到肖莱特自己的地盘，节奏可能不太一样。\n\n肖莱特近10场8胜2负，火力不差，场均能轰86.2分。但有个细节，他们近期主场防守漏得比较多，近五场场均丢85分往上，这不像一支强队的主场表现。巴黎这边，历史交锋占优，近10次碰面赢了7次，近6次交手场均有172.5分，分差也就7分左右，说明两队其实经常咬得紧。\n\n市场给到巴黎客让2.5到3.5分的支持，这个浅盘卡得挺有意思。总分从180.5往下调，感觉机构并不看好两人继续互飙。未获取到明确伤停公告，但我个人更关注的是，巴黎刚输完，这种级别的队伍很少连着在同一个对手身上翻车，临场强度应该会提一个档次。\n\n今日看法：\n主任方向：客胜\n让分看法：客队让分思路 1手\n大小分：小分方向 1手\n分差参考：客队1到5分\n\nPS：\n\n老周：\n阿凯这场看小分的理由居然是“机构调了总分”，这理由比巴黎的防守还玄乎，老唐你信吗？','2026-06-07 13:07:12','akai','PREVIEW',NULL,NULL,'巴黎再战肖莱特，复仇稳了？','巴黎客场再战肖莱特，几天前主场1分惜败。作者认为巴黎历史交锋占优，且很少连败同一对手，本场将提升强度找回面子，推荐客胜、客队让分及小分方向，分差看1至5分。','阿凯｜今日观点｜肖莱特vs巴黎','拒绝连败','OixYT_CU-d8vfrqIfwimasgNywEjppqZSSbGCMiciLbFWZgQj4jSBlfN9BU4xwsK','2247483954','http://mp.weixin.qq.com/s?__biz=MzcwODI2NTU0OA==&mid=2247483954&idx=1&sn=3e7dd0053e96adba45de5d8086d9d7f0&chksm=f5391111c24e9807f96212e2c6fec325a8dd33e4ba4b1ff54c97da0947a9839392c551641009#rd','2026-06-07 13:11:34'),(102,'洛杉矶火花vs波特兰火焰',NULL,'football','PREVIEW','PUBLISHED',NULL,NULL,NULL,NULL,'比赛时间：北京时间 2026-06-08 07:00\n\n洛杉矶火花 vs 波特兰火焰\n\n这场球有点意思，两队伤停信息完全是一锅粥。不同消息源给出的说法互相打架，咱们只能照实引用。\n\n火焰这边，多个消息源都提到主力后卫脚踝肿胀，控卫琼斯左膝也肿了，队医已经把他列入出战成疑，上场时间很可能被压缩。这对火焰的高位压迫打击很大，资料显示一旦琼斯缺阵，他们夺回球权的成功率会从38.7%直接掉到29.1%，替补控卫的失误率还会飙升。防守端连续三场高位压迫成功率暴跌，这节奏明显不对。唯一的好消息是锋线威廉姆斯解禁复出，训练状态看起来不错。\n\n火花这边同样不省心，内线核心归期未定，轮换中锋哈里斯又拉伤腹股沟，内线轮换深度被削得很薄。不过约翰逊的护框能力在线，场均3.1次封盖能让对手篮下命中率压到41.2%，这点可以跟火焰的突破形成对参考。\n\n节奏上我看好这场打得比较紧。双方后场都有核心球员身体出问题，失误率可能往上走，攻防转换速度提不上去。火焰虽然高位压迫退步明显，但主场收缩护框的能力还在，火花客场下半场体能衰减时失误率会升到18.6%，很难拉开比分。\n\n今日看法：\n主任方向：主胜\n让分看法：洛杉矶火花-7.5 1手\n大小分：小177 1手\n分差参考：主队8到12分\n\nPS：\n\n老唐：\n阿凯，让7.5又给主胜1到5分，这盘口和分差自己先打起来了，小北算账都得懵。','2026-06-07 13:10:45','akai','PREVIEW',NULL,NULL,'火花让分偏深，小分是关键','火花主场迎战火焰，双方后场核心均有伤疑，节奏偏慢。作者看好小分格局，倾向主胜但分差倾向主队8到12分。','阿凯｜今日观点｜洛杉矶火花vs波特兰火焰','让分虚高','OixYT_CU-d8vfrqIfwimahWBl0KTF4mkaoRRf6Gl-mOEhCxc8FFgN6s5HvlO-UIR','2247483959','http://mp.weixin.qq.com/s?__biz=MzcwODI2NTU0OA==&mid=2247483959&idx=1&sn=96b86a0fbdd1e9aec376fecbf4bfe9c6&chksm=f5391114c24e98022ae02e30361fc24b36b80b6f4810cc72c7e389a5081158a769a67163ceee#rd','2026-06-07 13:11:45');
/*!40000 ALTER TABLE `article_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `article_task_102_gap_test_20260607_212710`
--

DROP TABLE IF EXISTS `article_task_102_gap_test_20260607_212710`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `article_task_102_gap_test_20260607_212710` (
  `id` bigint NOT NULL DEFAULT '0',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `match_id` bigint DEFAULT NULL,
  `sport_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文章体育类型 football/basketball',
  `article_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `chief_editor_content` longtext COLLATE utf8mb4_unicode_ci,
  `football_editor_content` longtext COLLATE utf8mb4_unicode_ci,
  `basketball_editor_content` longtext COLLATE utf8mb4_unicode_ci,
  `analyst_content` longtext COLLATE utf8mb4_unicode_ci,
  `final_content` longtext COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `author_editor` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `article_category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `related_article_id` bigint DEFAULT NULL,
  `rewrite_note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_summary` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cover_text` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cover_headline` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_draft_media_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_publish_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `article_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `publish_time` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `article_task_102_gap_test_20260607_212710`
--

LOCK TABLES `article_task_102_gap_test_20260607_212710` WRITE;
/*!40000 ALTER TABLE `article_task_102_gap_test_20260607_212710` DISABLE KEYS */;
INSERT INTO `article_task_102_gap_test_20260607_212710` VALUES (102,'洛杉矶火花vs波特兰火焰',NULL,'football','PREVIEW','PUBLISHED',NULL,NULL,NULL,NULL,'比赛时间：北京时间 2026-06-08 07:00\n\n洛杉矶火花 vs 波特兰火焰\n\n这场球有点意思，两队伤停信息完全是一锅粥。不同消息源给出的说法互相打架，咱们只能照实引用。\n\n火焰这边，多个消息源都提到主力后卫脚踝肿胀，控卫琼斯左膝也肿了，队医已经把他列入出战成疑，上场时间很可能被压缩。这对火焰的高位压迫打击很大，资料显示一旦琼斯缺阵，他们夺回球权的成功率会从38.7%直接掉到29.1%，替补控卫的失误率还会飙升。防守端连续三场高位压迫成功率暴跌，这节奏明显不对。唯一的好消息是锋线威廉姆斯解禁复出，训练状态看起来不错。\n\n火花这边同样不省心，内线核心归期未定，轮换中锋哈里斯又拉伤腹股沟，内线轮换深度被削得很薄。不过约翰逊的护框能力在线，场均3.1次封盖能让对手篮下命中率压到41.2%，这点可以跟火焰的突破形成对参考。\n\n节奏上我看好这场打得比较紧。双方后场都有核心球员身体出问题，失误率可能往上走，攻防转换速度提不上去。火焰虽然高位压迫退步明显，但主场收缩护框的能力还在，火花客场下半场体能衰减时失误率会升到18.6%，很难拉开比分。\n\n今日看法：\n主任方向：主胜\n让分看法：洛杉矶火花-7.5 1手\n大小分：小177 1手\n分差参考：主队8到12分\n\nPS：\n\n老唐：\n阿凯，让7.5又给主胜1到5分，这盘口和分差自己先打起来了，小北算账都得懵。','2026-06-07 13:10:45','akai','PREVIEW',NULL,NULL,'火花让分偏深，小分是关键','火花主场迎战火焰，双方后场核心均有伤疑，节奏偏慢。作者看好小分格局，倾向主胜但分差倾向主队8到12分。','阿凯｜今日观点｜洛杉矶火花vs波特兰火焰','让分虚高','OixYT_CU-d8vfrqIfwimahWBl0KTF4mkaoRRf6Gl-mOEhCxc8FFgN6s5HvlO-UIR','2247483959','http://mp.weixin.qq.com/s?__biz=MzcwODI2NTU0OA==&mid=2247483959&idx=1&sn=96b86a0fbdd1e9aec376fecbf4bfe9c6&chksm=f5391114c24e98022ae02e30361fc24b36b80b6f4810cc72c7e389a5081158a769a67163ceee#rd','2026-06-07 13:11:45');
/*!40000 ALTER TABLE `article_task_102_gap_test_20260607_212710` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `article_task_102_gap_test_20260607_212724`
--

DROP TABLE IF EXISTS `article_task_102_gap_test_20260607_212724`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `article_task_102_gap_test_20260607_212724` (
  `id` bigint NOT NULL DEFAULT '0',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `match_id` bigint DEFAULT NULL,
  `sport_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文章体育类型 football/basketball',
  `article_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `chief_editor_content` longtext COLLATE utf8mb4_unicode_ci,
  `football_editor_content` longtext COLLATE utf8mb4_unicode_ci,
  `basketball_editor_content` longtext COLLATE utf8mb4_unicode_ci,
  `analyst_content` longtext COLLATE utf8mb4_unicode_ci,
  `final_content` longtext COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `author_editor` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `article_category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `related_article_id` bigint DEFAULT NULL,
  `rewrite_note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_summary` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cover_text` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cover_headline` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_draft_media_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_publish_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `article_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `publish_time` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `article_task_102_gap_test_20260607_212724`
--

LOCK TABLES `article_task_102_gap_test_20260607_212724` WRITE;
/*!40000 ALTER TABLE `article_task_102_gap_test_20260607_212724` DISABLE KEYS */;
INSERT INTO `article_task_102_gap_test_20260607_212724` VALUES (102,'洛杉矶火花vs波特兰火焰',NULL,'football','PREVIEW','PUBLISHED',NULL,NULL,NULL,NULL,'比赛时间：北京时间 2026-06-08 07:00\n\n洛杉矶火花 vs 波特兰火焰\n\n这场球有点意思，两队伤停信息完全是一锅粥。不同消息源给出的说法互相打架，咱们只能照实引用。\n\n火焰这边，多个消息源都提到主力后卫脚踝肿胀，控卫琼斯左膝也肿了，队医已经把他列入出战成疑，上场时间很可能被压缩。这对火焰的高位压迫打击很大，资料显示一旦琼斯缺阵，他们夺回球权的成功率会从38.7%直接掉到29.1%，替补控卫的失误率还会飙升。防守端连续三场高位压迫成功率暴跌，这节奏明显不对。唯一的好消息是锋线威廉姆斯解禁复出，训练状态看起来不错。\n\n火花这边同样不省心，内线核心归期未定，轮换中锋哈里斯又拉伤腹股沟，内线轮换深度被削得很薄。不过约翰逊的护框能力在线，场均3.1次封盖能让对手篮下命中率压到41.2%，这点可以跟火焰的突破形成对参考。\n\n节奏上我看好这场打得比较紧。双方后场都有核心球员身体出问题，失误率可能往上走，攻防转换速度提不上去。火焰虽然高位压迫退步明显，但主场收缩护框的能力还在，火花客场下半场体能衰减时失误率会升到18.6%，很难拉开比分。\n\n今日看法：\n主任方向：主胜\n让分看法：洛杉矶火花-7.5 1手\n大小分：小177 1手\n分差参考：主队8到12分\n\nPS：\n\n老唐：\n阿凯，让7.5又给主胜1到5分，这盘口和分差自己先打起来了，小北算账都得懵。','2026-06-07 13:10:45','akai','PREVIEW',NULL,NULL,'火花让分偏深，小分是关键','火花主场迎战火焰，双方后场核心均有伤疑，节奏偏慢。作者看好小分格局，倾向主胜但分差倾向主队8到12分。','阿凯｜今日观点｜洛杉矶火花vs波特兰火焰','让分虚高','OixYT_CU-d8vfrqIfwimahWBl0KTF4mkaoRRf6Gl-mOEhCxc8FFgN6s5HvlO-UIR','2247483959','http://mp.weixin.qq.com/s?__biz=MzcwODI2NTU0OA==&mid=2247483959&idx=1&sn=96b86a0fbdd1e9aec376fecbf4bfe9c6&chksm=f5391114c24e98022ae02e30361fc24b36b80b6f4810cc72c7e389a5081158a769a67163ceee#rd','2026-06-07 13:11:45');
/*!40000 ALTER TABLE `article_task_102_gap_test_20260607_212724` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `article_task_basketball_102_test_20260607_212353`
--

DROP TABLE IF EXISTS `article_task_basketball_102_test_20260607_212353`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `article_task_basketball_102_test_20260607_212353` (
  `id` bigint NOT NULL DEFAULT '0',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `match_id` bigint DEFAULT NULL,
  `sport_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文章体育类型 football/basketball',
  `article_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `chief_editor_content` longtext COLLATE utf8mb4_unicode_ci,
  `football_editor_content` longtext COLLATE utf8mb4_unicode_ci,
  `basketball_editor_content` longtext COLLATE utf8mb4_unicode_ci,
  `analyst_content` longtext COLLATE utf8mb4_unicode_ci,
  `final_content` longtext COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `author_editor` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `article_category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `related_article_id` bigint DEFAULT NULL,
  `rewrite_note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_summary` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cover_text` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cover_headline` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_draft_media_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_publish_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `article_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `publish_time` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `article_task_basketball_102_test_20260607_212353`
--

LOCK TABLES `article_task_basketball_102_test_20260607_212353` WRITE;
/*!40000 ALTER TABLE `article_task_basketball_102_test_20260607_212353` DISABLE KEYS */;
INSERT INTO `article_task_basketball_102_test_20260607_212353` VALUES (102,'洛杉矶火花vs波特兰火焰',NULL,'football','PREVIEW','PUBLISHED',NULL,NULL,NULL,NULL,'比赛时间：北京时间 2026-06-08 07:00\n\n洛杉矶火花 vs 波特兰火焰\n\n这场球有点意思，两队伤停信息完全是一锅粥。不同消息源给出的说法互相打架，咱们只能照实引用。\n\n火焰这边，多个消息源都提到主力后卫脚踝肿胀，控卫琼斯左膝也肿了，队医已经把他列入出战成疑，上场时间很可能被压缩。这对火焰的高位压迫打击很大，资料显示一旦琼斯缺阵，他们夺回球权的成功率会从38.7%直接掉到29.1%，替补控卫的失误率还会飙升。防守端连续三场高位压迫成功率暴跌，这节奏明显不对。唯一的好消息是锋线威廉姆斯解禁复出，训练状态看起来不错。\n\n火花这边同样不省心，内线核心归期未定，轮换中锋哈里斯又拉伤腹股沟，内线轮换深度被削得很薄。不过约翰逊的护框能力在线，场均3.1次封盖能让对手篮下命中率压到41.2%，这点可以跟火焰的突破形成对参考。\n\n节奏上我看好这场打得比较紧。双方后场都有核心球员身体出问题，失误率可能往上走，攻防转换速度提不上去。火焰虽然高位压迫退步明显，但主场收缩护框的能力还在，火花客场下半场体能衰减时失误率会升到18.6%，很难拉开比分。\n\n今日看法：\n主任方向：主胜\n让分看法：Los Angeles Sparks-7.5 1手\n大小分：小177 1手\n分差参考：主队1到5分\n\nPS：\n\n老唐：\n阿凯，让7.5又给主胜1到5分，这盘口和分差自己先打起来了，小北算账都得懵。','2026-06-07 13:10:45','akai','PREVIEW',NULL,NULL,'让7.5只赢5分？火花有玄机','火花主场迎战火焰，双方后场核心均有伤疑，节奏偏慢。作者看好小分格局，倾向主胜但分差只在1-5分。','阿凯｜今日观点｜洛杉矶火花vs波特兰火焰','让分虚高','OixYT_CU-d8vfrqIfwimahWBl0KTF4mkaoRRf6Gl-mOEhCxc8FFgN6s5HvlO-UIR','2247483959','http://mp.weixin.qq.com/s?__biz=MzcwODI2NTU0OA==&mid=2247483959&idx=1&sn=96b86a0fbdd1e9aec376fecbf4bfe9c6&chksm=f5391114c24e98022ae02e30361fc24b36b80b6f4810cc72c7e389a5081158a769a67163ceee#rd','2026-06-07 13:11:45');
/*!40000 ALTER TABLE `article_task_basketball_102_test_20260607_212353` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `article_task_cover_test_backup_20260604_231945`
--

DROP TABLE IF EXISTS `article_task_cover_test_backup_20260604_231945`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `article_task_cover_test_backup_20260604_231945` (
  `id` bigint NOT NULL DEFAULT '0',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `match_id` bigint DEFAULT NULL,
  `sport_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文章体育类型 football/basketball',
  `article_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `chief_editor_content` longtext COLLATE utf8mb4_unicode_ci,
  `football_editor_content` longtext COLLATE utf8mb4_unicode_ci,
  `basketball_editor_content` longtext COLLATE utf8mb4_unicode_ci,
  `analyst_content` longtext COLLATE utf8mb4_unicode_ci,
  `final_content` longtext COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `author_editor` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `article_category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `related_article_id` bigint DEFAULT NULL,
  `rewrite_note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_summary` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cover_text` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cover_headline` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_draft_media_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_publish_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `article_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `publish_time` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `article_task_cover_test_backup_20260604_231945`
--

LOCK TABLES `article_task_cover_test_backup_20260604_231945` WRITE;
/*!40000 ALTER TABLE `article_task_cover_test_backup_20260604_231945` DISABLE KEYS */;
INSERT INTO `article_task_cover_test_backup_20260604_231945` VALUES (94,'千叶市原 VS 福冈黄蜂',NULL,'football','PREVIEW','APPROVED',NULL,NULL,NULL,NULL,'千叶市原vs福冈黄蜂：排位战里的暗礁，别光盯主胜\n\n比赛时间：北京时间 2026-06-06 13:00\n\n又是日职联排位战，千叶市原主场被捧得很高。我扫了一圈市场上的声音，主胜概率给到六成上下，和五月底两队交手前完全反了过来。\n\n但我得说个不太合群的观点：这场热得有点快。\n\n刚拿到的资料显示，福冈黄蜂近六场联赛零胜四平两负，走势确实不好看。可仔细看这几场平局，球队输球能力被夸大了，韧性还有。千叶市原这边同样是近六场一胜五负，唯一的胜场还是五月中那场3比0，剩下五场全输，防守端丢球不少。\n\n更让我犹豫的是伤停。阵容未获取到6月6日的明确名单，只能参考一周前的报告——双方后卫线都有缺阵，福冈桥本悠、千叶植田悠太和久保庭良太都无法确认是否复出。防线都不完整的情况下，比赛变数比平时更大。\n\n上一场5月30日两队刚踢成2比2，千叶市原先领先再被反超，最后绝平。这场继续涉及名次争夺，战意不会低，但双方近期进攻效率都不算高，福冈近六场只进五球，千叶的进球也集中在那场3比0里。\n\n热度过高的主胜，老唐总觉得里面藏着点东西。\n\n今日看法：\n主任方向：负\n主任看法：福冈黄蜂-0 1手\n大小球：大2 1手\n推荐比分：1-2 0-2\n\nPS：\n\n小北：\n老唐，你上一场也这么说福冈，结果他们最后10分钟送俩，我这心现在还没缓过来。','2026-06-04 14:18:35','laotang','PREVIEW',NULL,NULL,'千叶市原vs福冈黄蜂：排位战里的暗礁','日职联排位战，千叶市原主场被捧，作者提醒主胜过热藏暗礁。福冈黄蜂虽多场不胜但韧性仍在，双方防线均有缺阵，更看好客队不败。','老唐｜今日观点｜千叶市原 VS 福冈黄蜂','主胜有坑','OixYT_CU-d8vfrqIfwimanGc3UuMBu20CMpYHcJCMGthF91Quj9YaDbempQ_L5VZ','2247483895','http://mp.weixin.qq.com/s?__biz=MzcwODI2NTU0OA==&mid=2247483895&idx=1&sn=2bbbc232e4579cce19533bffc8da049d&chksm=f53912d4c24e9bc2b3ccef4719c4b6ff2a280c43957f95d41e2b76e6f045bca46c21e9753857#rd','2026-06-04 14:26:52');
/*!40000 ALTER TABLE `article_task_cover_test_backup_20260604_231945` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `article_task_layout_test_backup_20260604_230738`
--

DROP TABLE IF EXISTS `article_task_layout_test_backup_20260604_230738`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `article_task_layout_test_backup_20260604_230738` (
  `id` bigint NOT NULL DEFAULT '0',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `match_id` bigint DEFAULT NULL,
  `sport_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文章体育类型 football/basketball',
  `article_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `chief_editor_content` longtext COLLATE utf8mb4_unicode_ci,
  `football_editor_content` longtext COLLATE utf8mb4_unicode_ci,
  `basketball_editor_content` longtext COLLATE utf8mb4_unicode_ci,
  `analyst_content` longtext COLLATE utf8mb4_unicode_ci,
  `final_content` longtext COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `author_editor` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `article_category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `related_article_id` bigint DEFAULT NULL,
  `rewrite_note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_summary` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cover_text` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cover_headline` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_draft_media_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_publish_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `article_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `publish_time` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `article_task_layout_test_backup_20260604_230738`
--

LOCK TABLES `article_task_layout_test_backup_20260604_230738` WRITE;
/*!40000 ALTER TABLE `article_task_layout_test_backup_20260604_230738` DISABLE KEYS */;
INSERT INTO `article_task_layout_test_backup_20260604_230738` VALUES (94,'千叶市原 VS 福冈黄蜂',NULL,'football','PREVIEW','PUBLISHED',NULL,NULL,NULL,NULL,'千叶市原vs福冈黄蜂：排位战里的暗礁，别光盯主胜\n\n比赛时间：北京时间 2026-06-06 13:00\n\n又是日职联排位战，千叶市原主场被捧得很高。我扫了一圈市场上的声音，主胜概率给到六成上下，和五月底两队交手前完全反了过来。\n\n但我得说个不太合群的观点：这场热得有点快。\n\n刚拿到的资料显示，福冈黄蜂近六场联赛零胜四平两负，走势确实不好看。可仔细看这几场平局，球队输球能力被夸大了，韧性还有。千叶市原这边同样是近六场一胜五负，唯一的胜场还是五月中那场3比0，剩下五场全输，防守端丢球不少。\n\n更让我犹豫的是伤停。阵容未获取到6月6日的明确名单，只能参考一周前的报告——双方后卫线都有缺阵，福冈桥本悠、千叶植田悠太和久保庭良太都无法确认是否复出。防线都不完整的情况下，比赛变数比平时更大。\n\n上一场5月30日两队刚踢成2比2，千叶市原先领先再被反超，最后绝平。这场继续涉及名次争夺，战意不会低，但双方近期进攻效率都不算高，福冈近六场只进五球，千叶的进球也集中在那场3比0里。\n\n热度过高的主胜，老唐总觉得里面藏着点东西。\n\n今日看法：\n主任方向：负\n主任看法：福冈黄蜂-0 1手\n大小球：大2 1手\n推荐比分：1-2 0-2\n\nPS：\n\n小北：\n老唐，你上一场也这么说福冈，结果他们最后10分钟送俩，我这心现在还没缓过来。','2026-06-04 14:18:35','laotang','PREVIEW',NULL,NULL,'千叶市原vs福冈黄蜂：排位战里的暗礁','日职联排位战，千叶市原主场被捧，作者提醒主胜过热藏暗礁。福冈黄蜂虽多场不胜但韧性仍在，双方防线均有缺阵，更看好客队不败。','老唐｜今日观点｜千叶市原 VS 福冈黄蜂','主胜有坑','OixYT_CU-d8vfrqIfwimanGc3UuMBu20CMpYHcJCMGthF91Quj9YaDbempQ_L5VZ','2247483895','http://mp.weixin.qq.com/s?__biz=MzcwODI2NTU0OA==&mid=2247483895&idx=1&sn=2bbbc232e4579cce19533bffc8da049d&chksm=f53912d4c24e9bc2b3ccef4719c4b6ff2a280c43957f95d41e2b76e6f045bca46c21e9753857#rd','2026-06-04 14:26:52');
/*!40000 ALTER TABLE `article_task_layout_test_backup_20260604_230738` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `article_task_layout_test_backup_20260604_231038`
--

DROP TABLE IF EXISTS `article_task_layout_test_backup_20260604_231038`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `article_task_layout_test_backup_20260604_231038` (
  `id` bigint NOT NULL DEFAULT '0',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `match_id` bigint DEFAULT NULL,
  `sport_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文章体育类型 football/basketball',
  `article_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `chief_editor_content` longtext COLLATE utf8mb4_unicode_ci,
  `football_editor_content` longtext COLLATE utf8mb4_unicode_ci,
  `basketball_editor_content` longtext COLLATE utf8mb4_unicode_ci,
  `analyst_content` longtext COLLATE utf8mb4_unicode_ci,
  `final_content` longtext COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `author_editor` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `article_category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `related_article_id` bigint DEFAULT NULL,
  `rewrite_note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_summary` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cover_text` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cover_headline` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_draft_media_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_publish_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `article_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `publish_time` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `article_task_layout_test_backup_20260604_231038`
--

LOCK TABLES `article_task_layout_test_backup_20260604_231038` WRITE;
/*!40000 ALTER TABLE `article_task_layout_test_backup_20260604_231038` DISABLE KEYS */;
INSERT INTO `article_task_layout_test_backup_20260604_231038` VALUES (94,'千叶市原 VS 福冈黄蜂',NULL,'football','PREVIEW','PUBLISHED',NULL,NULL,NULL,NULL,'千叶市原vs福冈黄蜂：排位战里的暗礁，别光盯主胜\n\n比赛时间：北京时间 2026-06-06 13:00\n\n又是日职联排位战，千叶市原主场被捧得很高。我扫了一圈市场上的声音，主胜概率给到六成上下，和五月底两队交手前完全反了过来。\n\n但我得说个不太合群的观点：这场热得有点快。\n\n刚拿到的资料显示，福冈黄蜂近六场联赛零胜四平两负，走势确实不好看。可仔细看这几场平局，球队输球能力被夸大了，韧性还有。千叶市原这边同样是近六场一胜五负，唯一的胜场还是五月中那场3比0，剩下五场全输，防守端丢球不少。\n\n更让我犹豫的是伤停。阵容未获取到6月6日的明确名单，只能参考一周前的报告——双方后卫线都有缺阵，福冈桥本悠、千叶植田悠太和久保庭良太都无法确认是否复出。防线都不完整的情况下，比赛变数比平时更大。\n\n上一场5月30日两队刚踢成2比2，千叶市原先领先再被反超，最后绝平。这场继续涉及名次争夺，战意不会低，但双方近期进攻效率都不算高，福冈近六场只进五球，千叶的进球也集中在那场3比0里。\n\n热度过高的主胜，老唐总觉得里面藏着点东西。\n\n今日看法：\n主任方向：负\n主任看法：福冈黄蜂-0 1手\n大小球：大2 1手\n推荐比分：1-2 0-2\n\nPS：\n\n小北：\n老唐，你上一场也这么说福冈，结果他们最后10分钟送俩，我这心现在还没缓过来。','2026-06-04 14:18:35','laotang','PREVIEW',NULL,NULL,'千叶市原vs福冈黄蜂：排位战里的暗礁','日职联排位战，千叶市原主场被捧，作者提醒主胜过热藏暗礁。福冈黄蜂虽多场不胜但韧性仍在，双方防线均有缺阵，更看好客队不败。','老唐｜今日观点｜千叶市原 VS 福冈黄蜂','主胜有坑','OixYT_CU-d8vfrqIfwimanGc3UuMBu20CMpYHcJCMGthF91Quj9YaDbempQ_L5VZ','2247483895','http://mp.weixin.qq.com/s?__biz=MzcwODI2NTU0OA==&mid=2247483895&idx=1&sn=2bbbc232e4579cce19533bffc8da049d&chksm=f53912d4c24e9bc2b3ccef4719c4b6ff2a280c43957f95d41e2b76e6f045bca46c21e9753857#rd','2026-06-04 14:26:52');
/*!40000 ALTER TABLE `article_task_layout_test_backup_20260604_231038` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `article_task_layout_test_backup_20260604_231428`
--

DROP TABLE IF EXISTS `article_task_layout_test_backup_20260604_231428`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `article_task_layout_test_backup_20260604_231428` (
  `id` bigint NOT NULL DEFAULT '0',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `match_id` bigint DEFAULT NULL,
  `sport_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文章体育类型 football/basketball',
  `article_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `chief_editor_content` longtext COLLATE utf8mb4_unicode_ci,
  `football_editor_content` longtext COLLATE utf8mb4_unicode_ci,
  `basketball_editor_content` longtext COLLATE utf8mb4_unicode_ci,
  `analyst_content` longtext COLLATE utf8mb4_unicode_ci,
  `final_content` longtext COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `author_editor` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `article_category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `related_article_id` bigint DEFAULT NULL,
  `rewrite_note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_summary` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cover_text` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cover_headline` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_draft_media_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_publish_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `article_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `publish_time` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `article_task_layout_test_backup_20260604_231428`
--

LOCK TABLES `article_task_layout_test_backup_20260604_231428` WRITE;
/*!40000 ALTER TABLE `article_task_layout_test_backup_20260604_231428` DISABLE KEYS */;
INSERT INTO `article_task_layout_test_backup_20260604_231428` VALUES (94,'千叶市原 VS 福冈黄蜂',NULL,'football','PREVIEW','PUBLISHED',NULL,NULL,NULL,NULL,'千叶市原vs福冈黄蜂：排位战里的暗礁，别光盯主胜\n\n比赛时间：北京时间 2026-06-06 13:00\n\n又是日职联排位战，千叶市原主场被捧得很高。我扫了一圈市场上的声音，主胜概率给到六成上下，和五月底两队交手前完全反了过来。\n\n但我得说个不太合群的观点：这场热得有点快。\n\n刚拿到的资料显示，福冈黄蜂近六场联赛零胜四平两负，走势确实不好看。可仔细看这几场平局，球队输球能力被夸大了，韧性还有。千叶市原这边同样是近六场一胜五负，唯一的胜场还是五月中那场3比0，剩下五场全输，防守端丢球不少。\n\n更让我犹豫的是伤停。阵容未获取到6月6日的明确名单，只能参考一周前的报告——双方后卫线都有缺阵，福冈桥本悠、千叶植田悠太和久保庭良太都无法确认是否复出。防线都不完整的情况下，比赛变数比平时更大。\n\n上一场5月30日两队刚踢成2比2，千叶市原先领先再被反超，最后绝平。这场继续涉及名次争夺，战意不会低，但双方近期进攻效率都不算高，福冈近六场只进五球，千叶的进球也集中在那场3比0里。\n\n热度过高的主胜，老唐总觉得里面藏着点东西。\n\n今日看法：\n主任方向：负\n主任看法：福冈黄蜂-0 1手\n大小球：大2 1手\n推荐比分：1-2 0-2\n\nPS：\n\n小北：\n老唐，你上一场也这么说福冈，结果他们最后10分钟送俩，我这心现在还没缓过来。','2026-06-04 14:18:35','laotang','PREVIEW',NULL,NULL,'千叶市原vs福冈黄蜂：排位战里的暗礁','日职联排位战，千叶市原主场被捧，作者提醒主胜过热藏暗礁。福冈黄蜂虽多场不胜但韧性仍在，双方防线均有缺阵，更看好客队不败。','老唐｜今日观点｜千叶市原 VS 福冈黄蜂','主胜有坑','OixYT_CU-d8vfrqIfwimanGc3UuMBu20CMpYHcJCMGthF91Quj9YaDbempQ_L5VZ','2247483895','http://mp.weixin.qq.com/s?__biz=MzcwODI2NTU0OA==&mid=2247483895&idx=1&sn=2bbbc232e4579cce19533bffc8da049d&chksm=f53912d4c24e9bc2b3ccef4719c4b6ff2a280c43957f95d41e2b76e6f045bca46c21e9753857#rd','2026-06-04 14:26:52');
/*!40000 ALTER TABLE `article_task_layout_test_backup_20260604_231428` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `article_task_layout_test_backup_20260604_231713`
--

DROP TABLE IF EXISTS `article_task_layout_test_backup_20260604_231713`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `article_task_layout_test_backup_20260604_231713` (
  `id` bigint NOT NULL DEFAULT '0',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `match_id` bigint DEFAULT NULL,
  `sport_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文章体育类型 football/basketball',
  `article_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `chief_editor_content` longtext COLLATE utf8mb4_unicode_ci,
  `football_editor_content` longtext COLLATE utf8mb4_unicode_ci,
  `basketball_editor_content` longtext COLLATE utf8mb4_unicode_ci,
  `analyst_content` longtext COLLATE utf8mb4_unicode_ci,
  `final_content` longtext COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `author_editor` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `article_category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `related_article_id` bigint DEFAULT NULL,
  `rewrite_note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_summary` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cover_text` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cover_headline` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_draft_media_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_publish_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `article_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `publish_time` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `article_task_layout_test_backup_20260604_231713`
--

LOCK TABLES `article_task_layout_test_backup_20260604_231713` WRITE;
/*!40000 ALTER TABLE `article_task_layout_test_backup_20260604_231713` DISABLE KEYS */;
INSERT INTO `article_task_layout_test_backup_20260604_231713` VALUES (94,'千叶市原 VS 福冈黄蜂',NULL,'football','PREVIEW','APPROVED',NULL,NULL,NULL,NULL,'千叶市原vs福冈黄蜂：排位战里的暗礁，别光盯主胜\n\n比赛时间：北京时间 2026-06-06 13:00\n\n又是日职联排位战，千叶市原主场被捧得很高。我扫了一圈市场上的声音，主胜概率给到六成上下，和五月底两队交手前完全反了过来。\n\n但我得说个不太合群的观点：这场热得有点快。\n\n刚拿到的资料显示，福冈黄蜂近六场联赛零胜四平两负，走势确实不好看。可仔细看这几场平局，球队输球能力被夸大了，韧性还有。千叶市原这边同样是近六场一胜五负，唯一的胜场还是五月中那场3比0，剩下五场全输，防守端丢球不少。\n\n更让我犹豫的是伤停。阵容未获取到6月6日的明确名单，只能参考一周前的报告——双方后卫线都有缺阵，福冈桥本悠、千叶植田悠太和久保庭良太都无法确认是否复出。防线都不完整的情况下，比赛变数比平时更大。\n\n上一场5月30日两队刚踢成2比2，千叶市原先领先再被反超，最后绝平。这场继续涉及名次争夺，战意不会低，但双方近期进攻效率都不算高，福冈近六场只进五球，千叶的进球也集中在那场3比0里。\n\n热度过高的主胜，老唐总觉得里面藏着点东西。\n\n今日看法：\n主任方向：负\n主任看法：福冈黄蜂-0 1手\n大小球：大2 1手\n推荐比分：1-2 0-2\n\nPS：\n\n小北：\n老唐，你上一场也这么说福冈，结果他们最后10分钟送俩，我这心现在还没缓过来。','2026-06-04 14:18:35','laotang','PREVIEW',NULL,NULL,'千叶市原vs福冈黄蜂：排位战里的暗礁','日职联排位战，千叶市原主场被捧，作者提醒主胜过热藏暗礁。福冈黄蜂虽多场不胜但韧性仍在，双方防线均有缺阵，更看好客队不败。','老唐｜今日观点｜千叶市原 VS 福冈黄蜂','主胜有坑','OixYT_CU-d8vfrqIfwimanGc3UuMBu20CMpYHcJCMGthF91Quj9YaDbempQ_L5VZ','2247483895','http://mp.weixin.qq.com/s?__biz=MzcwODI2NTU0OA==&mid=2247483895&idx=1&sn=2bbbc232e4579cce19533bffc8da049d&chksm=f53912d4c24e9bc2b3ccef4719c4b6ff2a280c43957f95d41e2b76e6f045bca46c21e9753857#rd','2026-06-04 14:26:52');
/*!40000 ALTER TABLE `article_task_layout_test_backup_20260604_231713` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `editor_ps`
--

DROP TABLE IF EXISTS `editor_ps`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `editor_ps` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `author_editor` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ps_editor` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `content` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `editor_ps`
--

LOCK TABLES `editor_ps` WRITE;
/*!40000 ALTER TABLE `editor_ps` DISABLE KEYS */;
INSERT INTO `editor_ps` VALUES (1,'akai','laozhou','阿凯这人一说“硬实力碾压”，我一般都会先把音量调小一点。'),(2,'akai','laotang','他这篇又开始上头了，不过篮球我不插嘴，等结果。'),(3,'akai','xiaobei','我只提醒一句，湖人客场数据没他说得那么轻松。'),(4,'laotang','akai','老唐又开始防冷了，他一年四季都在担心冷门。'),(5,'laotang','laozhou','老唐最大的优点是谨慎，最大的缺点也是谨慎。'),(6,'laotang','xiaobei','数据上确实有风险，但他可能想得有点太多。'),(7,'xiaobei','akai','小北这篇太理性了，看完我都不好意思上头。'),(8,'xiaobei','laozhou','数据没问题，但比赛有时候不是表格里踢出来的。'),(9,'xiaobei','laotang','小北说得细，不过临场变化还是要留一手。'),(10,'laozhou','akai','老周一开口，编辑部年轻人就开始装忙。'),(11,'laozhou','laotang','老周这次说得稳，但我还是想防一手冷。'),(12,'laozhou','xiaobei','老周看人，小北看数，这次两边可以结合着看。');
/*!40000 ALTER TABLE `editor_ps` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `match_live`
--

DROP TABLE IF EXISTS `match_live`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `match_live` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sport_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `league_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `home_team` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `away_team` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `match_time` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `live_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `qrcode_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `keywords` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `wechat_media_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `stream_key` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '直播播放key',
  `stream_url` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '临时直播源地址',
  `stream_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'auto' COMMENT '直播源类型:auto/flv/hls',
  `show_in_wechat` tinyint DEFAULT '1' COMMENT '是否在公众号最近直播展示',
  `stream_updated_at` timestamp NULL DEFAULT NULL COMMENT '直播源更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `match_live`
--

LOCK TABLES `match_live` WRITE;
/*!40000 ALTER TABLE `match_live` DISABLE KEYS */;
INSERT INTO `match_live` VALUES (39,NULL,'国际友谊赛','列支敦士登','塞浦路斯','21:00','AVAILABLE','https://api.5q.lol/upload/live_qr/live_39.png','国际友谊赛 ，列支敦士登，塞浦路斯','2026-06-07 13:04:06','gbWB_Pi55cEdl8yH5q6wB-ApYdyleC9wU2VaAkYp4kFfS4D5gvSWZc2ETyaysCUS','live_39','http://hdl.steamhd.shop/aoEAtc?g=-1002147602115&t=1780837409&sign=50811c599d273434','auto',1,'2026-06-07 13:04:06'),(40,NULL,'土伦杯','民主刚果U23','哥伦比亚U19','21:00','AVAILABLE','https://api.5q.lol/upload/live_qr/live_40.png','哥伦比亚U19 民主刚果U23 土伦杯','2026-06-07 13:05:02','gbWB_Pi55cEdl8yH5q6wB9OIPotzg6VNo1JXEJqztwqzGBY2GFktWjMTJeKCx9pC','live_40','http://hdl.steamhd.shop/bGLHX0?g=-1002147602115&t=1780837472&sign=f30519f6da2164e5','auto',1,'2026-06-07 13:05:03'),(41,NULL,'葡篮超','本菲卡','波尔图','23:00','AVAILABLE','https://api.5q.lol/upload/live_qr/live_41.png','葡篮超，本菲卡，波尔图，','2026-06-07 14:31:00','gbWB_Pi55cEdl8yH5q6wB9Xu-wrko8yCmrLkh3cvKK5Fi_wJQBrMf7wObA0l6DFs','live_41','https://video10.letaocm.top/live/sd-2-3922077.flv','auto',1,'2026-06-07 14:31:00'),(42,NULL,'意篮甲A2','维罗纳','里米尼','23:00','AVAILABLE','https://api.5q.lol/upload/live_qr/live_42.png','里米尼，意篮甲A2，维罗纳','2026-06-07 14:31:38','gbWB_Pi55cEdl8yH5q6wB6M-rKlNvRhc5qo3rlFlaw3Pp5b0pUwIPlEOwjbbkcpE','live_42','https://video10.letaocm.top/live/sd-2-3921983.flv','auto',1,'2026-06-07 14:31:38'),(43,NULL,'肯尼亚超','布拉德斯','惊恐者','22:00','AVAILABLE','https://api.5q.lol/upload/live_qr/live_43.png','惊恐者，布拉德斯，肯尼亚超','2026-06-07 14:32:44','gbWB_Pi55cEdl8yH5q6wB1jWAU-X7GqPbhHfJm3E_4qKlGNv0CrabzANu5Q-0azC','live_43','https://video10.letaocm.top/live/sd-2-3922310.flv','auto',1,'2026-06-07 14:32:45');
/*!40000 ALTER TABLE `match_live` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-07 15:44:56
