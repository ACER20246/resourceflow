package com.github.acer20246.resourceflow.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.acer20246.resourceflow.dto.CategoryDTO;
import com.github.acer20246.resourceflow.entity.Category;
import com.github.acer20246.resourceflow.entity.Resource;
import com.github.acer20246.resourceflow.mapper.CategoryMapper;
import com.github.acer20246.resourceflow.mapper.ResourceMapper;
import com.github.acer20246.resourceflow.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {
    private final CategoryMapper categoryMapper;
    private final ResourceMapper resourceMapper;

    @Override
    public List<Category> listCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getCategoryId));
    }

    @Override
    public void save(CategoryDTO categoryDTO) {

        Long duplicateCount = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getName, categoryDTO.getName()));
        if (duplicateCount > 0) {
            throw new RuntimeException("分类名称已存在");
        }
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        category.setStatus("ENABLED");
        categoryMapper.insert(category);
    }

    @Override
    public void update(Long categoryId, CategoryDTO categoryDTO) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        BeanUtils.copyProperties(categoryDTO, category);
        category.setUpdatedAt(LocalDateTime.now());
        categoryMapper.updateById(category);
    }

    @Override
    public void changeStatus(Long categoryId, Integer status) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        category.setStatus(status == 1 ? "ENABLED" : "DISABLED");
        category.setUpdatedAt(LocalDateTime.now());
        categoryMapper.updateById(category);
    }

    @Override
    public void delete(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        List<Resource> resources = resourceMapper.selectList(new LambdaQueryWrapper<Resource>()
                .eq(Resource::getCategoryId, categoryId));
        if (!resources.isEmpty()) {
            throw new RuntimeException("分类下存在资源，无法删除");
        }
        categoryMapper.deleteById(categoryId);
    }
}
