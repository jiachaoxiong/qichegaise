package com.qcg.controller;

import com.qcg.common.Result;
import com.qcg.dto.LoginRequest;
import com.qcg.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request.getCode()));
    }
}
