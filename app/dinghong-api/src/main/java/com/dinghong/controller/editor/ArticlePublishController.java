package com.dinghong.controller.editor;

import com.dinghong.service.wechat.WechatDraftService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/editor")
public class ArticlePublishController {

    private final WechatDraftService wechatDraftService;

    public ArticlePublishController(WechatDraftService wechatDraftService) {
        this.wechatDraftService = wechatDraftService;
    }

    @PostMapping("/publish-draft/{id}")
    public String publishDraft(@PathVariable Long id) {
        return wechatDraftService.createDraft(id);
    }
}
