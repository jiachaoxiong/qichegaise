package com.qcg.controller;

import com.qcg.common.Result;
import com.qcg.dto.AppointmentRequest;
import com.qcg.entity.User;
import com.qcg.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    public Result<?> create(@AuthenticationPrincipal User user,
                            @Valid @RequestBody AppointmentRequest req) {
        return Result.ok(appointmentService.create(user, req));
    }

    @GetMapping("/my")
    public Result<?> myAppointments(@AuthenticationPrincipal User user) {
        return Result.ok(appointmentService.listByUser(user));
    }

    @GetMapping("/shop")
    public Result<?> shopAppointments(@AuthenticationPrincipal User user) {
        return Result.ok(appointmentService.listByShop(user));
    }

    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@AuthenticationPrincipal User user,
                                   @PathVariable Long id,
                                   @RequestBody Map<String, String> body) {
        return Result.ok(appointmentService.updateStatus(user, id, body.get("status")));
    }
}
