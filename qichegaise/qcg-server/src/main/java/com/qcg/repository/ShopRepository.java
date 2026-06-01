package com.qcg.repository;

import com.qcg.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ShopRepository extends JpaRepository<Shop, Long> {
    List<Shop> findByStatusOrderByIdDesc(String status);
    List<Shop> findByOwnerId(Long ownerId);
}
