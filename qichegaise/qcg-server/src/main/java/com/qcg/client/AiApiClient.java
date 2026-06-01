package com.qcg.client;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.http.ProtocolType;
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
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiEndpoint;

    public AiApiClient(@Value("${oss.access-key-id}") String keyId,
                       @Value("${oss.access-key-secret}") String keySecret,
                       @Value("${ai.endpoint:vision.aliyuncs.com}") String apiEndpoint) {
        DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", keyId, keySecret);
        this.client = new DefaultAcsClient(profile);
        this.apiEndpoint = apiEndpoint;
    }

    /**
     * 提交换色请求。返回 taskId（格式：segmentedUrl::hexColor::uuid）
     */
    public String submitTask(String imageUrl, String hexColor) {
        try {
            String segmentedUrl = segmentVehicle(imageUrl);
            log.info("车型分割成功: {}", segmentedUrl);
            return segmentedUrl + "::" + hexColor + "::" + UUID.randomUUID();
        } catch (Exception e) {
            log.error("AI 换色失败", e);
            throw new RuntimeException("AI 换色失败: " + e.getMessage());
        }
    }

    public TaskStatus queryTask(String taskId) {
        return taskId != null && taskId.startsWith("http") ? TaskStatus.COMPLETED : TaskStatus.FAILED;
    }

    public String getResultUrl(String taskId) {
        if (taskId != null && taskId.contains("::")) return taskId.split("::")[0];
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
        request.setSysDomain(apiEndpoint);
        request.setSysVersion("2019-12-30");
        request.setSysAction("SegmentVehicle");
        request.setSysProtocol(ProtocolType.HTTPS);
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

    // ──────── 智能车身换色算法 ────────
    //
    // 核心思路：
    //   1. 采样图像中央区域，通过颜色直方图找到"车身主色调"
    //   2. 只对颜色接近车身主色调的像素进行换色（排除车牌、背景、轮胎等）
    //   3. 保留原始明暗层次，让新车色看起来真实有立体感
    //
    // 为什么旧算法会换车牌：
    //   旧算法简单地对所有饱和度>0.15的像素换色。白/银/黑车的车身
    //   饱和度很低会被跳过，而蓝色车牌饱和度高反而被换色。

    public static BufferedImage replaceColor(BufferedImage src, String hexColor) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        // ── 第一步：找到车身主色调 ──
        // 采样图像中央 50% 区域（大多数照片中车在中央）
        int cx = w / 4, cy = h / 4;
        int cw = w / 2, ch = h / 2;

        // 颜色量化直方图（16 级/通道 = 4096 桶）
        int[] hist = new int[4096];
        int bestIdx = 0, bestCount = 0;

        for (int y = cy; y < cy + ch; y++) {
            for (int x = cx; x < cx + cw; x++) {
                int rgb = src.getRGB(x, y);
                int qr = ((rgb >> 16) & 0xFF) >> 4;   // 0-15
                int qg = ((rgb >> 8) & 0xFF) >> 4;
                int qb = (rgb & 0xFF) >> 4;
                int idx = (qr << 8) | (qg << 4) | qb;
                hist[idx]++;
                if (hist[idx] > bestCount) {
                    bestCount = hist[idx];
                    bestIdx = idx;
                }
            }
        }

        // 提取主色调量化值
        int domQR = (bestIdx >> 8) & 0xF;
        int domQG = (bestIdx >> 4) & 0xF;
        int domQB = bestIdx & 0xF;

        // 计算主色调簇的实际平均 RGB
        long sumR = 0, sumG = 0, sumB = 0;
        int domSamples = 0;
        int minQR = Math.max(0, domQR - 1), maxQR = Math.min(15, domQR + 1);
        int minQG = Math.max(0, domQG - 1), maxQG = Math.min(15, domQG + 1);
        int minQB = Math.max(0, domQB - 1), maxQB = Math.min(15, domQB + 1);

        for (int y = cy; y < cy + ch; y++) {
            for (int x = cx; x < cx + cw; x++) {
                int rgb = src.getRGB(x, y);
                int qr = ((rgb >> 16) & 0xFF) >> 4;
                int qg = ((rgb >> 8) & 0xFF) >> 4;
                int qb = (rgb & 0xFF) >> 4;
                if (qr >= minQR && qr <= maxQR && qg >= minQG && qg <= maxQG && qb >= minQB && qb <= maxQB) {
                    sumR += (rgb >> 16) & 0xFF;
                    sumG += (rgb >> 8) & 0xFF;
                    sumB += rgb & 0xFF;
                    domSamples++;
                }
            }
        }

        int domR, domG, domB;
        if (domSamples > 0) {
            domR = (int) (sumR / domSamples);
            domG = (int) (sumG / domSamples);
            domB = (int) (sumB / domSamples);
        } else {
            // 退化：取图像正中央像素
            int centerRgb = src.getRGB(w / 2, h / 2);
            domR = (centerRgb >> 16) & 0xFF;
            domG = (centerRgb >> 8) & 0xFF;
            domB = centerRgb & 0xFF;
        }

        float[] domHSB = Color.RGBtoHSB(domR, domG, domB, null);
        log.debug("车身主色调: RGB({},{},{}), 样本数={}", domR, domG, domB, domSamples);

        // ── 第二步：计算自适应阈值 ──
        // 低饱和度车（白/黑/银/灰）→ 宽阈值，覆盖不同明暗面
        // 高饱和度车（红/蓝/绿）→ 窄阈值，精准只换车身
        float colorThreshold;
        if (domHSB[1] < 0.08f) {
            colorThreshold = 110.0f;   // 无彩色车身：覆盖大面积明暗变化
        } else if (domHSB[1] < 0.25f) {
            colorThreshold = 75.0f;    // 低饱和车
        } else {
            colorThreshold = 55.0f;    // 鲜艳车身：精准匹配
        }

        // ── 第三步：逐像素智能换色 ──
        Color targetColor = Color.decode(hexColor);
        float[] targetHSB = Color.RGBtoHSB(targetColor.getRed(),
                targetColor.getGreen(), targetColor.getBlue(), null);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgba = src.getRGB(x, y);
                int alpha = (rgba >> 24) & 0xFF;
                if (alpha == 0) { result.setRGB(x, y, 0); continue; }

                int r = (rgba >> 16) & 0xFF;
                int g = (rgba >> 8) & 0xFF;
                int b = rgba & 0xFF;

                // 计算该像素与车身主色调的 RGB 欧氏距离
                float dr = r - domR, dg = g - domG, db = b - domB;
                float dist = (float) Math.sqrt(dr * dr + dg * dg + db * db);

                if (dist <= colorThreshold) {
                    // ✅ 属于车身区域 → 换色
                    float[] hsb = Color.RGBtoHSB(r, g, b, null);

                    // 混合系数：越接近主色调越强，边界处渐变过渡
                    float blend = 1.0f - (dist / colorThreshold);
                    blend = blend * blend;  // 二次方过渡，边缘更自然

                    // 计算目标饱和度
                    float newSat;
                    if (domHSB[1] < 0.08f) {
                        // 无彩色原车 → 目标色的饱和度根据亮度调整（暗处低饱和更真实）
                        newSat = targetHSB[1] * (0.4f + 0.6f * hsb[2]);
                    } else {
                        // 有彩色原车 → 等比缩放饱和度
                        float satRatio = hsb[1] / Math.max(0.01f, domHSB[1]);
                        newSat = Math.min(1.0f, targetHSB[1] * satRatio);
                    }

                    // 目标色 = 目标色相 + 调整后饱和度 + 原始亮度
                    Color newColor = Color.getHSBColor(targetHSB[0], newSat, hsb[2]);

                    // 按 blend 融合（边界平滑过渡）
                    int newR = clamp(r + (int) ((newColor.getRed() - r) * blend));
                    int newG = clamp(g + (int) ((newColor.getGreen() - g) * blend));
                    int newB = clamp(b + (int) ((newColor.getBlue() - b) * blend));

                    result.setRGB(x, y, (alpha << 24) | (newR << 16) | (newG << 8) | newB);
                } else {
                    // ❌ 不属于车身（车牌/背景/轮胎/窗户）→ 保留原色
                    result.setRGB(x, y, rgba);
                }
            }
        }
        return result;
    }

    private static int clamp(int val) {
        return Math.max(0, Math.min(255, val));
    }
}
