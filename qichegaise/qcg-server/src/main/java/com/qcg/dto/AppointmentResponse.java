package com.qcg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long shopId;
    private String shopName;
    private Long carPhotoId;
    private String resultUrl;
    private Long colorId;
    private String colorName;
    private String colorHex;
    private LocalDateTime appointmentTime;
    private String status;
    private String remark;
    private LocalDateTime createdAt;
}
