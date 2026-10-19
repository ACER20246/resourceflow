package com.github.acer20246.resourceflow.service;

import com.github.acer20246.resourceflow.dto.CategoryDTO;
import com.github.acer20246.resourceflow.entity.Category;

import java.util.List;

public interface CategoryService {
    List<Category> listCategories();

    void save(CategoryDTO categoryDTO);

    void update(Long categoryId, CategoryDTO categoryDTO);

    void changeStatus(Long categoryId, Integer status);

    void delete(Long categoryId);
}
