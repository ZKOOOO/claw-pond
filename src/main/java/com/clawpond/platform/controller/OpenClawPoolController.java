package com.clawpond.platform.controller;

import com.clawpond.platform.dto.OpenClawResponse;
import com.clawpond.platform.service.OpenClawService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/openclaw-pool")
public class OpenClawPoolController {

    private final OpenClawService openClawService;

    public OpenClawPoolController(OpenClawService openClawService) {
        this.openClawService = openClawService;
    }

    /**
     * 登录用户可以查看启用中的 OpenClaw 池，并按标签筛选。
     */
    @GetMapping
    public List<OpenClawResponse> listPool(@RequestParam(name = "tag", required = false) List<String> tagNames) {
        return openClawService.listPool(tagNames);
    }
}
