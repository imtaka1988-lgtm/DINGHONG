package com.dinghong.service;

import org.springframework.stereotype.Service;

@Service
public class MatchService {

    public String reply(String content) {
        if (content == null) {
            return defaultReply();
        }

        if (content.contains("湖人")) {
            return "🏀 今日赛事\n\n湖人 VS 勇士\n比赛时间：20:00\n\n直播二维码稍后开放。\n回复【今日比赛】查看全部赛事。";
        }

        if (content.contains("勇士")) {
            return "🏀 今日赛事\n\n勇士 VS 湖人\n比赛时间：20:00\n\n直播二维码稍后开放。\n回复【今日比赛】查看全部赛事。";
        }

        if (content.contains("今日比赛")) {
            return "今日赛事列表：\n\n1. 湖人 VS 勇士 20:00\n2. 曼城 VS 阿森纳 22:00\n\n发送球队名可查看对应赛事。";
        }

        if (content.contains("篮球直播")) {
            return "🏀 今日篮球直播：\n\n湖人 VS 勇士\n比赛时间：20:00";
        }

        if (content.contains("足球直播")) {
            return "⚽ 今日足球直播：\n\n曼城 VS 阿森纳\n比赛时间：22:00";
        }

        if (content.contains("曼城") || content.contains("阿森纳")) {
            return "⚽ 今日赛事\n\n曼城 VS 阿森纳\n比赛时间：22:00\n\n由于版权及转播限制，本平台暂未获得该场赛事直播授权。\n回复【今日比赛】查看其他赛事。";
        }

        return defaultReply();
    }

    private String defaultReply() {
        return "欢迎来到顶红体育。\n\n你可以回复：\n【今日比赛】\n【篮球直播】\n【足球直播】\n或直接发送球队名，例如：湖人、曼城。";
    }
}
