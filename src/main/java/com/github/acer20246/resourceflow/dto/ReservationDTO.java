package com.github.acer20246.resourceflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建预约请求参数。
 *
 * <p>{@code requestId} 用于幂等：同一用户重复提交相同 requestId 只会生成一笔预约。</p>
 */
@Schema(description = "创建预约请求")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDTO {
    @Schema(description = "研修室 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long resourceId;

    @Schema(description = "时间段 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long slotId;

    @Schema(description = "客户端幂等请求号，重复提交返回同一笔预约", requiredMode = Schema.RequiredMode.REQUIRED)
    private String requestId;

    @Schema(description = "预约备注")
    private String remark;
}
