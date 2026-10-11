package com.ylh.picturego.controller;

import com.ylh.picturego.common.Result;
import com.ylh.picturego.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.Mapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class HealthController {
    /**
     * 健康检查
     * @return
     */
    @GetMapping("/health")
    public Result<String> healthCheck() {
        return ResultUtils.success("healthy");
    }
}
