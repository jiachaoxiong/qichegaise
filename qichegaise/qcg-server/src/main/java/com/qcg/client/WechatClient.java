package com.qcg.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
public class WechatClient {

    private final String appId;
    private final String appSecret;
    private final RestTemplate restTemplate;

    public WechatClient(@Value("${wechat.app-id}") String appId,
                        @Value("${wechat.app-secret}") String appSecret) {
        this.appId = appId;
        this.appSecret = appSecret;
        this.restTemplate = new RestTemplate();
    }

    /**
     * 通过 code 换取 openid。
     * 微信 API: GET https://api.weixin.qq.com/sns/jscode2session
     */
    public String getOpenid(String code) {
        String url = String.format(
            "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
            appId, appSecret, code
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp != null && resp.containsKey("openid")) {
                return (String) resp.get("openid");
            }
            log.error("微信登录失败: {}", resp);
            throw new RuntimeException("微信登录失败: " + resp.getOrDefault("errmsg", "未知错误"));
        } catch (Exception e) {
            log.error("调用微信API异常", e);
            throw new RuntimeException("微信服务异常，请稍后重试");
        }
    }
}
