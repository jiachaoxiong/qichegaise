package com.qcg.repository;

import com.qcg.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Favorite> findByUserIdAndShopId(Long userId, Long shopId);
    boolean existsByUserIdAndShopId(Long userId, Long shopId);
    void deleteByUserIdAndShopId(Long userId, Long shopId);
}
