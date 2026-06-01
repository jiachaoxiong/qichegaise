package com.qcg.controller;

import com.qcg.common.Result;
import com.qcg.entity.CarPhoto;
import com.qcg.entity.User;
import com.qcg.service.PhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;

    @PostMapping("/upload")
    public Result<?> upload(@AuthenticationPrincipal User user,
                            @RequestParam("file") MultipartFile file) {
        CarPhoto photo = photoService.upload(user, file);
        Map<String, Object> data = new HashMap<>();
        data.put("id", photo.getId());
        data.put("originalUrl", photo.getOriginalUrl());
        return Result.ok(data);
    }

    @PostMapping("/from-url")
    public Result<?> createFromUrl(@AuthenticationPrincipal User user,
                                   @RequestBody Map<String, String> body) {
        CarPhoto photo = photoService.createFromUrl(user, body.get("imageUrl"));
        Map<String, Object> data = new HashMap<>();
        data.put("id", photo.getId());
        data.put("originalUrl", photo.getOriginalUrl());
        return Result.ok(data);
    }

    @GetMapping
    public Result<?> list(@AuthenticationPrincipal User user) {
        return Result.ok(photoService.listByUser(user.getId()));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@AuthenticationPrincipal User user,
                            @PathVariable Long id) {
        photoService.delete(user, id);
        return Result.ok();
    }
}
