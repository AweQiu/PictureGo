package com.ylh.picturego.controller;

import com.ylh.picturego.common.Result;
import com.ylh.picturego.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {
    /**
     * 健康检查
     * @return
     */
    @GetMapping("/alive")
    public Result<String> isAlive() {
        return ResultUtils.success("hello");
    }
}
