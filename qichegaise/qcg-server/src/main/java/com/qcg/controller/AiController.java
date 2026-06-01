package com.qcg.controller;

import com.qcg.common.Result;
import com.qcg.dto.ColorizeRequest;
import com.qcg.entity.User;
import com.qcg.service.AiColorizeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiColorizeService aiColorizeService;

    @PostMapping("/colorize")
    public Result<?> colorize(@AuthenticationPrincipal User user,
                              @RequestBody ColorizeRequest request) {
        // 直接通过service调用，异常由GlobalExceptionHandler处理
        var result = aiColorizeService.submit(user, request.getPhotoId(), request.getColorId());
        return Result.ok(result);
    }

    @GetMapping("/tasks/{photoId}")
    public Result<?> pollTask(@AuthenticationPrincipal User user,
                              @PathVariable Long photoId) {
        return Result.ok(aiColorizeService.poll(photoId));
    }
}
