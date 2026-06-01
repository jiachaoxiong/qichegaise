package com.qcg.repository;

import com.qcg.entity.ShopCase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ShopCaseRepository extends JpaRepository<ShopCase, Long> {
    List<ShopCase> findByShopIdOrderByIdDesc(Long shopId);
}
