package com.qcg.controller;

import com.qcg.common.Result;
import com.qcg.service.ColorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/colors")
@RequiredArgsConstructor
public class ColorController {

    private final ColorService colorService;

    @GetMapping
    public Result<?> list(@RequestParam(required = false) String category) {
        if (category != null) {
            return Result.ok(colorService.listByCategory(category));
        }
        return Result.ok(colorService.listAll());
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.ok(colorService.getById(id));
    }
}
