package com.github.acer20246.resourceflow.service;

import com.github.acer20246.resourceflow.common.result.PageResult;
import com.github.acer20246.resourceflow.dto.ResourceDTO;
import com.github.acer20246.resourceflow.dto.ResourcePageQueryDTO;
import com.github.acer20246.resourceflow.vo.ResourceVO;

public interface ResourceService {
    PageResult<ResourceVO> listResources(ResourcePageQueryDTO queryDTO);

    void addResource(ResourceDTO resourceDTO);

    void updateResource(Long resourceId, ResourceDTO resourceDTO);

    void deleteResource(Long resourceId);

    ResourceVO getResource(Long resourceId);
}
