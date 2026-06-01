package com.qcg.controller;

import com.qcg.common.Result;
import com.qcg.service.CarModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/car-models")
@RequiredArgsConstructor
public class CarModelController {

    private final CarModelService carModelService;

    @GetMapping
    public Result<?> list(@RequestParam(required = false) String brand) {
        if (brand != null) {
            return Result.ok(carModelService.listByBrand(brand));
        }
        return Result.ok(carModelService.listAll());
    }
}
