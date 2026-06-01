package com.qcg.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.PutObjectResult;
import com.qcg.common.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OssService {

    private final OSS ossClient;

    @Value("${oss.bucket-name}")
    private String bucketName;

    @Value("${oss.endpoint}")
    private String endpoint;

    /**
     * 上传文件到 OSS，返回公网访问 URL。
     */
    public String upload(MultipartFile file) {
        String ext = getExtension(file.getOriginalFilename());
        String key = "photos/" + UUID.randomUUID().toString() + "." + ext;

        try (InputStream is = file.getInputStream()) {
            PutObjectResult result = ossClient.putObject(bucketName, key, is);
            log.info("OSS 上传成功: key={}, etag={}", key, result.getETag());
        } catch (IOException e) {
            log.error("OSS 上传失败", e);
            throw new BusinessException(500, "图片上传失败");
        }

        return String.format("https://%s.%s/%s", bucketName, endpoint, key);
    }

    /**
     * 从远端 URL 下载图片并上传到 OSS，返回 OSS URL。
     */
    public String uploadFromUrl(String sourceUrl) {
        try {
            java.net.URL url = new java.net.URL(sourceUrl);
            String ext = "jpg";
            String key = "results/" + UUID.randomUUID().toString() + "." + ext;

            try (InputStream is = url.openStream()) {
                ossClient.putObject(bucketName, key, is);
            }

            return String.format("https://%s.%s/%s", bucketName, endpoint, key);
        } catch (IOException e) {
            log.error("下载并上传图片失败: {}", sourceUrl, e);
            throw new BusinessException(500, "图片转存失败");
        }
    }

    /**
     * 上传 byte[] 到 OSS，返回公网 URL。
     */
    public String uploadBytes(byte[] data, String filename) {
        String key = "results/" + UUID.randomUUID().toString() + "-" + filename;
        ossClient.putObject(bucketName, key, new java.io.ByteArrayInputStream(data));
        return String.format("https://%s.%s/%s", bucketName, endpoint, key);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
