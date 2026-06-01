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
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 本地换色服务——下载原图 → HSB 着色 → 上传 OSS。
 * 不依赖阿里云视觉 API，纯本地处理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiColorizeService {

    private final CarPhotoRepository photoRepo;
    private final ColorRepository colorRepo;
    private final OssService ossService;

    public TaskResultResponse submit(User user, Long photoId, Long colorId) {
        CarPhoto photo = photoRepo.findById(photoId)
                .orElseThrow(() -> new BusinessException("图片不存在"));

        if (!photo.getUser().getId().equals(user.getId())) {
            throw new BusinessException(403, "无权操作此图片");
        }

        Color color = colorRepo.findById(colorId)
                .orElseThrow(() -> new BusinessException("颜色不存在"));

        String imageUrl = photo.getOriginalUrl();
        if (imageUrl == null || imageUrl.isEmpty()) {
            throw new BusinessException("图片URL为空，请重新上传");
        }

        try {
            // 1. 下载原图
            BufferedImage original = downloadImage(imageUrl);
            if (original == null) {
                throw new BusinessException("无法读取图片，请确认图片格式正确");
            }

            // 2. HSB 颜色替换
            BufferedImage colorized = AiApiClient.replaceColor(original, color.getHexCode());

            // 3. 上传结果到 OSS
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(colorized, "PNG", baos);
            byte[] resultBytes = baos.toByteArray();
            String resultUrl = ossService.uploadBytes(resultBytes, "result-" + photoId + ".png");

            // 4. 更新数据库
            photo.setColor(color);
            photo.setResultUrl(resultUrl);
            photo.setStatus(PhotoStatus.COMPLETED);
            photoRepo.save(photo);

            log.info("换色成功: photoId={}, color={}, resultUrl={}", photoId, color.getHexCode(), resultUrl);
            return TaskResultResponse.builder()
                    .photoId(photo.getId())
                    .status(PhotoStatus.COMPLETED)
                    .resultUrl(resultUrl)
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("换色失败: photoId={}", photoId, e);
            photo.setStatus(PhotoStatus.FAILED);
            photoRepo.save(photo);
            return TaskResultResponse.builder()
                    .photoId(photo.getId())
                    .status(PhotoStatus.FAILED)
                    .errorReason("换色处理失败: " + e.getMessage())
                    .build();
        }
    }

    /** 下载图片——优先 HTTP，失败则 OSS SDK */
    private BufferedImage downloadImage(String url) throws IOException {
        // 先尝试 HTTP 直接下载
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "QCG-Server");
            try (InputStream is = conn.getInputStream()) {
                BufferedImage img = ImageIO.read(is);
                if (img != null) return img;
            }
        } catch (Exception httpEx) {
            log.warn("HTTP下载失败: {}", httpEx.getMessage());
        }

        // HTTP 失败或返回非图片，尝试 OSS SDK
        try {
            byte[] bytes = ossService.download(url);
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img != null) return img;
            throw new IOException("ImageIO 无法解码图片");
        } catch (Exception ossEx) {
            log.error("OSS SDK下载也失败: {}", ossEx.getMessage());
            throw new IOException("所有下载方式均失败: " + url);
        }
    }

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
                    .errorReason("换色处理失败")
                    .build();
        }

        return TaskResultResponse.builder()
                .photoId(photo.getId())
                .status(PhotoStatus.PENDING)
                .build();
    }
}
