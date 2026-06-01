package com.qcg.service;

import com.qcg.entity.Color;
import com.qcg.repository.ColorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ColorService {

    private final ColorRepository colorRepository;

    public List<Color> listAll() {
        return colorRepository.findByIsActiveTrue();
    }

    public List<Color> listByCategory(String category) {
        return colorRepository.findByCategoryAndIsActiveTrue(category);
    }

    public Color getById(Long id) {
        return colorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("颜色不存在"));
    }
}
