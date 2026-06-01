package com.qcg.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
public class AiApiClient {

    public enum TaskStatus { PENDING, COMPLETED, FAILED }

    private final String apiUrl;
    private final String apiKey;
    private final RestTemplate restTemplate;

    public AiApiClient(@Value("${ai.api-url}") String apiUrl,
                       @Value("${ai.api-key}") String apiKey) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.restTemplate = new RestTemplate();
    }

    public String submitTask(String imageUrl, String hexColor) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, String> body = Map.of(
            "image_url", imageUrl,
            "target_color", hexColor
        );

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.postForObject(
                    apiUrl + "/tasks", request, Map.class);
            if (resp != null && resp.containsKey("task_id")) {
                return (String) resp.get("task_id");
            }
            throw new RuntimeException("AI API 返回异常: " + resp);
        } catch (Exception e) {
            log.error("提交 AI 任务失败", e);
            throw new RuntimeException("AI 服务繁忙，请稍后重试");
        }
    }

    public TaskStatus queryTask(String taskId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.exchange(
                    apiUrl + "/tasks/" + taskId,
                    HttpMethod.GET,
                    request,
                    (Class<Map<String, Object>>) (Class<?>) Map.class
            ).getBody();

            if (resp != null) {
                String status = (String) resp.get("status");
                if ("completed".equalsIgnoreCase(status)) return TaskStatus.COMPLETED;
                if ("failed".equalsIgnoreCase(status)) return TaskStatus.FAILED;
            }
            return TaskStatus.PENDING;
        } catch (Exception e) {
            log.error("查询 AI 任务失败: taskId={}", taskId, e);
            throw new RuntimeException("查询 AI 任务状态失败");
        }
    }

    public String getResultUrl(String taskId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.exchange(
                    apiUrl + "/tasks/" + taskId + "/result",
                    HttpMethod.GET,
                    request,
                    (Class<Map<String, Object>>) (Class<?>) Map.class
            ).getBody();

            if (resp != null && resp.containsKey("result_url")) {
                return (String) resp.get("result_url");
            }
            throw new RuntimeException("AI 结果未就绪");
        } catch (Exception e) {
            log.error("获取 AI 结果失败: taskId={}", taskId, e);
            throw new RuntimeException("获取 AI 结果失败");
        }
    }
}
