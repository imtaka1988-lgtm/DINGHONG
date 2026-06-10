package com.dinghong.controller.editor;

import com.dinghong.service.wechat.WechatDraftService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/editor")
public class WechatPublishController {

    private final WechatDraftService wechatDraftService;

    public WechatPublishController(WechatDraftService wechatDraftService) {
        this.wechatDraftService = wechatDraftService;
    }

    @PostMapping("/wechat-publish/{id}")
    public String publishToWechat(@PathVariable Long id) {
        return wechatDraftService.publishDraft(id);
    }

    @PostMapping("/wechat-publish-status/{id}")
    public String queryWechatPublishStatus(@PathVariable Long id) {
        return wechatDraftService.queryPublishStatus(id);
    }
}
