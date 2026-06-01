package com.qcg.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ColorizeRequest {
    @NotNull(message = "图片ID不能为空")
    private Long photoId;

    @NotNull(message = "颜色ID不能为空")
    private Long colorId;

    /** 用户点击车身的坐标（相对值 0.0 ~ 1.0），用于精准定位车身区域 */
    private Float sampleX;
    private Float sampleY;
}
