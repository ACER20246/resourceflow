package com.github.acer20246.resourceflow.controller;
import com.github.acer20246.resourceflow.common.result.PageResult;
import com.github.acer20246.resourceflow.dto.ResourceSlotPageDTO;
import com.github.acer20246.resourceflow.entity.ResourceSlot;
import com.github.acer20246.resourceflow.service.ResourceSlotService;
import com.github.acer20246.resourceflow.vo.SlotVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * 研修室时间段查询接口。
 *
 * <p>基础路径 {@code /api/resources}，包含管理端分页查询和用户端按日查询。
 * 该路径在 SecurityConfig 中为匿名可访问。</p>
 */
@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
@Slf4j
public class ResourceSlotController {
    private final ResourceSlotService resourceSlotService;

    /**
     *
     * @param pageDTO 查询条件，字段为空表示不参与过滤
     * @return 时间段分页结果
     */
    @GetMapping("/slots")
    public PageResult<ResourceSlot> getSlots(ResourceSlotPageDTO pageDTO) {
        log.info("获取资源时段: {}", pageDTO);
        return resourceSlotService.getSlots(pageDTO);
    }

    /**
     * 查询指定研修室某一天的全部时间段。
     *
     * @param resourceId 研修室 ID
     * @param date       预约日期
     * @return 该研修室当天的时段视图
     */
    @GetMapping("/{resourceId}/slots")
    public SlotVO getSlotsByResourceIdAndDate(@PathVariable Long resourceId, Date date) {
        log.info("获取资源时段通过资源ID: {}", resourceId);
        return resourceSlotService.getSlotsByResourceIdAndDate(resourceId, date);
    }

}
