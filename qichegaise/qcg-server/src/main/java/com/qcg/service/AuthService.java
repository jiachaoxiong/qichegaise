package com.qcg.service;

import com.qcg.client.WechatClient;
import com.qcg.dto.LoginResponse;
import com.qcg.entity.User;
import com.qcg.repository.UserRepository;
import com.qcg.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final WechatClient wechatClient;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public LoginResponse login(String code) {
        String openid = wechatClient.getOpenid(code);

        User user = userRepository.findByOpenid(openid)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .openid(openid)
                            .build();
                    return userRepository.save(newUser);
                });

        String token = jwtUtil.generateToken(user.getId());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
