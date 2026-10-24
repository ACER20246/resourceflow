package com.github.acer20246.resourceflow.service;

import com.github.acer20246.resourceflow.common.result.PageResult;
import com.github.acer20246.resourceflow.dto.ResourceSlotPageDTO;
import com.github.acer20246.resourceflow.entity.ResourceSlot;
import com.github.acer20246.resourceflow.vo.SlotVO;

import java.util.Date;

/**
 * 研修室时间段服务。
 *
 * <p>提供管理端的分页查询和用户端的按日查询两类能力。</p>
 */
public interface ResourceSlotService {

    /**
     * 分页查询时间段，供管理端维护使用。
     *
     * @param pageDTO 查询条件，字段为空表示不参与过滤
     * @return 时间段分页结果
     */
    PageResult<ResourceSlot> getSlots(ResourceSlotPageDTO pageDTO);

    /**
     * 查询指定研修室某一天的全部时间段。
     *
     * @param resourceId 研修室 ID
     * @param date       预约日期
     * @return 该研修室当天的时段视图
     */
    SlotVO getSlotsByResourceIdAndDate(Long resourceId, Date date);
}
