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
                .build();
    }
}
