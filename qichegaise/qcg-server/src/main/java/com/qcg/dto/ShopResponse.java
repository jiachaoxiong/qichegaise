package com.qcg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopResponse {
    private Long id;
    private String name;
    private String address;
    private BigDecimal lat;
    private BigDecimal lng;
    private String phone;
    private String coverUrl;
    private String description;
    private String status;
    private Long ownerId;
    private String ownerName;
    private int caseCount;
    private boolean isFavorited;
}
