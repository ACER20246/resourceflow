package com.github.acer20246.resourceflow.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class CategoryController {

    @RequestMapping("/api/category")
    public void getCategory() {
        log.info("获取分类信息");
    }
}
