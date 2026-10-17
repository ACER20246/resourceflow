package com.github.acer20246.resourceflow.controller;

import com.github.acer20246.resourceflow.dto.CategoryDTO;
import com.github.acer20246.resourceflow.entity.Category;
import com.github.acer20246.resourceflow.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@Tag(name = "分类管理", description = "分类相关的操作")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "获取分类列表", description = "获取分类列表接口")
    @GetMapping("/categories")
    public List<Category> listCategories() {
        return categoryService.listCategories();
    }

    @PostMapping("/admin/categories")
    @Operation(summary = "新增分类", description = "新增分类接口")
    public void addCategory(@RequestBody CategoryDTO categoryDTO) {
        log.info("新增分类：{}", categoryDTO);
        categoryService.save(categoryDTO);
    }

    @PutMapping("/admin/categories/{categoryId}")
    @Operation(summary = "修改分类", description = "修改分类接口")
    public void updateCategory(@PathVariable Long categoryId, @RequestBody CategoryDTO categoryDTO) {
        log.info("修改分类：{}，数据：{}", categoryId, categoryDTO);
        // 实现修改分类的逻辑
        categoryService.update(categoryId, categoryDTO);
    }

    @DeleteMapping("/admin/categories/{categoryId}")
    @Operation(summary = "删除分类", description = "删除分类接口")
    public void deleteCategory(@PathVariable Long categoryId) {
        // 实现删除分类的逻辑
        categoryService.delete(categoryId);
    }

    @PatchMapping("/admin/categories/{categoryId}/status")
    @Operation(summary = "修改分类状态", description = "修改分类状态接口")
    public void changeCategoryStatus(@PathVariable Long categoryId, @RequestParam Integer status) {
        log.info("修改分类状态：{}，状态：{}", categoryId, status);
        // 实现修改分类状态的逻辑
        categoryService.changeStatus(categoryId, status);
    }
}
