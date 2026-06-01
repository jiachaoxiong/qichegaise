package com.qcg.service;

import com.qcg.entity.Shop;
import com.qcg.entity.User;
import com.qcg.enums.UserRole;
import com.qcg.repository.ShopRepository;
import com.qcg.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShopServiceTest {

    @Mock ShopRepository shopRepo;
    @Mock UserRepository userRepo;
    @InjectMocks ShopService shopService;

    @Test
    void shouldRegisterShopAndUpgradeUserRole() {
        User user = User.builder().id(1L).role(UserRole.USER).build();
        Shop shop = Shop.builder().id(10L).name("测试门店").status("PENDING").owner(user).build();

        when(shopRepo.findByOwnerId(1L)).thenReturn(List.of());
        when(shopRepo.save(any(Shop.class))).thenReturn(shop);

        var result = shopService.register(user, "测试门店", "北京", "138", "desc");

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void shouldListApprovedShops() {
        when(shopRepo.findByStatusOrderByIdDesc("APPROVED"))
                .thenReturn(List.of(Shop.builder().id(1L).name("A店").build()));

        var shops = shopService.listApproved();

        assertThat(shops).hasSize(1);
        assertThat(shops.get(0).getName()).isEqualTo("A店");
    }
}
