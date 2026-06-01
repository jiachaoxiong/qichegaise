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
    @Transactional
    public TaskResultResponse submit(User user, Long photoId, Long colorId) {
        CarPhoto photo = photoRepo.findById(photoId)
                .orElseThrow(() -> new BusinessException("图片不存在"));

        if (!photo.getUser().getId().equals(user.getId())) {
            throw new BusinessException(403, "无权操作此图片");
        }

        Color color = colorRepo.findById(colorId)
                .orElseThrow(() -> new BusinessException("颜色不存在"));

        try {
            // 1. 阿里云车型分割 → 获取分割图 URL
            String taskId = aiClient.submitTask(photo.getOriginalUrl(), color.getHexCode());
            String segmentedUrl = aiClient.getResultUrl(taskId);
            String targetHex = aiClient.getTargetColor(taskId);

            // 2. 下载分割图 + 颜色替换
            BufferedImage segmented = ImageIO.read(new URL(segmentedUrl));
            BufferedImage colorized = AiApiClient.replaceColor(segmented, targetHex);

            // 3. 转为 byte[] 并上传 OSS
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(colorized, "PNG", baos);
            byte[] resultBytes = baos.toByteArray();

            String ossUrl = ossService.uploadBytes(resultBytes, "result-" + photoId + ".png");

            // 4. 更新数据库
            photo.setAiTaskId(taskId);
            photo.setColor(color);
            photo.setResultUrl(ossUrl);
            photo.setStatus(PhotoStatus.COMPLETED);
            photoRepo.save(photo);

            return TaskResultResponse.builder()
                    .photoId(photo.getId())
                    .taskId(taskId)
                    .status(PhotoStatus.COMPLETED)
                    .resultUrl(ossUrl)
                    .build();

        } catch (Exception e) {
            log.error("AI API 调用失败，使用本地降级渲染: photoId={}", photoId, e);
            try {
                // 降级方案：尝试多种方式下载原图 → 全图着色
                BufferedImage original = null;
                String imgUrl = photo.getOriginalUrl();
                if (imgUrl == null || imgUrl.isEmpty()) {
                    throw new RuntimeException("图片URL为空");
                }
                // 先尝试 URL 直接下载（OSS 公网可达时）
                try {
                    original = ImageIO.read(new URL(imgUrl));
                } catch (Exception urlEx) {
                    // URL 下载失败，尝试 OSS SDK 下载
                    try {
                        byte[] bytes = ossService.download(imgUrl);
                        original = ImageIO.read(new ByteArrayInputStream(bytes));
                    } catch (Exception sdkEx) {
                        throw new RuntimeException("所有下载方式均失败", sdkEx);
                    }
                }
                if (original == null) throw new RuntimeException("无法读取图片");
                BufferedImage tinted = AiApiClient.replaceColor(original, color.getHexCode());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(tinted, "PNG", baos);
                String ossUrl = ossService.uploadBytes(baos.toByteArray(), "fallback-" + photoId + ".png");

                photo.setAiTaskId("fallback-" + System.currentTimeMillis());
                photo.setColor(color);
                photo.setResultUrl(ossUrl);
                photo.setStatus(PhotoStatus.COMPLETED);
                photoRepo.save(photo);

                return TaskResultResponse.builder()
                        .photoId(photo.getId())
                        .status(PhotoStatus.COMPLETED)
                        .resultUrl(ossUrl)
                        .build();
            } catch (Exception fallbackErr) {
                log.error("降级渲染也失败: photoId={}", photoId, fallbackErr);
                photo.setStatus(PhotoStatus.FAILED);
                photoRepo.save(photo);
                return TaskResultResponse.builder()
                        .photoId(photo.getId())
                        .status(PhotoStatus.FAILED)
                        .errorReason("AI 处理失败，请重新上传")
                        .build();
            }
        }
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
