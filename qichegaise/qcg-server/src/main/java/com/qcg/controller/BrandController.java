package com.qcg.controller;

import com.qcg.common.Result;
import com.qcg.repository.BrandRepository;
import com.qcg.repository.ColorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandRepository brandRepo;
    private final ColorRepository colorRepo;

    @GetMapping
    public Result<?> list() {
        return Result.ok(brandRepo.findAll());
    }

    @GetMapping("/{id}/colors")
    public Result<?> colors(@PathVariable Long id) {
        return Result.ok(colorRepo.findByBrandIdAndIsActiveTrue(id));
    }
}
