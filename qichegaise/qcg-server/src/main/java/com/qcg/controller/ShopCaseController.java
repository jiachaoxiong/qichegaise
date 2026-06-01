package com.qcg.controller;

import com.qcg.common.Result;
import com.qcg.dto.ShopCaseRequest;
import com.qcg.entity.User;
import com.qcg.service.ShopCaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop-cases")
@RequiredArgsConstructor
public class ShopCaseController {

    private final ShopCaseService shopCaseService;

    @PostMapping
    public Result<?> create(@AuthenticationPrincipal User user,
                            @Valid @RequestBody ShopCaseRequest req) {
        return Result.ok(shopCaseService.create(user, req));
    }

    @GetMapping("/shop/{shopId}")
    public Result<?> listByShop(@PathVariable Long shopId) {
        return Result.ok(shopCaseService.listByShop(shopId));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@AuthenticationPrincipal User user,
                            @PathVariable Long id) {
        shopCaseService.delete(user, id);
        return Result.ok();
    }
}
