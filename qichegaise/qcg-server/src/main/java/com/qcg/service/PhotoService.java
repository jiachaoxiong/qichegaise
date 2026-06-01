package com.qcg.service;

import com.qcg.common.BusinessException;
import com.qcg.entity.CarPhoto;
import com.qcg.entity.User;
import com.qcg.repository.CarPhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PhotoService {

    private final OssService ossService;
    private final CarPhotoRepository carPhotoRepository;

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png");
    private static final long MAX_SIZE = 10 * 1024 * 1024;

    @Transactional
    public CarPhoto upload(User user, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("请选择图片");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BusinessException("仅支持 JPG/PNG 格式");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("图片大小不能超过 10MB");
        }

        String url = ossService.upload(file);

        CarPhoto photo = CarPhoto.builder()
                .user(user)
                .originalUrl(url)
                .build();

        return carPhotoRepository.save(photo);
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
