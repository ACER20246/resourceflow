package com.github.acer20246.resourceflow.controller;

import com.github.acer20246.resourceflow.common.result.PageResult;
import com.github.acer20246.resourceflow.dto.ResourceDTO;
import com.github.acer20246.resourceflow.dto.ResourcePageQueryDTO;
import com.github.acer20246.resourceflow.service.ResourceService;
import com.github.acer20246.resourceflow.vo.ResourceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@Tag(name = "资源接口", description = "资源相关的操作")
@RequiredArgsConstructor
public class ResourceController {
    private final ResourceService resourceService;

    /**
     * 获取资源列表
     */
    @GetMapping("/resources")
    @Operation(summary = "获取资源列表", description = "获取资源列表接口")
    public PageResult <ResourceVO> getResource(@ParameterObject ResourcePageQueryDTO queryDTO) {
        return resourceService.listResources(queryDTO);
    }
    @PostMapping("/admin/resources")
    @Operation(summary = "新增资源列表", description = "新增资源列表接口")
    public void addResource(@RequestBody ResourceDTO resourceDTO) {
        log.info("新增资源：{}", resourceDTO);
        resourceService.addResource(resourceDTO);
    }
    @PatchMapping("/admin/resources/{resourceId}")
    @Operation(summary = "修改资源列表", description = "修改资源列表接口")
    public void updateResource(@PathVariable Long resourceId, @RequestBody ResourceDTO resourceDTO) {
        log.info("修改资源：{}", resourceDTO);
        resourceService.updateResource(resourceId, resourceDTO);
    }
    @DeleteMapping("/admin/resources/{resourceId}")
    @Operation(summary = "删除资源列表", description = "删除资源列表接口")
    public void deleteResource(@PathVariable Long resourceId) {
        log.info("删除资源：{}", resourceId);
        resourceService.deleteResource(resourceId);
    }
    @GetMapping("/resources/{resourceId}")
    @Operation(summary = "查询资源详情")
    public ResourceVO getResource(@PathVariable Long resourceId) {
        return resourceService.getResource(resourceId);
    }
}
