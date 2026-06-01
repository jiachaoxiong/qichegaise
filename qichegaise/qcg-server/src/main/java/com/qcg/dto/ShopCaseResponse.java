package com.qcg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopCaseResponse {
    private Long id;
    private Long shopId;
    private String shopName;
    private String beforeUrl;
    private String afterUrl;
    private Long colorId;
    private String colorName;
    private String colorHex;
    private String carModelName;
    private String description;
    private int likes;
}
