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

    public ShopResponse getDetail(Long shopId) {
        Shop shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new BusinessException("门店不存在"));
        return toResponse(shop, false);
    }

    public List<ShopResponse> listMyShop(User user) {
        return shopRepo.findByOwnerId(user.getId()).stream()
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
                .caseCount(0)
                .build();
    }
}
