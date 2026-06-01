package com.qcg.repository;

import com.qcg.entity.CarPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CarPhotoRepository extends JpaRepository<CarPhoto, Long> {
    List<CarPhoto> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<CarPhoto> findByIdAndUserId(Long id, Long userId);
}
