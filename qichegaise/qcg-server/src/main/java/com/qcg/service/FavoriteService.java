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

    public List<Shop> listFavorites(User user) {
        return favoriteRepo.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(Favorite::getShop)
                .collect(Collectors.toList());
    }
}
