package com.dinghong.controller.editor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/editor")
public class ArticlePublishRealController {

    @PostMapping("/publish/{id}")
    public String publish(@PathVariable Long id) {
        return "PUBLISH_REQUEST_" + id;
    }
}
