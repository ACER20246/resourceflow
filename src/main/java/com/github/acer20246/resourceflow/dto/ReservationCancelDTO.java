package com.github.acer20246.resourceflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 取消预约请求参数，取消原因可为空。
 */
@Schema(description = "取消预约请求")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationCancelDTO {

    @Schema(description = "取消原因")
    private String reason;
}
