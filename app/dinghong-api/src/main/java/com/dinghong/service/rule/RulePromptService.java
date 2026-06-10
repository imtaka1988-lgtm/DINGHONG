package com.dinghong.service.rule;

import org.springframework.stereotype.Service;

@Service
public class RulePromptService {

    public String globalWritingRule() {
        return "【顶红体育统一硬规则】\n"
                + "你是顶红体育编辑部成员。文章定位为赛事分析、赛前观察、赛后复盘和玩法参考。\n"
                + "文章可以直接使用主任方向、主任看法、大小球、大小分、让胜、让平、让负、让分看法、推荐比分、分差参考、1手、2手等表达，禁止强行改成委婉说法。\n"
                + "文章默认短文模式，全文250到400字。禁止写成长篇新闻稿，禁止堆砌资料。\n"
                + "必须优先参考系统提供的联网赛事资料。如果资料存在，必须结合资料分析。禁止忽略资料凭空创作。\n"
                + "禁止虚构球员伤停、预计首发、历史交锋、近期战绩、积分排名、赛程信息、媒体消息、记者爆料、作者经历、编辑经历。\n"
                + "如果资料未明确提及，必须写未获取到明确资料。\n"
                + "允许直接给出玩法参考，但不能承诺结果。禁止绝对化表达：一定、肯定、必然、毫无悬念、百分百、铁定、绝对。\n"
                + forbiddenWordRule() + "\n"
                + "\n【最终玩法表达纠错规则】\n禁止输出主任主任方向。\n篮球主任方向必须写主胜或客胜，禁止写主队方向、客队方向。\n篮球让分看法如果没有明确盘口数字，写主队让分思路或客队受让思路，禁止写主队方向、客队方向。\n篮球大小分如果没有明确总分数字，写大分或小分，禁止写大分方向、小分方向。\n篮球分差参考必须比“双方分差不大”更具体，可写主队1到5分、客队5分以内、分差预计在个位数。\n足球主任方向必须写胜、平、负、让胜、让平、让负之一或组合倾向，禁止写主队方向、客队方向。\n足球大小球没有明确数字时，写大球或小球；有明确数字时写大2、小2、大2.5、小2.5。\n\n"

                + "\n【盘口数字真实性最高规则】\n让分、大小分、让球、大小球里的具体数字，只有在用户输入的比赛信息、后台字段或联网资料中明确出现时，才可以照写。\n如果资料里没有明确盘口数字，禁止自行编造 -1、-4.5、168.5、2.5、3.5 等数字。\n没有明确数字时，必须写方向型表达：\n足球写：主任看法：让胜思路 1手、让平思路 1手、让负思路 1手；大小球：大球方向 1手、小球方向 1手。\n篮球写：让分看法：主队方向 1手、客队方向 1手；大小分：大分方向 1手、小分方向 1手。\n推荐比分和分差参考可以基于分析给出，但必须是观点参考，不是结果承诺。\n\n"

                + "禁止输出Markdown格式，禁止井号、星号、反引号、代码块、表格。直接输出纯文本公众号正文。\n";
    }

    public String previewArticleRule(String matchInfo) {
        if (isBasketball(matchInfo)) {
            return basketballPreviewRule();
        }
        return footballPreviewRule();
    }

    private String footballPreviewRule() {
        return "【足球赛前玩法格式，最高优先级】\n"
                + "赛前文章结尾必须出现“今日看法：”。\n"
                + "今日看法必须使用下面格式，禁止写成“方向、进球数/总分、比分参考”这种旧格式。\n"
                + "必须直接写玩法，不要委婉表达。\n"
                + "固定格式：\n"
                + "今日看法：\n"
                + "主任方向：胜/平/负/让胜/让平/让负，选择一项或给出明确倾向\n"
                + "主任看法：例如让平（拜仁-1球 平） 1手\n"
                + "大小球：例如小2、大2.5、小3 1手或2手\n"
                + "推荐比分：例如0-0 0-1，最多两个\n"
                + "如果资料不足，也必须按这个格式写，但要降低手数，例如1手。\n"
                + "禁止把让胜、让平、让负改写成主队更值得关注、客队具备守住局面这种委婉话。\n"
                + "禁止把大小球改写成进球数偏多、进球数偏少。\n";
    }

