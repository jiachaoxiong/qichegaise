package com.qcg.controller;

import com.qcg.common.Result;
import com.qcg.entity.Color;
import com.qcg.entity.Shop;
import com.qcg.entity.User;
import com.qcg.enums.UserRole;
import com.qcg.repository.*;
import com.qcg.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepo;
    private final ShopRepository shopRepo;
    private final AppointmentRepository appointmentRepo;
    private final CarPhotoRepository carPhotoRepo;
    private final ColorRepository colorRepo;

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:admin123}")
    private String adminPassword;

    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (!adminUsername.equals(username) || !adminPassword.equals(password)) {
            return Result.fail(401, "用户名或密码错误");
        }
        User admin = userRepo.findByOpenid("admin")
                .orElseGet(() -> userRepo.save(User.builder()
                        .openid("admin").nickname("管理员").role(UserRole.ADMIN).build()));
        String token = jwtUtil.generateToken(admin.getId());
        return Result.ok(Map.of("token", token));
    }

    @GetMapping("/dashboard")
    public Result<?> dashboard() {
        long userCount = userRepo.count();
        long shopCount = shopRepo.count();
        long pendingShopCount = shopRepo.findByStatusOrderByIdDesc("PENDING").size();
        long appointmentCount = appointmentRepo.count();
        long photoCount = carPhotoRepo.count();
        return Result.ok(Map.of(
            "userCount", userCount, "shopCount", shopCount,
            "pendingShopCount", pendingShopCount,
            "appointmentCount", appointmentCount, "photoCount", photoCount
        ));
    }

    @PutMapping("/shops/{id}/audit")
    public Result<?> auditShop(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Shop shop = shopRepo.findById(id).orElseThrow(() -> new RuntimeException("门店不存在"));
        shop.setStatus(body.get("status"));
        shopRepo.save(shop);
        return Result.ok();
    }

    @PostMapping("/colors")
    public Result<?> addColor(@RequestBody Map<String, String> body) {
        Color color = Color.builder()
                .name(body.get("name")).hexCode(body.get("hexCode"))
                .category(body.get("category")).build();
        colorRepo.save(color);
        return Result.ok();
    }

    @DeleteMapping("/colors/{id}")
    public Result<?> deleteColor(@PathVariable Long id) {
        colorRepo.deleteById(id);
        return Result.ok();
    }

    @GetMapping("/users")
    public Result<?> listUsers() {
        return Result.ok(userRepo.findAll());
    }

    @GetMapping("/appointments")
    public Result<?> listAppointments() {
        return Result.ok(appointmentRepo.findAll());
    }

    @GetMapping("/shops")
    public Result<?> listShops() {
        return Result.ok(shopRepo.findAll());
    }
}
