package com.qcg.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AppointmentRequest {
    @NotNull(message = "门店ID不能为空")
    private Long shopId;
    private Long carPhotoId;
    private Long colorId;
    @NotNull(message = "预约时间不能为空")
    private LocalDateTime appointmentTime;
    private String remark;
}