    private String basketballPreviewRule() {
        return "【篮球赛前玩法格式，最高优先级】\n"
                + "赛前文章结尾必须出现“今日看法：”。\n"
                + "篮球没有“不败”这种足球说法，禁止写中国男篮不败、主队不败、客队不败。\n"
                + "今日看法必须使用下面格式，禁止写成“方向、进球数/总分、比分参考”这种旧格式。\n"
                + "必须直接写玩法，不要委婉表达。\n"
                + "固定格式：\n"
                + "今日看法：\n"
                + "主任方向：主胜/客胜，选择一项\n"
                + "让分看法：例如主队-4.5 1手，或客队+4.5 1手\n"
                + "大小分：例如小168.5 2手，或大172.5 1手\n"
                + "分差参考：例如主队1到5分、客队5分以内、双方分差不大\n"
                + "如果资料没有明确盘口数字，可以写主胜、客胜、主队让分思路、客队受让思路、大分、小分，但标签必须是主任方向、让分看法、大小分、分差参考。\n"
                + "禁止把大小分改写成总分节奏偏高、总分节奏偏低。\n"
                + "禁止把让分看法改写成普通分析话。\n";
    }

    public String reviewArticleRule(String matchInfo) {
        return "【赛后复盘硬规则】\n"
                + "赛后复盘第一任务是验证赛前玩法观点是否正确。\n"
                + "如果存在关联预测文章，必须读取赛前观点，然后逐项验证：主任方向、主任看法、大小球或大小分、推荐比分或分差参考、手数。\n"
                + "赛后复盘必须包含：【赛果】【赛前观点】【结果验证】【比赛复盘】【复盘结论】。\n"
                + "结果验证必须使用：✓ 命中，✗ 未命中，△ 部分符合。\n"
                + "示例：✓ 主任方向命中；✗ 推荐比分未命中；△ 大小球判断部分符合。\n"
                + "禁止写早就知道、果然如此、毫无意外、早已看透。\n"
                + reviewRule(matchInfo);
    }

    public String psRule() {
        return "【PS硬规则】\n"
                + "每篇文章只允许一个PS。PS长度20到50字。PS可以调侃、吐槽、反驳或补充观点，但不要重复写完整玩法推荐。禁止必中、包红、稳赢、重注、梭哈等词。\n";
    }

    public String titleRule() {
        return "【标题硬规则】\n"
                + "标题20字以内，适合微信公众号。可以体现主任方向、大小球、大小分、比分参考、复盘验证结果。禁止必中、包红、稳赢、百分百。只返回标题，不要解释。\n";
    }

    public String summaryRule() {
        return "【摘要硬规则】\n"
                + "摘要80字以内，可以提到主任方向、主任看法、大小球、大小分、推荐比分、分差参考，但不能承诺结果。只返回摘要，不要解释。\n";
    }

    public String coverHeadlineRule() {
        return "【封面文案硬规则】\n"
                + "封面核心观点4到8个字，不要解释。可以写方向看法、让平可看、小分思路、大小偏谨慎、方向命中、比分偏差。禁止必中、包红、稳赢、百分百。\n";
    }

    public String previewResearchRule() {
        return "【赛前联网资料整理规则】\n"
                + "只整理搜索资料中明确出现的信息，不做无依据预测，不编造。输出比赛时间与背景、双方状态、阵容伤停、历史交锋、关键因素、资料不足提醒、玩法参考可用信息。没有明确资料写未获取到明确资料。纯文本，禁止Markdown。\n";
    }

    public String reviewResearchRule() {
        return "【赛后联网资料整理规则】\n"
                + "必须判断搜索资料中是否存在明确赛果。只整理明确出现的信息，不编造比分、进球、红黄牌、技术统计、采访。输出最终赛果、关键事件、技术统计、赛前玩法验证需要的信息、资料不足提醒。纯文本，禁止Markdown。\n";
    }

    public String forbiddenWordRule() {
        return "【禁止词库】禁止出现：稳赢、稳胆、稳过、稳如狗、稳吃、必中、包红、红单、收米、梭哈、重注、满仓、上车、跟单、冲、必胜、无脑选、闭眼选、立帖为证、锁死、送钱、提款机、赌赢、稳赚、内部消息、百分百、铁定、绝对。注意：主任方向、主任看法、大小球、大小分、大球、小球、大分、小分、让胜、让平、让负、让分看法、推荐比分、分差参考、1手、2手不是禁止词，必须允许正常使用。";
    }

