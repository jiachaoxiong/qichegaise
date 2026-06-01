# 汽车改色膜换色系统 Phase 2 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现门店生态——门店入驻、案例展示、品牌色卡、车主浏览和收藏。

**Architecture:** 复用 Phase 1 的认证体系（同一 user 表，role=SHOP），新增门店/案例/收藏的 Service + Controller 层。数据库 Shop/ShopCase 表已建，新增 favorite 关联表。小程序新增门店列表页和门店详情页。

**Tech Stack:** Java 17+, Spring Boot 3.2.5, JPA/Hibernate, MySQL 8.0（与 Phase 1 一致）

---

## 文件变动概览

```
qichegaise/qcg-server/
├── src/main/java/com/qcg/
│   ├── entity/                        (Shop.java, ShopCase.java ✓已存在)
│   ├── repository/
│   │   ├── ShopRepository.java        (新增)
│   │   ├── ShopCaseRepository.java    (新增)
│   │   └── FavoriteRepository.java    (新增)
│   ├── dto/
│   │   ├── ShopRegisterRequest.java   (新增)
│   │   ├── ShopResponse.java          (新增)
│   │   ├── ShopCaseRequest.java       (新增)
│   │   └── ShopCaseResponse.java      (新增)
│   ├── service/
│   │   ├── ShopService.java           (新增)
│   │   ├── ShopCaseService.java       (新增)
│   │   └── FavoriteService.java       (新增)
│   ├── controller/
│   │   ├── ShopController.java        (新增)
│   │   ├── ShopCaseController.java    (新增)
│   │   └── BrandController.java       (新增)
│   └── entity/
│       └── Favorite.java              (新增)
├── src/main/resources/
│   └── schema.sql                     (修改: 添加 favorite 表)
└── src/test/java/com/qcg/
    └── service/
        └── ShopServiceTest.java       (新增)

qichegaise/qcg-miniapp/
├── app.json                           (修改: 添加门店 tab)
├── pages/
│   ├── shop-list/                     (新增: 门店列表)
│   │   ├── shop-list.js
│   │   ├── shop-list.wxml
│   │   ├── shop-list.wxss
│   │   └── shop-list.json
│   └── shop-detail/                   (新增: 门店详情)
│       ├── shop-detail.js
│       ├── shop-detail.wxml
│       ├── shop-detail.wxss
│       └── shop-detail.json
```

---

### Task 1: 门店/案例/收藏 Repository + Favorite 实体

**Files:**
- Create: `qichegaise/qcg-server/src/main/java/com/qcg/repository/ShopRepository.java`
- Create: `qichegaise/qcg-server/src/main/java/com/qcg/repository/ShopCaseRepository.java`
- Create: `qichegaise/qcg-server/src/main/java/com/qcg/repository/FavoriteRepository.java`
- Create: `qichegaise/qcg-server/src/main/java/com/qcg/entity/Favorite.java`
- Modify: `qichegaise/qcg-server/src/main/resources/schema.sql`

- [ ] **Step 1: 创建 Favorite 实体**

```java
package com.qcg.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "favorite", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "shop_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Favorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

- [ ] **Step 2: 创建 3 个 Repository**

```java
package com.qcg.repository;

import com.qcg.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ShopRepository extends JpaRepository<Shop, Long> {
    List<Shop> findByStatusOrderByIdDesc(String status);
    List<Shop> findByOwnerId(Long ownerId);
}
```

```java
package com.qcg.repository;

import com.qcg.entity.ShopCase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ShopCaseRepository extends JpaRepository<ShopCase, Long> {
    List<ShopCase> findByShopIdOrderByIdDesc(Long shopId);
    List<ShopCase> findByShopIdInOrderByIdDesc(List<Long> shopIds);
}
```

```java
package com.qcg.repository;

