package com.qcg.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
public class WechatClient {

    private final String appId;
    private final String appSecret;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

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

        log.info("调用微信登录 API: appid={}, code={}...", appId, code.substring(0, Math.min(code.length(), 8)));

        try {
            ResponseEntity<String> entity = restTemplate.getForEntity(url, String.class);
            String body = entity.getBody();

            if (body == null) {
                log.error("微信 API 返回空响应");
                throw new RuntimeException("微信服务返回空响应");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = mapper.readValue(body, Map.class);

            if (resp.containsKey("openid")) {
                log.info("微信登录成功: openid={}", resp.get("openid"));
                return (String) resp.get("openid");
            }

            // 微信返回了错误
            Object errcode = resp.get("errcode");
            Object errmsg = resp.get("errmsg");
            log.error("微信 API 错误: errcode={}, errmsg={}, body={}", errcode, errmsg, body);

            String msg = errmsg != null ? errmsg.toString() : "未知错误";
            throw new RuntimeException("微信登录失败: " + msg);

        } catch (RuntimeException e) {
            throw e; // 直接抛出我们自己的异常
        } catch (Exception e) {
            log.error("调用微信 API 网络异常", e);
            throw new RuntimeException("微信服务异常，请稍后重试");
        }
    }
}
