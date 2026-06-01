package com.qcg.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ColorizeRequest {
    @NotNull(message = "图片ID不能为空")
    private Long photoId;

    @NotNull(message = "颜色ID不能为空")
    private Long colorId;
}
