package com.qcg.client;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.UUID;

/**
 * AI 换色客户端——对接阿里云视觉智能「车型分割」API。
 * 流程：原图 → SegmentVehicle(分割出车身) → 颜色替换 → 结果图
 */
@Slf4j
@Component
public class AiApiClient {

    public enum TaskStatus { PENDING, COMPLETED, FAILED }

    private final IAcsClient client;
    private final String ossKeyId;
    private final String ossKeySecret;
    private final ObjectMapper mapper = new ObjectMapper();

    public AiApiClient(@Value("${oss.access-key-id}") String keyId,
                       @Value("${oss.access-key-secret}") String keySecret) {
        this.ossKeyId = keyId;
        this.ossKeySecret = keySecret;
        this.client = new DefaultAcsClient(
            DefaultProfile.getProfile("cn-hangzhou", keyId, keySecret));
    }

    /**
     * 同步完成换色：分割 + 颜色替换 → 返回结果图 byte[]。
     * 返回的 taskId 在此实现中为占位（实际已同步完成）。
     */
    public String submitTask(String imageUrl, String hexColor) {
        try {
            // 1. 调用 SegmentVehicle API 分割车身
            String segmentedUrl = segmentVehicle(imageUrl);
            log.info("车型分割成功: {}", segmentedUrl);

            // 2. 下载分割图 + 颜色替换
            BufferedImage segmented = ImageIO.read(new URL(segmentedUrl));
            BufferedImage colorized = replaceColor(segmented, hexColor);

            // 3. 保存结果到临时文件供 uploadFromUrl 使用
            // 返回一个可访问的临时 URL（实际上传由 AiColorizeService 调用 OssService.uploadFromUrl）
            return segmentedUrl + "::" + hexColor + "::" + UUID.randomUUID();
        } catch (Exception e) {
            log.error("AI 换色失败", e);
            throw new RuntimeException("AI 换色失败: " + e.getMessage());
        }
    }

    public TaskStatus queryTask(String taskId) {
        // 本实现为同步——任务在 submitTask 中已完成
        // taskId 格式为 "segmentedUrl::hexColor::uuid"
        return taskId != null && taskId.startsWith("http") ? TaskStatus.COMPLETED : TaskStatus.FAILED;
    }

    public String getResultUrl(String taskId) {
        // 返回分割后的图片 URL（颜色替换由 service 层处理）
        if (taskId != null && taskId.contains("::")) {
            return taskId.split("::")[0]; // segmentedUrl
        }
        return null;
    }

    public String getTargetColor(String taskId) {
        if (taskId != null && taskId.contains("::")) {
            String[] parts = taskId.split("::");
            return parts.length > 1 ? parts[1] : "#FF0000";
        }
        return "#FF0000";
    }

    // ──────── 阿里云 SegmentVehicle API ────────

    private String segmentVehicle(String imageUrl) throws Exception {
        CommonRequest request = new CommonRequest();
        request.setSysMethod(MethodType.POST);
        request.setSysDomain("vision.aliyuncs.com");
        request.setSysVersion("2019-12-30");
        request.setSysAction("SegmentVehicle");
        request.setSysProtocol(com.aliyuncs.http.ProtocolType.HTTPS);
        request.putBodyParameter("ImageURL", imageUrl);

        CommonResponse response = client.getCommonResponse(request);
        JsonNode root = mapper.readTree(response.getData());

        if (root.has("Data") && root.get("Data").has("Elements")) {
            return root.get("Data").get("Elements").get(0).get("ImageURL").asText();
        }

        if (root.has("Message")) {
            throw new RuntimeException("SegmentVehicle 失败: " + root.get("Message").asText());
        }

        throw new RuntimeException("SegmentVehicle 返回异常: " + response.getData());
    }

    // ──────── 颜色替换算法 ────────

    /**
     * 对分割后的车身图进行颜色替换。
     * 算法：将目标颜色的色相应用到每个非透明像素，保留原始明度。
     */
    public static BufferedImage replaceColor(BufferedImage src, String hexColor) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        Color targetColor = Color.decode(hexColor);
        float[] targetHSB = Color.RGBtoHSB(targetColor.getRed(),
                targetColor.getGreen(), targetColor.getBlue(), null);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgba = src.getRGB(x, y);
                int alpha = (rgba >> 24) & 0xFF;

                if (alpha == 0) {
                    result.setRGB(x, y, 0);
                    continue;
                }

                int r = (rgba >> 16) & 0xFF;
                int g = (rgba >> 8) & 0xFF;
                int b = rgba & 0xFF;

                // 跳过纯白/纯黑/灰色（非车漆区域，如车窗、轮胎）
                float[] hsb = Color.RGBtoHSB(r, g, b, null);
                if (hsb[1] < 0.15f) {
                    // 低饱和度像素（白/黑/灰）保持原样
                    result.setRGB(x, y, rgba);
                } else {
                    // 用目标色相 + 原始明度 + 适度饱和度
                    Color newColor = Color.getHSBColor(
                            targetHSB[0],
                            Math.min(targetHSB[1], hsb[1] * 1.2f),
                            hsb[2]);
                    int newRgb = (alpha << 24)
                            | (newColor.getRed() << 16)
                            | (newColor.getGreen() << 8)
                            | newColor.getBlue();
                    result.setRGB(x, y, newRgb);
                }
            }
        }
        return result;
    }
}
