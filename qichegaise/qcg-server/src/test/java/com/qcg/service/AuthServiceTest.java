package com.qcg.service;

import com.qcg.entity.User;
import com.qcg.repository.UserRepository;
import com.qcg.security.JwtUtil;
import com.qcg.client.WechatClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock JwtUtil jwtUtil;
    @Mock WechatClient wechatClient;

    @InjectMocks
    AuthService authService;

    @BeforeEach
    void setUp() {
        when(jwtUtil.generateToken(any())).thenReturn("test-token");
    }

    @Test
    void shouldLoginExistingUser() {
        User existing = User.builder().id(1L).openid("openid123").nickname("老用户").build();
        when(wechatClient.getOpenid(anyString())).thenReturn("openid123");
        when(userRepository.findByOpenid("openid123")).thenReturn(Optional.of(existing));

        var result = authService.login("test-code");

        assertThat(result.getToken()).isEqualTo("test-token");
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getNickname()).isEqualTo("老用户");
    }

    @Test
    void shouldCreateNewUserOnFirstLogin() {
        when(wechatClient.getOpenid(anyString())).thenReturn("openid999");
        when(userRepository.findByOpenid("openid999")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });

        var result = authService.login("test-code");

        assertThat(result.getToken()).isEqualTo("test-token");
        assertThat(result.getUserId()).isEqualTo(2L);
    }
}
