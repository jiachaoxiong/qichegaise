package com.qcg.service;

import com.qcg.entity.CarModel;
import com.qcg.repository.CarModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CarModelService {

    private final CarModelRepository carModelRepository;

    public List<CarModel> listAll() {
        return carModelRepository.findByIsActiveTrue();
    }

    public List<CarModel> listByBrand(String brandName) {
        return carModelRepository.findByBrandNameAndIsActiveTrue(brandName);
    }
}