import com.qcg.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Favorite> findByUserIdAndShopId(Long userId, Long shopId);
    boolean existsByUserIdAndShopId(Long userId, Long shopId);
    void deleteByUserIdAndShopId(Long userId, Long shopId);
}
```

- [ ] **Step 3: 在 schema.sql 末尾添加 favorite 表**

```sql
-- 收藏表 (Phase 2)
CREATE TABLE IF NOT EXISTS favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    shop_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_shop (user_id, shop_id),
    FOREIGN KEY (user_id) REFERENCES `user`(id),
    FOREIGN KEY (shop_id) REFERENCES shop(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 4: 编译验证 + 提交**

```bash
cd qichegaise/qcg-server && mvn compile
git add qichegaise/qcg-server/src/main/java/com/qcg/repository/ShopRepository.java qichegaise/qcg-server/src/main/java/com/qcg/repository/ShopCaseRepository.java qichegaise/qcg-server/src/main/java/com/qcg/repository/FavoriteRepository.java qichegaise/qcg-server/src/main/java/com/qcg/entity/Favorite.java qichegaise/qcg-server/src/main/resources/schema.sql
git commit -m "feat: Phase2 — Repository层 + Favorite实体 + schema更新"
```

---

### Task 2: DTO 类（门店 + 案例）

**Files:**
- Create: `qichegaise/qcg-server/src/main/java/com/qcg/dto/ShopRegisterRequest.java`
- Create: `qichegaise/qcg-server/src/main/java/com/qcg/dto/ShopResponse.java`
- Create: `qichegaise/qcg-server/src/main/java/com/qcg/dto/ShopCaseRequest.java`
- Create: `qichegaise/qcg-server/src/main/java/com/qcg/dto/ShopCaseResponse.java`

- [ ] **Step 1: 创建 ShopRegisterRequest**

```java
package com.qcg.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShopRegisterRequest {
    @NotBlank(message = "门店名称不能为空")
    private String name;

    private String address;
    private String phone;
    private String description;
}
```

- [ ] **Step 2: 创建 ShopResponse**

```java
package com.qcg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopResponse {
    private Long id;
    private String name;
    private String address;
    private BigDecimal lat;
    private BigDecimal lng;
    private String phone;
    private String coverUrl;
    private String description;
    private String status;
    private Long ownerId;
    private String ownerName;
    private int caseCount;
    private boolean isFavorited;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 3: 创建 ShopCaseRequest**

```java
package com.qcg.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShopCaseRequest {
    @NotBlank(message = "施工前照片不能为空")
    private String beforeUrl;

    @NotBlank(message = "施工后照片不能为空")
    private String afterUrl;

    private Long colorId;
    private String carModelName;
    private String description;
}
```

- [ ] **Step 4: 创建 ShopCaseResponse**

```java
package com.qcg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopCaseResponse {
    private Long id;
    private Long shopId;
    private String shopName;
    private String beforeUrl;
    private String afterUrl;
    private Long colorId;
    private String colorName;
    private String colorHex;
    private String carModelName;
    private String description;
    private int likes;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 5: 编译 + 提交**

```bash
cd qichegaise/qcg-server && mvn compile
git add qichegaise/qcg-server/src/main/java/com/qcg/dto/
git commit -m "feat: Phase2 — 门店/案例 DTO"
```

---

### Task 3: ShopService + ShopController（门店入驻/查询）

**Files:**
- Create: `qichegaise/qcg-server/src/main/java/com/qcg/service/ShopService.java`
- Create: `qichegaise/qcg-server/src/main/java/com/qcg/controller/ShopController.java`
- Create: `qichegaise/qcg-server/src/test/java/com/qcg/service/ShopServiceTest.java`

- [ ] **Step 1: TDD — 写测试**

```java
package com.qcg.service;

import com.qcg.entity.Shop;
import com.qcg.entity.User;
import com.qcg.enums.UserRole;
import com.qcg.repository.ShopRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShopServiceTest {

    @Mock ShopRepository shopRepo;
    @InjectMocks ShopService shopService;

    @Test
    void shouldRegisterShopAndSetOwnerRole() {
        User user = User.builder().id(1L).role(UserRole.USER).build();
        Shop shop = Shop.builder().id(10L).name("测试门店").status("PENDING").build();

        when(shopRepo.save(any(Shop.class))).thenReturn(shop);

        var result = shopService.register(user, "测试门店", "北京", "13800138000", "desc");

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void shouldListApprovedShops() {
        when(shopRepo.findByStatusOrderByIdDesc("APPROVED"))
                .thenReturn(java.util.List.of(Shop.builder().id(1L).name("A店").build()));

        var shops = shopService.listApproved();

        assertThat(shops).hasSize(1);
    }
}
```

- [ ] **Step 2: 验证测试失败**

```bash
cd qichegaise/qcg-server && mvn test -Dtest=ShopServiceTest
```
预期：编译失败

- [ ] **Step 3: 创建 ShopService**

```java
package com.qcg.service;

import com.qcg.common.BusinessException;
import com.qcg.dto.ShopResponse;
import com.qcg.entity.Shop;
import com.qcg.entity.User;
import com.qcg.enums.UserRole;
import com.qcg.repository.ShopRepository;
import com.qcg.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopRepository shopRepo;
    private final UserRepository userRepo;

    @Transactional
    public ShopResponse register(User user, String name, String address, String phone, String description) {
        // 一个用户只能开一家门店
        if (!shopRepo.findByOwnerId(user.getId()).isEmpty()) {
            throw new BusinessException("您已注册过门店");
        }

        Shop shop = Shop.builder()
                .name(name)
                .address(address)
                .phone(phone)
                .description(description)
                .status("PENDING")
                .owner(user)
                .build();
        shop = shopRepo.save(shop);

        // 升级用户角色为 SHOP
        if (user.getRole() == UserRole.USER) {
            user.setRole(UserRole.SHOP);
            userRepo.save(user);
        }

        return toResponse(shop, false);
    }

    public List<ShopResponse> listApproved() {
        return shopRepo.findByStatusOrderByIdDesc("APPROVED").stream()
                .map(s -> toResponse(s, false))
                .collect(Collectors.toList());
    }

    public ShopResponse getDetail(Long shopId, Long currentUserId) {
        Shop shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new BusinessException("门店不存在"));
        boolean faved = false; // 由 Controller 层查询
        return toResponse(shop, faved);
    }

    public List<ShopResponse> listMyShop(User user) {
        List<Shop> shops = shopRepo.findByOwnerId(user.getId());
        return shops.stream()
                .map(s -> toResponse(s, false))
                .collect(Collectors.toList());
    }

    private ShopResponse toResponse(Shop shop, boolean isFavorited) {
        return ShopResponse.builder()
                .id(shop.getId())
                .name(shop.getName())
                .address(shop.getAddress())
                .lat(shop.getLat())
                .lng(shop.getLng())
                .phone(shop.getPhone())
                .coverUrl(shop.getCoverUrl())
                .description(shop.getDescription())
                .status(shop.getStatus())
                .ownerId(shop.getOwner() != null ? shop.getOwner().getId() : null)
                .ownerName(shop.getOwner() != null ? shop.getOwner().getNickname() : null)
                .isFavorited(isFavorited)
                .build();
    }
}
```

- [ ] **Step 4: 创建 ShopController**

```java
package com.qcg.controller;

import com.qcg.common.Result;
import com.qcg.dto.ShopRegisterRequest;
import com.qcg.entity.User;
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

    @GetMapping("/{id}")
    public Result<?> detail(@AuthenticationPrincipal User user,
                            @PathVariable Long id) {
        return Result.ok(shopService.getDetail(id, user.getId()));
    }
}
```

- [ ] **Step 5: 运行测试验证**

```bash
cd qichegaise/qcg-server && mvn test -Dtest=ShopServiceTest
```
预期：2 tests PASS

- [ ] **Step 6: 提交**

```bash
git add qichegaise/qcg-server/src/main/java/com/qcg/service/ShopService.java qichegaise/qcg-server/src/main/java/com/qcg/controller/ShopController.java qichegaise/qcg-server/src/test/java/com/qcg/service/ShopServiceTest.java
git commit -m "feat: Phase2 — 门店注册/查询（含单测）"
```

---

### Task 4: ShopCaseService + ShopCaseController（案例管理）

**Files:**
- Create: `qichegaise/qcg-server/src/main/java/com/qcg/service/ShopCaseService.java`
- Create: `qichegaise/qcg-server/src/main/java/com/qcg/controller/ShopCaseController.java`

- [ ] **Step 1: 创建 ShopCaseService**

```java
package com.qcg.service;

import com.qcg.common.BusinessException;
import com.qcg.dto.ShopCaseRequest;
import com.qcg.dto.ShopCaseResponse;
import com.qcg.entity.*;
import com.qcg.repository.ColorRepository;
import com.qcg.repository.ShopCaseRepository;
import com.qcg.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShopCaseService {

    private final ShopCaseRepository caseRepo;
    private final ShopRepository shopRepo;
    private final ColorRepository colorRepo;

    @Transactional
    public ShopCaseResponse create(User user, ShopCaseRequest req) {
        // 查找用户的店铺
        List<Shop> shops = shopRepo.findByOwnerId(user.getId());
        if (shops.isEmpty()) {
            throw new BusinessException("请先注册门店");
        }
        Shop shop = shops.get(0);

        ShopCase shopCase = ShopCase.builder()
                .shop(shop)
                .beforeUrl(req.getBeforeUrl())
                .afterUrl(req.getAfterUrl())
                .carModelName(req.getCarModelName())
                .description(req.getDescription())
                .build();

        if (req.getColorId() != null) {
            Color color = colorRepo.findById(req.getColorId())
                    .orElseThrow(() -> new BusinessException("颜色不存在"));
            shopCase.setColor(color);
        }

        shopCase = caseRepo.save(shopCase);
        return toResponse(shopCase);
    }

    public List<ShopCaseResponse> listByShop(Long shopId) {
        return caseRepo.findByShopIdOrderByIdDesc(shopId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public void delete(User user, Long caseId) {
        ShopCase shopCase = caseRepo.findById(caseId)
                .orElseThrow(() -> new BusinessException("案例不存在"));

        if (!shopCase.getShop().getOwner().getId().equals(user.getId())) {
            throw new BusinessException(403, "无权删除此案例");
        }

        caseRepo.delete(shopCase);
    }

    public ShopCaseResponse toResponse(ShopCase c) {
        return ShopCaseResponse.builder()
                .id(c.getId())
                .shopId(c.getShop().getId())
                .shopName(c.getShop().getName())
                .beforeUrl(c.getBeforeUrl())
                .afterUrl(c.getAfterUrl())
                .colorId(c.getColor() != null ? c.getColor().getId() : null)
                .colorName(c.getColor() != null ? c.getColor().getName() : null)
                .colorHex(c.getColor() != null ? c.getColor().getHexCode() : null)
                .carModelName(c.getCarModelName())
                .description(c.getDescription())
                .likes(c.getLikes())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
```

- [ ] **Step 2: 创建 ShopCaseController**

```java
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
```

- [ ] **Step 3: 编译 + 提交**

```bash
cd qichegaise/qcg-server && mvn compile
git add qichegaise/qcg-server/src/main/java/com/qcg/service/ShopCaseService.java qichegaise/qcg-server/src/main/java/com/qcg/controller/ShopCaseController.java
git commit -m "feat: Phase2 — 案例上传/查询/删除"
```

---

### Task 5: FavoriteService + 收藏 API

**Files:**
- Create: `qichegaise/qcg-server/src/main/java/com/qcg/service/FavoriteService.java`
- Modify: `qichegaise/qcg-server/src/main/java/com/qcg/controller/ShopController.java`

- [ ] **Step 1: 创建 FavoriteService**

```java
package com.qcg.service;

import com.qcg.common.BusinessException;
import com.qcg.entity.Favorite;
import com.qcg.entity.Shop;
import com.qcg.entity.User;
import com.qcg.repository.FavoriteRepository;
import com.qcg.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepo;
    private final ShopRepository shopRepo;

    @Transactional
    public void add(User user, Long shopId) {
        if (favoriteRepo.existsByUserIdAndShopId(user.getId(), shopId)) {
            throw new BusinessException("已收藏过该门店");
        }
        Shop shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new BusinessException("门店不存在"));

        Favorite fav = Favorite.builder()
                .user(user)
                .shop(shop)
                .build();
        favoriteRepo.save(fav);
    }

    @Transactional
    public void remove(User user, Long shopId) {
        favoriteRepo.deleteByUserIdAndShopId(user.getId(), shopId);
    }

    public List<Long> getFavoritedShopIds(Long userId) {
        return favoriteRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(f -> f.getShop().getId())
                .collect(Collectors.toList());
    }

    public List<Shop> listFavorites(User user) {
        return favoriteRepo.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(Favorite::getShop)
                .collect(Collectors.toList());
    }
}
```

- [ ] **Step 2: 在 ShopController 添加收藏端点**

```java
// 在 ShopController 中添加以下注入和方法：

private final FavoriteService favoriteService; // 更新构造函数

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

@GetMapping("/favorites")
public Result<?> myFavorites(@AuthenticationPrincipal User user) {
    return Result.ok(favoriteService.listFavorites(user));
}
```

- [ ] **Step 3: 编译 + 提交**

```bash
cd qichegaise/qcg-server && mvn compile
git add qichegaise/qcg-server/src/main/java/com/qcg/service/FavoriteService.java qichegaise/qcg-server/src/main/java/com/qcg/controller/ShopController.java
git commit -m "feat: Phase2 — 门店收藏功能"
```

---

### Task 6: BrandController + 品牌色卡 API

**Files:**
- Create: `qichegaise/qcg-server/src/main/java/com/qcg/controller/BrandController.java`

- [ ] **Step 1: 创建 BrandController**

```java
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
```

- [ ] **Step 2: ColorRepository 补充方法**

使用 Edit 工具，在 `ColorRepository.java` 中添加：

```java
List<Color> findByBrandIdAndIsActiveTrue(Long brandId);
```

- [ ] **Step 3: 编译 + 提交**

```bash
cd qichegaise/qcg-server && mvn compile
git add qichegaise/qcg-server/src/main/java/com/qcg/controller/BrandController.java qichegaise/qcg-server/src/main/java/com/qcg/repository/ColorRepository.java
git commit -m "feat: Phase2 — 品牌色卡 API"
```

---

### Task 7: 小程序 — 门店列表页

**Files:**
- Create: `qichegaise/qcg-miniapp/pages/shop-list/shop-list.json`
- Create: `qichegaise/qcg-miniapp/pages/shop-list/shop-list.wxml`
- Create: `qichegaise/qcg-miniapp/pages/shop-list/shop-list.js`
- Create: `qichegaise/qcg-miniapp/pages/shop-list/shop-list.wxss`
- Modify: `qichegaise/qcg-miniapp/app.json`

- [ ] **Step 1: shop-list.json**

```json
{
  "navigationBarTitleText": "贴膜门店",
  "usingComponents": {}
}
```

- [ ] **Step 2: shop-list.wxml**

```html
<view class="container">
  <view class="shop-card card" wx:for="{{shops}}" wx:key="id"
        bindtap="onTapShop" data-id="{{item.id}}">
    <image class="shop-cover" src="{{item.coverUrl}}" mode="aspectFill"
           wx:if="{{item.coverUrl}}"/>
    <view class="shop-cover placeholder" wx:else>
      <text>暂无封面</text>
    </view>
    <view class="shop-info">
      <text class="shop-name">{{item.name}}</text>
      <text class="shop-addr">{{item.address || '地址未填写'}}</text>
      <text class="shop-case-count">{{item.caseCount || 0}} 个案例</text>
    </view>
    <view class="shop-arrow">→</view>
  </view>
  <view class="empty" wx:if="{{shops.length === 0}}">
    <text>暂无门店入驻</text>
  </view>
</view>
```

- [ ] **Step 3: shop-list.js**

```javascript
const api = require('../../utils/api')

Page({
  data: { shops: [] },

  onShow() {
    api.get('/api/shops').then(shops => {
      this.setData({ shops: shops || [] })
    }).catch(() => {})
  },

  onTapShop(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/shop-detail/shop-detail?id=' + id })
  }
})
```

- [ ] **Step 4: shop-list.wxss**

```css
.shop-card {
  display: flex;
  align-items: center;
  padding: 24rpx;
  margin-bottom: 16rpx;
}
.shop-cover {
  width: 120rpx;
  height: 120rpx;
  border-radius: 12rpx;
  margin-right: 20rpx;
  background: #eee;
}
.shop-cover.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  color: #999;
}
.shop-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.shop-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
}
.shop-addr {
  font-size: 24rpx;
  color: #999;
  margin-top: 8rpx;
}
.shop-case-count {
  font-size: 22rpx;
  color: #ff4444;
  margin-top: 4rpx;
}
.shop-arrow {
  font-size: 28rpx;
  color: #ccc;
}
```

- [ ] **Step 5: 更新 app.json — 在 tabBar.list 中追加**

在 tabBar.list 数组中添加：
```json
{
  "pagePath": "pages/shop-list/shop-list",
  "text": "门店",
  "iconPath": "images/shop.png",
  "selectedIconPath": "images/shop-active.png"
}
```

同时在 pages 数组中添加：`"pages/shop-list/shop-list"` 和 `"pages/shop-detail/shop-detail"`

- [ ] **Step 6: 提交**

```bash
git add qichegaise/qcg-miniapp/pages/shop-list/ qichegaise/qcg-miniapp/app.json
git commit -m "feat: Phase2 — 小程序门店列表页"
```

---

### Task 8: 小程序 — 门店详情页

**Files:**
- Create: `qichegaise/qcg-miniapp/pages/shop-detail/shop-detail.json`
- Create: `qichegaise/qcg-miniapp/pages/shop-detail/shop-detail.wxml`
- Create: `qichegaise/qcg-miniapp/pages/shop-detail/shop-detail.js`
- Create: `qichegaise/qcg-miniapp/pages/shop-detail/shop-detail.wxss`

- [ ] **Step 1: shop-detail.json**

```json
{
  "navigationBarTitleText": "门店详情",
  "usingComponents": {}
}
```

- [ ] **Step 2: shop-detail.wxml**

```html
<view class="container">
  <!-- 门店信息 -->
  <view class="shop-header card">
    <image class="shop-cover-big" src="{{shop.coverUrl}}" mode="aspectFill"
           wx:if="{{shop.coverUrl}}"/>
    <view class="shop-cover-big placeholder" wx:else>
      <text>暂无封面</text>
    </view>
    <view class="shop-meta">
      <text class="shop-name">{{shop.name}}</text>
      <text class="shop-addr">📍 {{shop.address || '地址未填写'}}</text>
      <text class="shop-phone" wx:if="{{shop.phone}}">📞 {{shop.phone}}</text>
      <text class="shop-desc" wx:if="{{shop.description}}">{{shop.description}}</text>
    </view>
    <view class="fav-btn {{isFavorited ? 'faved' : ''}}" bindtap="onToggleFavorite">
      {{isFavorited ? '❤️ 已收藏' : '🤍 收藏门店'}}
    </view>
  </view>

  <!-- 施工案例 -->
  <view class="section-title">施工案例</view>
  <view class="case-card card" wx:for="{{cases}}" wx:key="id">
    <view class="case-compare">
      <view class="case-img-wrap">
        <image class="case-img" src="{{item.beforeUrl}}" mode="aspectFit"/>
        <text class="case-label">施工前</text>
      </view>
      <text class="case-arrow">→</text>
      <view class="case-img-wrap">
        <image class="case-img" src="{{item.afterUrl}}" mode="aspectFit"/>
        <text class="case-label">施工后</text>
      </view>
    </view>
    <view class="case-info" wx:if="{{item.carModelName || item.colorName}}">
      <text wx:if="{{item.carModelName}}">{{item.carModelName}}</text>
      <text wx:if="{{item.colorName}}"> · {{item.colorName}}</text>
    </view>
  </view>

  <view class="empty" wx:if="{{cases.length === 0}}">
    <text>暂无施工案例</text>
  </view>
</view>
```

- [ ] **Step 3: shop-detail.js**

```javascript
const api = require('../../utils/api')

Page({
  data: {
    shopId: null,
    shop: {},
    cases: [],
    isFavorited: false
  },

  onLoad(options) {
    const shopId = options.id
    this.setData({ shopId })
    this.loadDetail()
    this.loadCases()
    this.checkFavorite()
  },

  loadDetail() {
    api.get('/api/shops/' + this.data.shopId).then(shop => {
      this.setData({ shop })
    })
  },

  loadCases() {
    api.get('/api/shop-cases/shop/' + this.data.shopId).then(cases => {
      this.setData({ cases: cases || [] })
    })
  },

  checkFavorite() {
    api.get('/api/shops/favorites').then(shops => {
      const faved = (shops || []).some(s => s.id == this.data.shopId)
      this.setData({ isFavorited: faved })
    }).catch(() => {})
  },

  onToggleFavorite() {
    const { shopId, isFavorited } = this.data
    const method = isFavorited ? 'delete' : 'post'
    api[method]('/api/shops/' + shopId + '/favorite').then(() => {
      this.setData({ isFavorited: !isFavorited })
      wx.showToast({
        title: isFavorited ? '已取消收藏' : '已收藏',
        icon: 'none'
      })
    })
  }
})
```

- [ ] **Step 4: shop-detail.wxss**

```css
.shop-header {
  padding: 0;
  overflow: hidden;
}
.shop-cover-big {
  width: 100%;
  height: 360rpx;
}
.shop-cover-big.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 28rpx;
  background: #eee;
}
.shop-meta {
  padding: 24rpx;
}
.shop-name {
  font-size: 36rpx;
  font-weight: 700;
  color: #1a1a1a;
}
.shop-addr, .shop-phone, .shop-desc {
  display: block;
  font-size: 26rpx;
  color: #666;
  margin-top: 10rpx;
}
.fav-btn {
  padding: 20rpx 24rpx;
  text-align: center;
  font-size: 28rpx;
  border-top: 1px solid #f0f0f0;
  color: #666;
}
.fav-btn.faved {
  color: #ff4444;
}
.section-title {
  font-size: 32rpx;
  font-weight: 600;
  padding: 32rpx 24rpx 16rpx;
  color: #333;
}
.case-compare {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.case-img-wrap {
  flex: 1;
  text-align: center;
}
.case-img {
  width: 100%;
  height: 240rpx;
  border-radius: 8rpx;
  background: #f0f0f0;
}
.case-label {
  display: block;
  font-size: 22rpx;
  color: #999;
  margin-top: 8rpx;
}
.case-arrow {
  font-size: 28rpx;
  color: #ccc;
}
.case-info {
  padding-top: 12rpx;
  font-size: 24rpx;
  color: #666;
}
```

- [ ] **Step 5: 提交**

```bash
git add qichegaise/qcg-miniapp/pages/shop-detail/
git commit -m "feat: Phase2 — 小程序门店详情页"
```

---

### Task 9: Phase 2 集成验证

**Files:**
- Modify: `qichegaise/qcg-server/src/test/java/com/qcg/controller/AuthControllerTest.java` （无需改动，确认全部测试通过即可）

- [ ] **Step 1: 运行全部测试**

```bash
cd qichegaise/qcg-server && mvn test
```
预期：全部 PASS（7 个 Phase 1 测试 + 2 个 Phase 2 测试 = 9 tests）

- [ ] **Step 2: 编译全部后端代码**

```bash
cd qichegaise/qcg-server && mvn compile
```
预期：BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git commit -m "test: Phase2 集成验证通过" --allow-empty
```

---

## Phase 2 API 汇总

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/shops/register | 门店入驻 |
| GET | /api/shops | 已审核门店列表 |
| GET | /api/shops/my | 我的门店 |
| GET | /api/shops/:id | 门店详情 |
| POST | /api/shops/:id/favorite | 收藏门店 |
| DELETE | /api/shops/:id/favorite | 取消收藏 |
| GET | /api/shops/favorites | 我的收藏 |
| POST | /api/shop-cases | 上传案例 |
| GET | /api/shop-cases/shop/:shopId | 门店案例列表 |
| DELETE | /api/shop-cases/:id | 删除案例 |
| GET | /api/brands | 品牌列表 |
| GET | /api/brands/:id/colors | 品牌色卡 |

---

> **下一步**：用户审阅后，选择执行模式。
