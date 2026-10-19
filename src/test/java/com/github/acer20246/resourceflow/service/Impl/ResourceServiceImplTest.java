package com.github.acer20246.resourceflow.service.Impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.acer20246.resourceflow.common.exception.BusinessException;
import com.github.acer20246.resourceflow.common.result.PageResult;
import com.github.acer20246.resourceflow.dto.ResourceDTO;
import com.github.acer20246.resourceflow.dto.ResourcePageQueryDTO;
import com.github.acer20246.resourceflow.entity.Category;
import com.github.acer20246.resourceflow.entity.Resource;
import com.github.acer20246.resourceflow.mapper.CategoryMapper;
import com.github.acer20246.resourceflow.mapper.ResourceMapper;
import com.github.acer20246.resourceflow.vo.ResourceVO;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ResourceServiceImpl} 单元测试：覆盖资源分页查询、参数校验、增删改查。
 */
@ExtendWith(MockitoExtension.class)
class ResourceServiceImplTest {

    @Mock
    private ResourceMapper resourceMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private ResourceServiceImpl resourceService;

    /**
     * MyBatis-Plus 的 Lambda 条件依赖实体的 TableInfo 缓存，
     * 纯单元测试环境中需要手动初始化。
     */
    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Resource.class);
        TableInfoHelper.initTableInfo(assistant, Category.class);
    }

    private Resource buildResource() {
        Resource resource = new Resource();
        resource.setResourceId(1001L);
        resource.setCategoryId(1L);
        resource.setName("A101 研修室");
        resource.setLocation("1 楼 A101");
        resource.setCapacity(12);
        resource.setDescription("配备投影仪");
        resource.setStatus("ENABLED");
        return resource;
    }

    private Category buildCategory() {
        Category category = new Category();
        category.setCategoryId(1L);
        category.setName("研修室");
        category.setStatus("ENABLED");
        return category;
    }

    private ResourceDTO buildResourceDTO() {
        return new ResourceDTO("A101 研修室", "配备投影仪", 1L, "1 楼 A101", 12);
    }

    @Test
    @DisplayName("分页查询资源：DTO 默认值为第 1 页、每页 20 条")
    void listResourcesWithDefaultPageParams() {
        // arrange
        doAnswer(invocation -> {
            Page<Resource> page = invocation.getArgument(0);
            page.setRecords(List.of(buildResource()));
            page.setTotal(1);
            return page;
        }).when(resourceMapper).selectPage(any(), any());

        // act
        PageResult<ResourceVO> result = resourceService.listResources(new ResourcePageQueryDTO());

        // assert
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(resourceMapper).selectPage(pageCaptor.capture(), any());
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1L);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(20L);
        assertThat(result.records()).hasSize(1);
        assertThat(result.records().get(0).getResourceId()).isEqualTo(1001L);
        assertThat(result.records().get(0).getName()).isEqualTo("A101 研修室");
        assertThat(result.total()).isEqualTo(1L);
    }

    @Test
    @DisplayName("分页查询资源：分页参数为 null 时服务层回退为第 1 页、每页 10 条")
    void listResourcesWithNullPageParams() {
        // arrange：分页参数显式置为 null
        doAnswer(invocation -> {
            Page<Resource> page = invocation.getArgument(0);
            page.setRecords(List.of());
            page.setTotal(0);
            return page;
        }).when(resourceMapper).selectPage(any(), any());

        // act
        resourceService.listResources(new ResourcePageQueryDTO(null, null, null, null, null));

        // assert
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(resourceMapper).selectPage(pageCaptor.capture(), any());
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1L);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(10L);
    }

    @Test
    @DisplayName("新增资源：请求为空或字段非法时抛出业务异常")
    void addResourceWithInvalidParams() {
        // act & assert
        assertThatThrownBy(() -> resourceService.addResource(null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("资源请求不能为空");

        ResourceDTO noCategoryId = buildResourceDTO();
        noCategoryId.setCategoryId(null);
        assertThatThrownBy(() -> resourceService.addResource(noCategoryId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("分类 ID 不能为空");

        ResourceDTO blankName = buildResourceDTO();
        blankName.setName("  ");
        assertThatThrownBy(() -> resourceService.addResource(blankName))
                .isInstanceOf(BusinessException.class)
                .hasMessage("资源名称不能为空");

        ResourceDTO blankLocation = buildResourceDTO();
        blankLocation.setLocation(" ");
        assertThatThrownBy(() -> resourceService.addResource(blankLocation))
                .isInstanceOf(BusinessException.class)
                .hasMessage("资源位置不能为空");

        ResourceDTO nonPositiveCapacity = buildResourceDTO();
        nonPositiveCapacity.setCapacity(0);
        assertThatThrownBy(() -> resourceService.addResource(nonPositiveCapacity))
                .isInstanceOf(BusinessException.class)
                .hasMessage("容纳人数必须大于 0");

        verify(resourceMapper, never()).insert(any(Resource.class));
    }

    @Test
    @DisplayName("新增资源：分类不存在时抛出业务异常")
    void addResourceWithUnknownCategory() {
        // arrange
        when(categoryMapper.selectById(999L)).thenReturn(null);

        // act & assert
        ResourceDTO dto = buildResourceDTO();
        dto.setCategoryId(999L);
        assertThatThrownBy(() -> resourceService.addResource(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("分类不存在");
        verify(resourceMapper, never()).insert(any(Resource.class));
    }

    @Test
    @DisplayName("新增资源：成功时默认状态 ENABLED 并写入时间戳")
    void addResourceSuccess() {
        // arrange
        when(categoryMapper.selectById(1L)).thenReturn(buildCategory());

        // act
        resourceService.addResource(buildResourceDTO());

        // assert
        ArgumentCaptor<Resource> captor = ArgumentCaptor.forClass(Resource.class);
        verify(resourceMapper).insert(captor.capture());
        Resource saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("A101 研修室");
        assertThat(saved.getLocation()).isEqualTo("1 楼 A101");
        assertThat(saved.getCapacity()).isEqualTo(12);
        assertThat(saved.getStatus()).isEqualTo("ENABLED");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
    }

    @Test
    @DisplayName("修改资源：资源不存在时抛出业务异常")
    void updateResourceNotFound() {
        // arrange
        when(resourceMapper.selectById(999L)).thenReturn(null);

        // act & assert
        assertThatThrownBy(() -> resourceService.updateResource(999L, buildResourceDTO()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("资源不存在");
        verify(resourceMapper, never()).updateById(any(Resource.class));
    }

    @Test
    @DisplayName("修改资源：成功时更新字段和更新时间")
    void updateResourceSuccess() {
        // arrange
        when(resourceMapper.selectById(1001L)).thenReturn(buildResource());
        when(categoryMapper.selectById(1L)).thenReturn(buildCategory());
        ResourceDTO dto = new ResourceDTO("A102 研修室", "新描述", 1L, "1 楼 A102", 20);

        // act
        resourceService.updateResource(1001L, dto);

        // assert
        ArgumentCaptor<Resource> captor = ArgumentCaptor.forClass(Resource.class);
        verify(resourceMapper).updateById(captor.capture());
        Resource updated = captor.getValue();
        assertThat(updated.getResourceId()).isEqualTo(1001L);
        assertThat(updated.getName()).isEqualTo("A102 研修室");
        assertThat(updated.getCapacity()).isEqualTo(20);
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("删除资源：资源不存在时抛出业务异常")
    void deleteResourceNotFound() {
        // arrange
        when(resourceMapper.selectById(999L)).thenReturn(null);

        // act & assert
        assertThatThrownBy(() -> resourceService.deleteResource(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("资源不存在");
        verify(resourceMapper, never()).deleteById(any(Long.class));
    }

    @Test
    @DisplayName("删除资源：成功时调用删除")
    void deleteResourceSuccess() {
        // arrange
        when(resourceMapper.selectById(1001L)).thenReturn(buildResource());

        // act
        resourceService.deleteResource(1001L);

        // assert
        verify(resourceMapper).deleteById(1001L);
    }

    @Test
    @DisplayName("查询资源详情：返回字段映射正确的 VO")
    void getResourceSuccess() {
        // arrange
        when(resourceMapper.selectById(1001L)).thenReturn(buildResource());

        // act
        ResourceVO vo = resourceService.getResource(1001L);

        // assert
        assertThat(vo.getResourceId()).isEqualTo(1001L);
        assertThat(vo.getName()).isEqualTo("A101 研修室");
        assertThat(vo.getLocation()).isEqualTo("1 楼 A101");
        assertThat(vo.getCapacity()).isEqualTo(12);
        assertThat(vo.getStatus()).isEqualTo("ENABLED");
    }
}