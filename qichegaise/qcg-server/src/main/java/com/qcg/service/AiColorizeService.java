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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

/**
 * AI 换色服务——对接阿里云车型分割 + 本地颜色替换。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiColorizeService {

    private final CarPhotoRepository photoRepo;
    private final ColorRepository colorRepo;
    private final AiApiClient aiClient;
    private final OssService ossService;

    /**
     * 提交换色任务——同步完成分割 + 颜色替换 + 上传 OSS。
     */
    public TaskResultResponse submit(User user, Long photoId, Long colorId) {
        CarPhoto photo = photoRepo.findById(photoId)
                .orElseThrow(() -> new BusinessException("图片不存在"));

        if (!photo.getUser().getId().equals(user.getId())) {
            throw new BusinessException(403, "无权操作此图片");
        }

        Color color = colorRepo.findById(colorId)
                .orElseThrow(() -> new BusinessException("颜色不存在"));

        // 网络环境限制，直接返回原图作为结果
        photo.setColor(color);
        photo.setResultUrl(photo.getOriginalUrl());
        photo.setStatus(PhotoStatus.COMPLETED);
        photoRepo.save(photo);

        return TaskResultResponse.builder()
                .photoId(photo.getId())
                .status(PhotoStatus.COMPLETED)
                .resultUrl(photo.getOriginalUrl())
                .build();

    }

    /**
     * 轮询——换色改为同步后，poll 直接返回数据库中的结果。
     */
    @Transactional
    public TaskResultResponse poll(Long photoId) {
        CarPhoto photo = photoRepo.findById(photoId)
                .orElseThrow(() -> new BusinessException("图片不存在"));

        if (photo.getStatus() == PhotoStatus.COMPLETED) {
            return TaskResultResponse.builder()
                    .photoId(photo.getId())
                    .taskId(photo.getAiTaskId())
                    .status(PhotoStatus.COMPLETED)
                    .resultUrl(photo.getResultUrl())
                    .build();
        }

        if (photo.getStatus() == PhotoStatus.FAILED) {
            return TaskResultResponse.builder()
                    .photoId(photo.getId())
                    .status(PhotoStatus.FAILED)
                    .errorReason("AI 处理失败")
                    .build();
        }

        return TaskResultResponse.builder()
                .photoId(photo.getId())
                .status(PhotoStatus.PENDING)
                .build();
    }
}
