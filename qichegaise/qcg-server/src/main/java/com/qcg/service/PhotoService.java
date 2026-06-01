package com.qcg.service;

import com.qcg.common.BusinessException;
import com.qcg.entity.CarPhoto;
import com.qcg.entity.User;
import com.qcg.repository.CarPhotoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoService {

    private final OssService ossService;
    private final CarPhotoRepository carPhotoRepository;

    private static final Set<String> ALLOWED_MIME = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_SIZE = 10 * 1024 * 1024;

    @Transactional
    public CarPhoto upload(User user, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("请选择图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("图片大小不能超过 10MB");
        }

        // 预读字节用于魔数检测 + ImageIO 解码验证
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("图片读取失败，请重试");
        }

        // 检测真实格式（魔数），比 Content-Type 更可靠
        String realFormat = detectImageFormat(bytes);
        if (realFormat == null) {
            throw new BusinessException("无法识别图片格式，仅支持 JPG/PNG/WebP");
        }
        log.info("上传图片真实格式: {} (Content-Type: {})", realFormat, file.getContentType());

        // 验证 ImageIO 能否解码（包括 WebP，依赖 imageio-webp 插件）
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) {
                throw new BusinessException("无法解析图片，请确认图片未损坏且格式为 JPG/PNG/WebP");
            }
        } catch (IOException e) {
            log.error("ImageIO解码失败", e);
            throw new BusinessException("图片解码失败，请尝试其他图片");
        }

        // 通过魔数确定扩展名，确保 OSS 上的后缀正确
        String url = ossService.uploadBytes("photos", bytes, getFilename(realFormat));

        CarPhoto photo = CarPhoto.builder()
                .user(user)
                .originalUrl(url)
                .build();

        return carPhotoRepository.save(photo);
    }

    /**
     * 通过魔数检测图片真实格式。
     * 返回 "jpg" / "png" / "webp"，未知格式返回 null。
     */
    private String detectImageFormat(byte[] bytes) {
        if (bytes == null || bytes.length < 12) return null;
        int b0 = bytes[0] & 0xFF;
        int b1 = bytes[1] & 0xFF;
        int b2 = bytes[2] & 0xFF;
        int b3 = bytes[3] & 0xFF;
        // JPEG: FF D8 FF
        if (b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF) return "jpg";
        // PNG: 89 50 4E 47
        if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47) return "png";
        // WebP: 52 49 46 46 (RIFF) + 57 45 42 50 (WEBP) at offset 8
        if (b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46
                && bytes.length >= 12
                && bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50) return "webp";
        return null;
    }

    private String getFilename(String format) {
        return "photo." + format;
    }

    public List<CarPhoto> listByUser(Long userId) {
        return carPhotoRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public void delete(User user, Long photoId) {
        CarPhoto photo = carPhotoRepository.findByIdAndUserId(photoId, user.getId())
                .orElseThrow(() -> new BusinessException("作品不存在"));
        carPhotoRepository.delete(photo);
    }

    @Transactional
    public CarPhoto createFromUrl(User user, String imageUrl) {
        CarPhoto photo = CarPhoto.builder()
                .user(user)
                .originalUrl(imageUrl)
                .build();
        return carPhotoRepository.save(photo);
    }
}
