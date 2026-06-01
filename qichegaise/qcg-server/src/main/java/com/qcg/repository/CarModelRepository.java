package com.qcg.repository;

import com.qcg.entity.CarModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CarModelRepository extends JpaRepository<CarModel, Long> {
    List<CarModel> findByIsActiveTrue();
    List<CarModel> findByBrandNameAndIsActiveTrue(String brandName);
}
