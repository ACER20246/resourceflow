package com.github.acer20246.resourceflow.service.Impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.github.acer20246.resourceflow.dto.CategoryDTO;
import com.github.acer20246.resourceflow.entity.Category;
import com.github.acer20246.resourceflow.entity.Resource;
import com.github.acer20246.resourceflow.mapper.CategoryMapper;
import com.github.acer20246.resourceflow.mapper.ResourceMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CategoryServiceImpl} 单元测试：覆盖分类的增删改查与重名/依赖校验。
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private ResourceMapper resourceMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    /**
     * MyBatis-Plus 的 Lambda 条件依赖实体的 TableInfo 缓存，
     * 纯单元测试环境中需要手动初始化。
     */
    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Category.class);
        TableInfoHelper.initTableInfo(assistant, Resource.class);
    }

    private Category buildCategory() {
        Category category = new Category();
        category.setCategoryId(1L);
        category.setName("研修室");
        category.setDescription("会议与学习空间");
        category.setStatus("ENABLED");
        return category;
    }

    @Test
    @DisplayName("查询分类列表：返回全部分类")
    void listCategories() {
        // arrange
        Category category = buildCategory();
        when(categoryMapper.selectList(any())).thenReturn(List.of(category));

        // act
        List<Category> categories = categoryService.listCategories();

        // assert
        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).getName()).isEqualTo("研修室");
    }

    @Test
    @DisplayName("新增分类：名称重复时抛出业务异常且不落库")
    void saveWithDuplicateName() {
        // arrange
        when(categoryMapper.selectCount(any())).thenReturn(1L);

        // act & assert
        assertThatThrownBy(() -> categoryService.save(new CategoryDTO("研修室", "重复分类")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("分类名称已存在");
        verify(categoryMapper, never()).insert(any(Category.class));
    }

    @Test
    @DisplayName("新增分类：成功时默认状态 ENABLED 并写入时间戳")
    void saveSuccess() {
        // arrange
        when(categoryMapper.selectCount(any())).thenReturn(0L);

        // act
        categoryService.save(CategoryDTO.builder().name("研修室").description("会议与学习空间").build());

        // assert
        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryMapper).insert(captor.capture());
        Category saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("研修室");
        assertThat(saved.getDescription()).isEqualTo("会议与学习空间");
        assertThat(saved.getStatus()).isEqualTo("ENABLED");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
    }

    @Test
    @DisplayName("修改分类：分类不存在时抛出业务异常")
    void updateCategoryNotFound() {
        // arrange
        when(categoryMapper.selectById(999L)).thenReturn(null);

        // act & assert
        assertThatThrownBy(() -> categoryService.update(999L, new CategoryDTO("新名称", "新描述")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("分类不存在");
        verify(categoryMapper, never()).updateById(any(Category.class));
    }

    @Test
    @DisplayName("修改分类：成功时更新名称和描述")
    void updateSuccess() {
        // arrange
        when(categoryMapper.selectById(1L)).thenReturn(buildCategory());

        // act
        categoryService.update(1L, CategoryDTO.builder().name("大型研修室").description("可容纳 20 人").build());

        // assert
        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryMapper).updateById(captor.capture());
        Category updated = captor.getValue();
        assertThat(updated.getCategoryId()).isEqualTo(1L);
        assertThat(updated.getName()).isEqualTo("大型研修室");
        assertThat(updated.getDescription()).isEqualTo("可容纳 20 人");
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("启用/停用分类：分类不存在时抛出业务异常")
    void changeStatusCategoryNotFound() {
        // arrange
        when(categoryMapper.selectById(999L)).thenReturn(null);

        // act & assert
        assertThatThrownBy(() -> categoryService.changeStatus(999L, 0))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("分类不存在");
    }

    @Test
    @DisplayName("启用/停用分类：1 启用，0 停用")
    void changeStatusSuccess() {
        // arrange
        when(categoryMapper.selectById(1L)).thenReturn(buildCategory());

        // act
        categoryService.changeStatus(1L, 0);

        // assert
        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("DISABLED");
    }

    @Test
    @DisplayName("删除分类：分类不存在时抛出业务异常")
    void deleteCategoryNotFound() {
        // arrange
        when(categoryMapper.selectById(999L)).thenReturn(null);

        // act & assert
        assertThatThrownBy(() -> categoryService.delete(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("分类不存在");
    }

    @Test
    @DisplayName("删除分类：分类下存在资源时不允许删除")
    void deleteWithExistingResources() {
        // arrange
        when(categoryMapper.selectById(1L)).thenReturn(buildCategory());
        Resource resource = new Resource();
        resource.setResourceId(1001L);
        resource.setCategoryId(1L);
        when(resourceMapper.selectList(any())).thenReturn(List.of(resource));

        // act & assert
        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("分类下存在资源，无法删除");
        verify(categoryMapper, never()).deleteById(any(Long.class));
    }

    @Test
    @DisplayName("删除分类：分类下无资源时删除成功")
    void deleteSuccess() {
        // arrange
        when(categoryMapper.selectById(1L)).thenReturn(buildCategory());
        when(resourceMapper.selectList(any())).thenReturn(List.of());

        // act
        categoryService.delete(1L);

        // assert
        verify(categoryMapper).deleteById(1L);
    }
}