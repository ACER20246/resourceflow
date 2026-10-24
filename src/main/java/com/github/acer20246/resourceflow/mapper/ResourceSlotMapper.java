package com.github.acer20246.resourceflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.acer20246.resourceflow.entity.ResourceSlot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ResourceSlotMapper extends BaseMapper<ResourceSlot> {

    /**
     * 原子占用一个名额。
     *
     * <p>只有影响行数为 1 时才允许写入预约记录。这里把 status 的计算放在 reserved_count
     * 之前，因为 MySQL 单表 UPDATE 按 SET 子句从左到右赋值，后面的表达式会读到已更新的值。</p>
     *
     * @param slotId 时间段 ID
     * @return 影响行数，0 表示名额已被抢完或时间段不可预约
     */
    int tryOccupy(@Param("slotId") Long slotId);

    /**
     * 回补一个名额，用于取消和失效。
     * @param slotId 时间段 ID
     * @return 影响行数
     */
    int release(@Param("slotId") Long slotId);
}
