package com.github.acer20246.resourceflow.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.acer20246.resourceflow.common.exception.BusinessException;
import com.github.acer20246.resourceflow.common.result.PageResult;
import com.github.acer20246.resourceflow.dto.ResourceDTO;
import com.github.acer20246.resourceflow.dto.ResourcePageQueryDTO;
import com.github.acer20246.resourceflow.entity.Category;
import com.github.acer20246.resourceflow.entity.Resource;
import com.github.acer20246.resourceflow.mapper.CategoryMapper;
import com.github.acer20246.resourceflow.mapper.ResourceMapper;
import com.github.acer20246.resourceflow.service.ResourceService;
import com.github.acer20246.resourceflow.vo.ResourceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {
    private final ResourceMapper resourceMapper;
    private final CategoryMapper categoryMapper;

    @Override
    public PageResult<ResourceVO> listResources(ResourcePageQueryDTO queryDTO) {
        log.info("获取资源列表");
        long pageNum = queryDTO.getPage() == null || queryDTO.getPage() < 1 ? 1 : queryDTO.getPage();
        long pageSize = queryDTO.getPageSize() == null || queryDTO.getPageSize() < 1 ? 10 : queryDTO.getPageSize();
        Page<Resource> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Resource> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(queryDTO.getName() != null && !queryDTO.getName().isBlank(), Resource::getName, queryDTO.getName())
                .eq(queryDTO.getCategoryId() != null , Resource::getCategoryId, queryDTO.getCategoryId())
                .eq(queryDTO.getStatus() != null , Resource::getStatus, queryDTO.getStatus())
                .orderByDesc(Resource::getCreatedAt);
        resourceMapper.selectPage(page, wrapper);
        List<ResourceVO> records = page.getRecords().stream()
                .map(this::convertToResourceVO)
                .collect(Collectors.toList());
        return new PageResult<>(records, page.getCurrent(), page.getSize(),page.getTotal());
    }

    /***
     * 将Resource对象转换为ResourceVO对象
     * @param resource Resource对象
     * @return ResourceVO对象
     */
    private ResourceVO convertToResourceVO(Resource resource) {
        ResourceVO resourceVO = new ResourceVO();
        BeanUtils.copyProperties(resource, resourceVO);
        return resourceVO;
    }

    @Override
    public void addResource(ResourceDTO resourceDTO) {
        log.info("新增资源：{}", resourceDTO);
        validateResource(resourceDTO);
        validateCategoryId(resourceDTO.getCategoryId());
        Resource resource = new Resource();
        BeanUtils.copyProperties(resourceDTO, resource);
        resource.setCreatedAt(LocalDateTime.now());
        resource.setUpdatedAt(LocalDateTime.now());
        resource.setStatus("ENABLED");
        resourceMapper.insert(resource);
    }

    /**
     * 更新资源信息
     * @param resourceId
     * @param resourceDTO
     */
    @Override
    public void updateResource(Long resourceId, ResourceDTO resourceDTO) {

        Resource resource = resourceMapper.selectById(resourceId);
        if (resource == null) {
            throw new BusinessException("资源不存在");
        }
        validateResource(resourceDTO);
        validateCategoryId(resourceDTO.getCategoryId());
        BeanUtils.copyProperties(resourceDTO, resource);
        resource.setUpdatedAt(LocalDateTime.now());
        resourceMapper.updateById(resource);
    }

    /**
     * 删除资源
     * @param resourceId
     */
    @Override
    public void deleteResource(Long resourceId) {
        Resource resource = resourceMapper.selectById(resourceId);
        if (resource == null) {
            throw new BusinessException("资源不存在");
        }
        resourceMapper.deleteById(resourceId);
    }

    @Override
    public ResourceVO getResource(Long resourceId) {
        log.info("查询资源详情：{}", resourceId);
        return convertToResourceVO(resourceMapper.selectById(resourceId));
    }

    /**
     * 验证资源请求参数是否合法
     * @param dto
     */
    private void validateResource(ResourceDTO dto) {
        if (dto == null) {
            throw new BusinessException("资源请求不能为空");
        }
        if (dto.getCategoryId() == null) {
            throw new BusinessException("分类 ID 不能为空");
        }
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BusinessException("资源名称不能为空");
        }
        if (dto.getLocation() == null || dto.getLocation().isBlank()) {
            throw new BusinessException("资源位置不能为空");
        }
        if (dto.getCapacity() == null || dto.getCapacity() <= 0) {
            throw new BusinessException("容纳人数必须大于 0");
        }
    }

    /**
     * 验证分类是否存在
     * @param CategoryId
     */
    private void validateCategoryId(Long CategoryId) {
        Category category = categoryMapper.selectById(CategoryId);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
    }

}
