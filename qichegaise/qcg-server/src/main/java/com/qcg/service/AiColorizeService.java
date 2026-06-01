package com.qcg.service;

import com.qcg.client.AiApiClient;
import com.qcg.common.BusinessException;
import com.qcg.dto.TaskResultResponse;
import com.qcg.entity.CarPhoto;
import com.qcg.entity.Color;
import com.qcg.entity.User;
import com.qcg.enums.PhotoStatus;
import com.qcg.repository.CarPhotoRepository;
import com.qcg.repository.ColorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiColorizeService {

    private final CarPhotoRepository photoRepo;
    private final ColorRepository colorRepo;
    private final AiApiClient aiClient;
    private final OssService ossService;

    @Transactional
    public TaskResultResponse submit(User user, Long photoId, Long colorId) {
        CarPhoto photo = photoRepo.findById(photoId)
                .orElseThrow(() -> new BusinessException("图片不存在"));

        if (!photo.getUser().getId().equals(user.getId())) {
            throw new BusinessException(403, "无权操作此图片");
        }

        Color color = colorRepo.findById(colorId)
                .orElseThrow(() -> new BusinessException("颜色不存在"));

        String taskId = aiClient.submitTask(photo.getOriginalUrl(), color.getHexCode());

        photo.setAiTaskId(taskId);
        photo.setColor(color);
        photo.setStatus(PhotoStatus.PENDING);
        photoRepo.save(photo);

        return TaskResultResponse.builder()
                .photoId(photo.getId())
                .taskId(taskId)
                .status(PhotoStatus.PENDING)
                .build();
    }

    @Transactional
    public TaskResultResponse poll(Long photoId) {
        CarPhoto photo = photoRepo.findById(photoId)
                .orElseThrow(() -> new BusinessException("图片不存在"));

        if (photo.getAiTaskId() == null) {
            throw new BusinessException("该图片未提交 AI 任务");
        }

        AiApiClient.TaskStatus aiStatus = aiClient.queryTask(photo.getAiTaskId());

        if (aiStatus == AiApiClient.TaskStatus.COMPLETED) {
            String resultUrl = aiClient.getResultUrl(photo.getAiTaskId());
            String ossUrl = ossService.uploadFromUrl(resultUrl);

            photo.setResultUrl(ossUrl);
            photo.setStatus(PhotoStatus.COMPLETED);
            photoRepo.save(photo);

            return TaskResultResponse.builder()
                    .photoId(photo.getId())
                    .taskId(photo.getAiTaskId())
                    .status(PhotoStatus.COMPLETED)
                    .resultUrl(ossUrl)
                    .build();
        }

        if (aiStatus == AiApiClient.TaskStatus.FAILED) {
            photo.setStatus(PhotoStatus.FAILED);
            photoRepo.save(photo);

            return TaskResultResponse.builder()
                    .photoId(photo.getId())
                    .taskId(photo.getAiTaskId())
                    .status(PhotoStatus.FAILED)
                    .errorReason("AI 处理失败，请确认上传的是清晰的整车照片")
                    .build();
        }

        return TaskResultResponse.builder()
                .photoId(photo.getId())
                .taskId(photo.getAiTaskId())
                .status(PhotoStatus.PENDING)
                .build();
    }
}
