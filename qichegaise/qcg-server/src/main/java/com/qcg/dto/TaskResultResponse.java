package com.qcg.dto;

import com.qcg.enums.PhotoStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResultResponse {
    private Long photoId;
    private String taskId;
    private PhotoStatus status;
    private String resultUrl;
    private String errorReason;
}
