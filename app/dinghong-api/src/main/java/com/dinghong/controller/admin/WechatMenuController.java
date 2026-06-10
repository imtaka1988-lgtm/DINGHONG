package com.dinghong.controller.admin;

import com.dinghong.service.wechat.WechatMenuService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/wechat/menu")
public class WechatMenuController {

    private final WechatMenuService wechatMenuService;

    public WechatMenuController(WechatMenuService wechatMenuService) {
        this.wechatMenuService = wechatMenuService;
    }

    @PostMapping("/create")
    public String create() {
        return wechatMenuService.createMenu();
    }

    @GetMapping("/current")
    public String current() {
        return wechatMenuService.getMenu();
    }

    @PostMapping("/delete")
    public String delete() {
        return wechatMenuService.deleteMenu();
    }
}
