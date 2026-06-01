package com.qcg.service;

import com.qcg.common.BusinessException;
import com.qcg.dto.AppointmentRequest;
import com.qcg.dto.AppointmentResponse;
import com.qcg.entity.*;
import com.qcg.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepo;
    private final ShopRepository shopRepo;
    private final ColorRepository colorRepo;
    private final CarPhotoRepository carPhotoRepo;

    @Transactional
    public AppointmentResponse create(User user, AppointmentRequest req) {
        Shop shop = shopRepo.findById(req.getShopId())
                .orElseThrow(() -> new BusinessException("门店不存在"));

        Appointment apt = Appointment.builder()
                .user(user)
                .shop(shop)
                .appointmentTime(req.getAppointmentTime())
                .remark(req.getRemark())
                .build();

        if (req.getColorId() != null) {
            Color color = colorRepo.findById(req.getColorId())
                    .orElseThrow(() -> new BusinessException("颜色不存在"));
            apt.setColor(color);
        }
        if (req.getCarPhotoId() != null) {
            CarPhoto photo = carPhotoRepo.findById(req.getCarPhotoId())
                    .orElseThrow(() -> new BusinessException("效果图不存在"));
            apt.setCarPhoto(photo);
        }

        apt = appointmentRepo.save(apt);
        return toResponse(apt);
    }

    public List<AppointmentResponse> listByUser(User user) {
        return appointmentRepo.findByUserIdOrderByAppointmentTimeDesc(user.getId()).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<AppointmentResponse> listByShop(User user) {
        List<Shop> shops = shopRepo.findByOwnerId(user.getId());
        if (shops.isEmpty()) throw new BusinessException("您还未注册门店");
        return appointmentRepo.findByShopIdOrderByAppointmentTimeDesc(shops.get(0).getId()).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public AppointmentResponse updateStatus(User user, Long appointmentId, String newStatus) {
        Appointment apt = appointmentRepo.findById(appointmentId)
                .orElseThrow(() -> new BusinessException("预约不存在"));
        if (!apt.getShop().getOwner().getId().equals(user.getId())
                && !apt.getUser().getId().equals(user.getId())) {
            throw new BusinessException(403, "无权操作此预约");
        }
        apt.setStatus(newStatus);
        appointmentRepo.save(apt);
        return toResponse(apt);
    }

    private AppointmentResponse toResponse(Appointment a) {
        return AppointmentResponse.builder()
                .id(a.getId()).userId(a.getUser().getId()).userName(a.getUser().getNickname())
                .shopId(a.getShop().getId()).shopName(a.getShop().getName())
                .carPhotoId(a.getCarPhoto() != null ? a.getCarPhoto().getId() : null)
                .resultUrl(a.getCarPhoto() != null ? a.getCarPhoto().getResultUrl() : null)
                .colorId(a.getColor() != null ? a.getColor().getId() : null)
                .colorName(a.getColor() != null ? a.getColor().getName() : null)
                .colorHex(a.getColor() != null ? a.getColor().getHexCode() : null)
                .appointmentTime(a.getAppointmentTime()).status(a.getStatus())
                .remark(a.getRemark()).createdAt(a.getCreatedAt()).build();
    }
}
