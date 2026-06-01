package com.qcg.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShopRegisterRequest {
    @NotBlank(message = "门店名称不能为空")
    private String name;
    private String address;
    private String phone;
    private String description;
}
