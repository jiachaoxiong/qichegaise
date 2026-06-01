package com.qcg.controller;

import com.qcg.common.Result;
import com.qcg.entity.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public Result<?> me(@AuthenticationPrincipal User user) {
        return Result.ok(Map.of(
            "id", user.getId(),
            "nickname", user.getNickname(),
            "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
            "role", user.getRole().name()
        ));
    }
}
