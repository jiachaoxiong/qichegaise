package com.qcg.repository;

import com.qcg.entity.Color;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ColorRepository extends JpaRepository<Color, Long> {
    List<Color> findByIsActiveTrue();
    List<Color> findByCategoryAndIsActiveTrue(String category);
}
