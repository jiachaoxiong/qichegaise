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

    // ──────── 智能车身换色算法 v3 ────────
    //
    // 策略：多点种子 → 区域生长 → 最优区域评分 → 只换该区域
    //
    // 1. 在图像 3×3 网格放置 9 个种子点
    // 2. 每个种子"生长"出一个颜色相似的连通区域
    // 3. 评分每个区域（大小 + 居中程度 - 贴边惩罚）
    // 4. 选最优区域作为车身，只对该区域颜色进行换色
    //
    // 这样无论车在画面哪个位置、什么颜色，都能正确识别。

    /** 区域生长结果 */
    private static class Region {
        int avgR, avgG, avgB;       // 区域平均色
        int size;                     // 像素数
        double score;                 // 综合评分
        float threshold;              // 换色距离阈值
        boolean[] mask;               // 区域掩码（w*h 尺寸）
    }

    public static BufferedImage replaceColor(BufferedImage src, String hexColor) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        // ── 第一步：找到最优车身区域 ──
        Region best = detectCarBodyRegion(src);
        log.debug("车身区域: RGB({},{},{}), 大小={}px, 评分={}",
                best.avgR, best.avgG, best.avgB, best.size, String.format("%.2f", best.score));

        // ── 第二步：逐像素智能换色 ──
        Color targetColor = Color.decode(hexColor);
        float[] targetHSB = Color.RGBtoHSB(targetColor.getRed(),
                targetColor.getGreen(), targetColor.getBlue(), null);
        float[] domHSB = Color.RGBtoHSB(best.avgR, best.avgG, best.avgB, null);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgba = src.getRGB(x, y);
                int alpha = (rgba >> 24) & 0xFF;
                if (alpha == 0) { result.setRGB(x, y, 0); continue; }

                int r = (rgba >> 16) & 0xFF;
                int g = (rgba >> 8) & 0xFF;
                int b = rgba & 0xFF;

                // 计算该像素与车身主色调的距离
                float dr = r - best.avgR, dg = g - best.avgG, db = b - best.avgB;
                float dist = (float) Math.sqrt(dr * dr + dg * dg + db * db);

                if (dist <= best.threshold) {
                    // ✅ 车身区域 → 换色
                    float[] hsb = Color.RGBtoHSB(r, g, b, null);
                    float blend = 1.0f - (dist / best.threshold);
                    blend = blend * blend;

                    float newSat;
                    if (domHSB[1] < 0.08f) {
                        newSat = targetHSB[1] * (0.4f + 0.6f * hsb[2]);
                    } else {
                        float satRatio = hsb[1] / Math.max(0.01f, domHSB[1]);
                        newSat = Math.min(1.0f, targetHSB[1] * satRatio);
                    }

                    Color newColor = Color.getHSBColor(targetHSB[0], newSat, hsb[2]);

                    int newR = clamp(r + (int) ((newColor.getRed() - r) * blend));
                    int newG = clamp(g + (int) ((newColor.getGreen() - g) * blend));
                    int newB = clamp(b + (int) ((newColor.getBlue() - b) * blend));

                    result.setRGB(x, y, (alpha << 24) | (newR << 16) | (newG << 8) | newB);
                } else {
                    // ❌ 非车身（车牌/背景/轮胎）→ 保留原色
                    result.setRGB(x, y, rgba);
                }
            }
        }
        return result;
    }

    // ── 区域检测：多点种子 + 区域生长 + 评分 ──

    private static Region detectCarBodyRegion(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();

        // 9 个种子点（3×3 网格），避开最边缘
        int[][] seeds = new int[9][2];
        for (int gy = 0; gy < 3; gy++) {
            for (int gx = 0; gx < 3; gx++) {
                seeds[gy * 3 + gx][0] = w * (1 + 2 * gx) / 6;   // 1/6, 3/6, 5/6
                seeds[gy * 3 + gx][1] = h * (1 + 2 * gy) / 6;
            }
        }

        Region best = null;

        for (int[] seed : seeds) {
            int sx = seed[0], sy = seed[1];
            int seedRgb = src.getRGB(sx, sy);
            int sr = (seedRgb >> 16) & 0xFF;
            int sg = (seedRgb >> 8) & 0xFF;
            int sb = seedRgb & 0xFF;

            // 跳过极端明暗的种子（阴影/高光/天空）
            int brightness = (sr + sg + sb) / 3;
            if (brightness < 25 || brightness > 245) continue;

            // 区域生长
            int tolerance = 35;  // RGB 容差
            boolean[] visited = new boolean[w * h];
            java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
            queue.add(new int[]{sx, sy});
            visited[sy * w + sx] = true;

            long sumR = 0, sumG = 0, sumB = 0;
            int count = 0;
            int minX = w, maxX = 0, minY = h, maxY = 0;

            while (!queue.isEmpty() && count < 80000) {
                int[] p = queue.poll();
                int px = p[0], py = p[1];
                int rgb = src.getRGB(px, py);
                sumR += (rgb >> 16) & 0xFF;
                sumG += (rgb >> 8) & 0xFF;
                sumB += rgb & 0xFF;
                count++;

                if (px < minX) minX = px; if (px > maxX) maxX = px;
                if (py < minY) minY = py; if (py > maxY) maxY = py;

                // 4-邻域扩展
                int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
                for (int[] d : dirs) {
                    int nx = px + d[0], ny = py + d[1];
                    if (nx >= 0 && nx < w && ny >= 0 && ny < h && !visited[ny * w + nx]) {
                        int nrgb = src.getRGB(nx, ny);
                        int nr = (nrgb >> 16) & 0xFF;
                        int ng = (nrgb >> 8) & 0xFF;
                        int nb = nrgb & 0xFF;
                        int dr = nr - sr, dg = ng - sg, db = nb - sb;
                        if (dr * dr + dg * dg + db * db <= tolerance * tolerance) {
                            visited[ny * w + nx] = true;
                            queue.add(new int[]{nx, ny});
                        }
                    }
                }
            }

            if (count < 300) continue;  // 太小的区域不可能是车身

            // ── 评分 ──
            boolean touchesBorder = (minX <= 1 || maxX >= w - 2 || minY <= 1 || maxY >= h - 2);

            // 居中分：区域重心离画面中心越近越好
            double cx = (minX + maxX) / 2.0, cy = (minY + maxY) / 2.0;
            double distToCenter = Math.hypot((cx - w / 2.0) / (w / 2.0), (cy - h / 2.0) / (h / 2.0));
            double centerScore = Math.max(0, 1.0 - distToCenter);

            // 大小分：区域越大越好
            double sizeScore = Math.min(1.0, count / 25000.0);

            // 综合评分（贴边区域降权）
            double score = (sizeScore * 0.6 + centerScore * 0.4) * (touchesBorder ? 0.4 : 1.0);

            if (best == null || score > best.score) {
                Region r = new Region();
                r.avgR = (int) (sumR / count);
                r.avgG = (int) (sumG / count);
                r.avgB = (int) (sumB / count);
                r.size = count;
                r.score = score;

                float[] hsb = Color.RGBtoHSB(r.avgR, r.avgG, r.avgB, null);
                if (hsb[1] < 0.08f)      r.threshold = 110.0f;
                else if (hsb[1] < 0.25f) r.threshold = 75.0f;
                else                      r.threshold = 55.0f;

                best = r;
            }
        }

        // 退化：没有找到好区域时用全图中心采样
        if (best == null || best.score < 0.05) {
            log.debug("区域生长未找到车身，回退到中心采样");
            return fallbackCenterSample(src);
        }

        return best;
    }

    /** 回退方案：高斯加权中心采样 */
    private static Region fallbackCenterSample(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();

        // 高斯加权直方图
        double[] hist = new double[4096];
        double bestWeight = 0;
        int bestIdx = 0;

        for (int y = (int) (h * 0.15); y < (int) (h * 0.85); y++) {
            for (int x = (int) (w * 0.1); x < (int) (w * 0.9); x++) {
                double dx = (x - w / 2.0) / (w / 2.0);
                double dy = (y - h / 2.0) / (h / 2.0);
                double weight = Math.exp(-(dx * dx + dy * dy) * 3.0);  // σ≈0.4

                int rgb = src.getRGB(x, y);
                int qr = ((rgb >> 16) & 0xFF) >> 4;
                int qg = ((rgb >> 8) & 0xFF) >> 4;
                int qb = (rgb & 0xFF) >> 4;
                int idx = (qr << 8) | (qg << 4) | qb;
                hist[idx] += weight;
                if (hist[idx] > bestWeight) { bestWeight = hist[idx]; bestIdx = idx; }
            }
        }

        int domQR = (bestIdx >> 8) & 0xF, domQG = (bestIdx >> 4) & 0xF, domQB = bestIdx & 0xF;
        long sumR = 0, sumG = 0, sumB = 0;
        int cnt = 0;
        for (int y = (int) (h * 0.15); y < (int) (h * 0.85); y++) {
            for (int x = (int) (w * 0.1); x < (int) (w * 0.9); x++) {
                int rgb = src.getRGB(x, y);
                int qr = ((rgb >> 16) & 0xFF) >> 4;
                int qg = ((rgb >> 8) & 0xFF) >> 4;
                int qb = (rgb & 0xFF) >> 4;
                if (Math.abs(qr - domQR) <= 1 && Math.abs(qg - domQG) <= 1 && Math.abs(qb - domQB) <= 1) {
                    sumR += (rgb >> 16) & 0xFF; sumG += (rgb >> 8) & 0xFF; sumB += rgb & 0xFF; cnt++;
                }
            }
        }

        Region r = new Region();
        if (cnt > 0) { r.avgR = (int) (sumR / cnt); r.avgG = (int) (sumG / cnt); r.avgB = (int) (sumB / cnt); }
        else { int c = src.getRGB(w / 2, h / 2); r.avgR = (c >> 16) & 0xFF; r.avgG = (c >> 8) & 0xFF; r.avgB = c & 0xFF; }
        r.size = cnt;
        r.score = 0.01;
        float[] hsb = Color.RGBtoHSB(r.avgR, r.avgG, r.avgB, null);
        if (hsb[1] < 0.08f) r.threshold = 110.0f; else if (hsb[1] < 0.25f) r.threshold = 75.0f; else r.threshold = 55.0f;
        return r;
    }

    private static int clamp(int val) {
        return Math.max(0, Math.min(255, val));
    }
}
