package com.qcg.controller;

import com.qcg.common.Result;
import com.qcg.dto.ShopRegisterRequest;
import com.qcg.entity.User;
import com.qcg.service.FavoriteService;
import com.qcg.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;
    private final FavoriteService favoriteService;

    @PostMapping("/register")
    public Result<?> register(@AuthenticationPrincipal User user,
                              @Valid @RequestBody ShopRegisterRequest req) {
        return Result.ok(shopService.register(user,
                req.getName(), req.getAddress(), req.getPhone(), req.getDescription()));
    }

    @GetMapping
    public Result<?> list() {
        return Result.ok(shopService.listApproved());
    }

    @GetMapping("/my")
    public Result<?> myShop(@AuthenticationPrincipal User user) {
        return Result.ok(shopService.listMyShop(user));
    }

    @GetMapping("/favorites")
    public Result<?> myFavorites(@AuthenticationPrincipal User user) {
        return Result.ok(favoriteService.listFavorites(user));
    }

    @PostMapping("/{id}/favorite")
    public Result<?> addFavorite(@AuthenticationPrincipal User user,
                                  @PathVariable Long id) {
        favoriteService.add(user, id);
        return Result.ok();
    }

    @DeleteMapping("/{id}/favorite")
    public Result<?> removeFavorite(@AuthenticationPrincipal User user,
                                     @PathVariable Long id) {
        favoriteService.remove(user, id);
        return Result.ok();
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.ok(shopService.getDetail(id));
    }
}