    public String sanitizeForbiddenWords(String text) {
        if (text == null) return "";
        return text.replace("稳如狗", "偏稳")
                .replace("稳赢", "倾向")
                .replace("稳胆", "重点看法")
                .replace("稳过", "倾向")
                .replace("稳吃", "倾向")
                .replace("必中", "重点关注")
                .replace("包红", "重点关注")
                .replace("红单", "判断")
                .replace("收米", "")
                .replace("梭哈", "")
                .replace("重注", "1手")
                .replace("满仓", "1手")
                .replace("上车", "参考")
                .replace("跟单", "参考")
                .replace("冲", "参考")
                .replace("必胜", "倾向")
                .replace("无脑选", "不宜简单看待")
                .replace("闭眼选", "不宜简单看待")
                .replace("立帖为证", "")
                .replace("锁死", "暂时倾向")
                .replace("送钱", "")
                .replace("提款机", "")
                .replace("赌赢", "判断正确")
                .replace("稳赚", "倾向")
                .replace("内部消息", "资料显示")
                .replace("百分百", "相对")
                .replace("铁定", "倾向")
                .replace("绝对", "相对");
    }

    public String reviewRule(String matchInfo) {
        if (isBasketball(matchInfo)) {
            return basketballRule();
        }
        return footballRule();
    }

    private boolean isBasketball(String matchInfo) {
        if (matchInfo == null) return false;
        String t = matchInfo.toLowerCase();
        return t.contains("nba")
                || t.contains("cba")
                || t.contains("wnba")
                || t.contains("basketball")
                || t.contains("篮球")
                || t.contains("男篮")
                || t.contains("女篮")
                || t.contains("湖人")
                || t.contains("勇士")
                || t.contains("凯尔特人")
                || t.contains("独行侠")
                || t.contains("掘金")
                || t.contains("快船")
                || t.contains("太阳")
                || t.contains("雄鹿")
                || t.contains("热火")
                || t.contains("尼克斯")
                || t.contains("森林狼")
                || t.contains("雷霆")
                || t.contains("76人")
                || t.contains("国王")
                || t.contains("灰熊")
                || t.contains("火箭")
                || t.contains("公牛")
                || t.contains("骑士")
                || t.contains("猛龙")

                || t.contains("wnba")
                || t.contains("fever")
                || t.contains("dream")
                || t.contains("lynx")
                || t.contains("valkyries")
                || t.contains("sky")
                || t.contains("connecticut sun")
                || t.contains("sparks")
                || t.contains("wings")
                || t.contains("portland fire")
                || t.contains("mercury")
                || t.contains("indiana")
                || t.contains("atlanta")
                || t.contains("minnesota")
                || t.contains("golden state valkyries")
                || t.contains("chicago sky")
                || t.contains("los angeles sparks")
                || t.contains("dallas wings")
                || t.contains("phoenix mercury")
                || t.contains("印第安纳狂热")
                || t.contains("狂热")
                || t.contains("亚特兰大梦想")
                || t.contains("梦想")
                || t.contains("明尼苏达山猫")
                || t.contains("山猫")
                || t.contains("金州女武神")
                || t.contains("女武神")
                || t.contains("芝加哥天空")
                || t.contains("天空")
                || t.contains("康涅狄格太阳")
                || t.contains("阳光")
                || t.contains("洛杉矶火花")
                || t.contains("火花")
                || t.contains("达拉斯飞翼")
                || t.contains("飞翼")
                || t.contains("波特兰火焰")
                || t.contains("火焰")
                || t.contains("菲尼克斯水星")
                || t.contains("水星");
    }

    private String footballRule() {
        return "【足球赛后结算规则】\n"
                + "1. 足球主任方向、主任看法、胜平负、让胜、让平、让负、大小球、推荐比分，默认按90分钟加伤停补时验证。\n"
                + "2. 不计算加时赛、点球大战，除非赛前明确写了晋级、夺冠、冠军归属。\n"
                + "3. 大小球只统计90分钟总进球。\n"
                + "4. 推荐比分按90分钟比分验证。\n"
                + "5. 复盘必须先验证玩法观点，再写比赛过程。\n\n";
    }

    private String basketballRule() {
        return "【篮球赛后结算规则】\n"
                + "1. 篮球主任方向、让分看法、大小分、分差参考，默认按全场最终比分验证，通常包含加时赛。\n"
                + "2. 只有明确写常规时间、不含加时、四节内，才不计算加时赛。\n"
                + "3. 大小分按最终总分判断。\n"
                + "4. 让分看法按最终分差判断。\n"
                + "5. 篮球禁止按足球不败逻辑复盘。\n\n";
    }
}
