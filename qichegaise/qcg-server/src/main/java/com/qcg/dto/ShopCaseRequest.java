package com.qcg.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShopCaseRequest {
    @NotBlank(message = "施工前照片不能为空")
    private String beforeUrl;

    @NotBlank(message = "施工后照片不能为空")
    private String afterUrl;

    private Long colorId;
    private String carModelName;
    private String description;
}
