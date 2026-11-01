package com.github.acer20246.resourceflow.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.acer20246.resourceflow.common.result.PageResult;
import com.github.acer20246.resourceflow.dto.ResourceSlotPageDTO;
import com.github.acer20246.resourceflow.entity.ResourceSlot;
import com.github.acer20246.resourceflow.mapper.ResourceSlotMapper;
import com.github.acer20246.resourceflow.service.ResourceSlotService;
import com.github.acer20246.resourceflow.vo.SlotVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 时间段服务实现。
 *
 * <p>当前实现直接依赖 MyBatis-Plus 的条件构造器完成查询，未引入缓存；
 * 时间段库存的并发扣减由 {@code ResourceSlotMapper#tryOccupy} 在数据库层保证。</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ResourceSlotServiceImpl implements ResourceSlotService {
    private final ResourceSlotMapper resourceSlotMapper;
    /**
     * 分页查询时间段。
     *
     * <p>页码和每页条数做了兜底：为空或小于 1 时分别取 1 和 10，避免负数或 0 导致 SQL 异常。</p>
     *
     * @param pageDTO 查询条件
     * @return 时间段分页结果
     */
    @Override
    public PageResult<ResourceSlot> getSlots(ResourceSlotPageDTO pageDTO) {
        log.info("获取资源时段: {}", pageDTO);
        long pageNum = pageDTO.getPage() == null || pageDTO.getPage() < 1 ? 1 : pageDTO.getPage();
        long pageSize = pageDTO.getPageSize() == null || pageDTO.getPageSize() < 1 ? 10 : pageDTO.getPageSize();
        Page<ResourceSlot> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ResourceSlot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(pageDTO.getResourceId() != null, ResourceSlot::getResourceId, pageDTO.getResourceId())
                .eq(pageDTO.getSlotDate() != null, ResourceSlot::getSlotDate, pageDTO.getSlotDate())
                .ge(pageDTO.getStartTime() != null, ResourceSlot::getStartTime, pageDTO.getStartTime())
                // 时间段结束：小于等于 endTime
                .le(pageDTO.getEndTime() != null, ResourceSlot::getEndTime, pageDTO.getEndTime())
                .eq(pageDTO.getStatus() != null, ResourceSlot::getStatus, pageDTO.getStatus())
        ;

        Page<ResourceSlot> resultPage = resourceSlotMapper.selectPage(page, wrapper);
        return new PageResult<>(resultPage.getRecords(), page.getCurrent(), page.getSize(),page.getTotal());
    }

    /**
     * 查询指定研修室某一天的全部时间段。
     *
     * <p>按研修室 ID 和日期做等值过滤，返回结果未做排序，前端如需按时间展示应自行排序。</p>
     *
     * @param resourceId 研修室 ID
     * @param date       预约日期
     * @return 该研修室当天的时段视图
     */
    @Override
    public SlotVO getSlotsByResourceIdAndDate(Long resourceId, Date date) {
        log.info("获取资源时段通过资源ID和日期: {}, {}", resourceId, date);
        LambdaQueryWrapper<ResourceSlot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResourceSlot::getResourceId, resourceId)
                .eq(ResourceSlot::getSlotDate, date);
        return SlotVO.builder()
                .resourceId(resourceId)
                .slotDate(date)
                .slots(resourceSlotMapper.selectList(wrapper))
                .build();
    }
}
