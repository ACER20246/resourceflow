package com.github.acer20246.resourceflow.vo;

import com.github.acer20246.resourceflow.entity.ResourceSlot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 某研修室某一天的可预约时段视图。
 *
 * <p>直接内嵌 {@link ResourceSlot} 列表，供前端按时段渲染可预约状态。
 * 日期使用 {@link Date}，由服务端按统一时区解析后返回。</p>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SlotVO {
    /** 研修室 ID */
    private Long resourceId;
    /** 查询的预约日期 */
    private Date slotDate;
    /** 当天的全部时间段，按开始时间升序 */
    private List<ResourceSlot> slots;
}
