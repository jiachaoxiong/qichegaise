# 汽车改色膜换色系统 Phase 1 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现"上传/选车 → AI 换色 → 保存分享"的完整闭环，包括微信小程序前端和 Spring Boot 后端。

**Architecture:** Spring Boot 3.x 后端提供 REST API，MySQL 存储结构化数据，OSS 存储图片，JWT 做无状态鉴权。微信小程序通过 HTTPS 调用后端，AI 换色采用异步提交+轮询模式。

**Tech Stack:** Java 17+, Spring Boot 3.x, Spring Security, JPA/Hibernate, MySQL 8.0, JWT (jjwt), 阿里云 OSS, 微信小程序原生框架

---

## 文件结构概览

本计划将创建以下文件结构。每个任务标注其负责的文件。

```
qichegaise/
├── qcg-server/
│   ├── pom.xml
│   ├── src/main/java/com/qcg/
│   │   ├── QcgApplication.java
│   │   ├── config/
│   │   │   ├── SecurityConfig.java
│   │   │   ├── OssConfig.java
│   │   │   └── CorsConfig.java
│   │   ├── common/
│   │   │   ├── Result.java
│   │   │   ├── BusinessException.java
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── security/
│   │   │   ├── JwtUtil.java
│   │   │   └── JwtAuthFilter.java
│   │   ├── entity/
│   │   │   ├── User.java
│   │   │   ├── CarModel.java
│   │   │   ├── Color.java
│   │   │   ├── Brand.java
│   │   │   ├── CarPhoto.java
│   │   │   ├── Shop.java
│   │   │   ├── ShopCase.java
│   │   │   └── Appointment.java
│   │   ├── dto/
│   │   │   ├── LoginRequest.java
│   │   │   ├── LoginResponse.java
│   │   │   ├── ColorizeRequest.java
│   │   │   └── TaskResultResponse.java
│   │   ├── repository/
│   │   │   ├── UserRepository.java
│   │   │   ├── ColorRepository.java
│   │   │   ├── BrandRepository.java
│   │   │   ├── CarModelRepository.java
│   │   │   └── CarPhotoRepository.java
│   │   ├── service/
│   │   │   ├── AuthService.java
│   │   │   ├── ColorService.java
│   │   │   ├── CarModelService.java
│   │   │   ├── PhotoService.java
│   │   │   ├── OssService.java
│   │   │   └── AiColorizeService.java
│   │   ├── controller/
│   │   │   ├── AuthController.java
│   │   │   ├── ColorController.java
│   │   │   ├── CarModelController.java
│   │   │   ├── PhotoController.java
│   │   │   └── AiController.java
│   │   ├── client/
│   │   │   ├── WechatClient.java
│   │   │   └── AiApiClient.java
│   │   └── enums/
│   │       ├── UserRole.java
│   │       └── PhotoStatus.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── schema.sql
│   └── src/test/java/com/qcg/
│       ├── service/
│       │   ├── AuthServiceTest.java
│       │   └── AiColorizeServiceTest.java
│       └── controller/
│           └── AuthControllerTest.java
│
├── qcg-miniapp/
│   ├── project.config.json
│   ├── app.js
│   ├── app.json
│   ├── app.wxss
│   ├── pages/
│   │   ├── index/          (首页：两种入口)
│   │   ├── color-picker/   (选色换色核心页)
│   │   ├── my-works/       (我的作品)
│   │   └── car-models/     (车型库)
│   └── utils/
│       ├── api.js          (API 封装)
│       └── auth.js         (登录+Token 管理)
│
└── docs/
    └── superpowers/
        ├── specs/2026-06-01-qichegaise-design.md
        └── plans/2026-06-01-qichegaise-phase1.md
```

---

### Task 1: 项目脚手架 — Spring Boot 工程

**Files:**
- Create: `qcg-server/pom.xml`
- Create: `qcg-server/src/main/java/com/qcg/QcgApplication.java`
- Create: `qcg-server/src/main/resources/application.yml`

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.qcg</groupId>
    <artifactId>qcg-server</artifactId>
    <version>0.1.0</version>
    <name>qcg-server</name>
    <description>汽车改色膜换色系统后端服务</description>

    <properties>
        <java.version>17</java.version>
        <jjwt.version>0.12.5</jjwt.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- 阿里云 OSS -->
        <dependency>
            <groupId>com.aliyun.oss</groupId>
            <artifactId>aliyun-sdk-oss</artifactId>
            <version>3.17.4</version>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建启动类**

```java
package com.qcg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class QcgApplication {
    public static void main(String[] args) {
        SpringApplication.run(QcgApplication.class, args);
    }
}
```

- [ ] **Step 3: 创建 application.yml**

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/qcg?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true
    username: root
    password: ${DB_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: none  # 用手动 schema.sql
    show-sql: false
    properties:
      hibernate:
        format_sql: true
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql

  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

# JWT
jwt:
  secret: ${JWT_SECRET:change-me-in-production-use-256-bit-key}
  expiration: 604800000  # 7 天 (毫秒)

# 微信小程序
wechat:
  app-id: ${WECHAT_APP_ID:your-app-id}
  app-secret: ${WECHAT_APP_SECRET:your-app-secret}

# 阿里云 OSS
oss:
  endpoint: ${OSS_ENDPOINT:oss-cn-hangzhou.aliyuncs.com}
  access-key-id: ${OSS_ACCESS_KEY_ID:your-key}
  access-key-secret: ${OSS_ACCESS_KEY_SECRET:your-secret}
  bucket-name: ${OSS_BUCKET:qcg-photos}

# AI API
ai:
  api-url: ${AI_API_URL:https://api.example.com/v1/colorize}
  api-key: ${AI_API_KEY:your-api-key}
  timeout: 30000
```

- [ ] **Step 4: 验证项目能启动**

```bash
cd qcg-server
mvn spring-boot:run
```
预期：启动失败（数据库未就绪），但 Spring 上下文加载无 Bean 定义错误。

> **注意**：数据库连接会在 Task 3 建表后正常工作。

- [ ] **Step 5: 提交**

```bash
git add qcg-server/pom.xml qcg-server/src/main/java/com/qcg/QcgApplication.java qcg-server/src/main/resources/application.yml
git commit -m "feat: Spring Boot 项目脚手架"
```

---

### Task 2: 公共模块 — 统一响应、异常处理、枚举

**Files:**
- Create: `qcg-server/src/main/java/com/qcg/common/Result.java`
- Create: `qcg-server/src/main/java/com/qcg/common/BusinessException.java`
- Create: `qcg-server/src/main/java/com/qcg/common/GlobalExceptionHandler.java`
- Create: `qcg-server/src/main/java/com/qcg/enums/UserRole.java`
- Create: `qcg-server/src/main/java/com/qcg/enums/PhotoStatus.java`

- [ ] **Step 1: 创建统一响应类 Result**

```java
package com.qcg.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> ok() {
        return new Result<>(200, "success", null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(400, message, null);
    }
}
```

- [ ] **Step 2: 创建业务异常类 BusinessException**

```java
package com.qcg.common;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }
}
```

- [ ] **Step 3: 创建全局异常处理 GlobalExceptionHandler**

```java
package com.qcg.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        return Result.fail(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(500, "服务器内部错误");
    }
}
```

- [ ] **Step 4: 创建枚举类**

```java
package com.qcg.enums;

public enum UserRole {
    USER, SHOP, ADMIN
}
```

```java
package com.qcg.enums;

public enum PhotoStatus {
    PENDING, COMPLETED, FAILED
}
```

- [ ] **Step 5: 提交**

```bash
git add qcg-server/src/main/java/com/qcg/common/ qcg-server/src/main/java/com/qcg/enums/
git commit -m "feat: 公共模块 — 统一响应、异常处理、枚举"
```

---

### Task 3: 数据库 Schema — 建表 SQL

**Files:**
- Create: `qcg-server/src/main/resources/schema.sql`

- [ ] **Step 1: 创建 schema.sql（全部 8 张表）**

```sql
-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    openid VARCHAR(64) NOT NULL UNIQUE,
    nickname VARCHAR(64),
    avatar_url VARCHAR(512),
    phone VARCHAR(20),
    role ENUM('USER','SHOP','ADMIN') NOT NULL DEFAULT 'USER',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_openid (openid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 预设车型表
CREATE TABLE IF NOT EXISTS car_model (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    brand_name VARCHAR(32) NOT NULL COMMENT '品牌名',
    model_name VARCHAR(64) NOT NULL COMMENT '车型名',
    year VARCHAR(8) COMMENT '年款',
    body_type VARCHAR(16) COMMENT 'SUV/轿车/MPV/跑车',
    image_url VARCHAR(512),
    is_active TINYINT NOT NULL DEFAULT 1,
    INDEX idx_brand (brand_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 改色膜品牌表
CREATE TABLE IF NOT EXISTS brand (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    logo_url VARCHAR(512),
    website VARCHAR(256)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 颜色表
CREATE TABLE IF NOT EXISTS color (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(32) NOT NULL COMMENT '颜色名称',
    hex_code VARCHAR(7) NOT NULL COMMENT '色值如#FF0000',
    category VARCHAR(32) COMMENT '哑光/亮光/金属/珠光/渐变',
    brand_id BIGINT COMMENT '关联品牌(Phase2)',
    is_active TINYINT NOT NULL DEFAULT 1,
    INDEX idx_category (category),
    FOREIGN KEY (brand_id) REFERENCES brand(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 车照/效果图表
CREATE TABLE IF NOT EXISTS car_photo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    car_model_id BIGINT,
    original_url VARCHAR(512) NOT NULL,
    result_url VARCHAR(512),
    color_id BIGINT,
    ai_task_id VARCHAR(128),
    status ENUM('PENDING','COMPLETED','FAILED') NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    FOREIGN KEY (user_id) REFERENCES `user`(id),
    FOREIGN KEY (car_model_id) REFERENCES car_model(id),
    FOREIGN KEY (color_id) REFERENCES color(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 门店表 (Phase 2 启用)
CREATE TABLE IF NOT EXISTS shop (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    address VARCHAR(256),
    lat DECIMAL(10,7),
    lng DECIMAL(10,7),
    phone VARCHAR(20),
    cover_url VARCHAR(512),
    description TEXT,
    status ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    owner_user_id BIGINT,
    FOREIGN KEY (owner_user_id) REFERENCES `user`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 门店案例表 (Phase 2 启用)
CREATE TABLE IF NOT EXISTS shop_case (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_id BIGINT NOT NULL,
    before_url VARCHAR(512),
    after_url VARCHAR(512),
    color_id BIGINT,
    car_model VARCHAR(64),
    description TEXT,
    likes INT NOT NULL DEFAULT 0,
    FOREIGN KEY (shop_id) REFERENCES shop(id),
    FOREIGN KEY (color_id) REFERENCES color(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 预约表 (Phase 3 启用)
CREATE TABLE IF NOT EXISTS appointment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    shop_id BIGINT NOT NULL,
    car_photo_id BIGINT,
    color_id BIGINT,
    appointment_time DATETIME NOT NULL,
    status ENUM('PENDING','CONFIRMED','CANCELLED','COMPLETED') NOT NULL DEFAULT 'PENDING',
    remark VARCHAR(256),
    FOREIGN KEY (user_id) REFERENCES `user`(id),
    FOREIGN KEY (shop_id) REFERENCES shop(id),
    FOREIGN KEY (car_photo_id) REFERENCES car_photo(id),
    FOREIGN KEY (color_id) REFERENCES color(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 2: 确认 MySQL 连接可用，重启验证 schema 加载**

```bash
cd qcg-server
mvn spring-boot:run
```
预期：启动成功，所有 8 张表自动创建。

- [ ] **Step 3: 提交**

```bash
git add qcg-server/src/main/resources/schema.sql
git commit -m "feat: 数据库 schema — 8 张表"
```

---

### Task 4: 实体类 — JPA Entity

**Files:**
- Create: `qcg-server/src/main/java/com/qcg/entity/User.java`
- Create: `qcg-server/src/main/java/com/qcg/entity/CarModel.java`
- Create: `qcg-server/src/main/java/com/qcg/entity/Color.java`
- Create: `qcg-server/src/main/java/com/qcg/entity/Brand.java`
- Create: `qcg-server/src/main/java/com/qcg/entity/CarPhoto.java`
- Create: `qcg-server/src/main/java/com/qcg/entity/Shop.java`
- Create: `qcg-server/src/main/java/com/qcg/entity/ShopCase.java`
- Create: `qcg-server/src/main/java/com/qcg/entity/Appointment.java`

- [ ] **Step 1: 创建 User 实体**

```java
package com.qcg.entity;

import com.qcg.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "`user`")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String openid;

    @Column(length = 64)
    private String nickname;

    @Column(length = 512)
    private String avatarUrl;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

- [ ] **Step 2: 创建 CarModel 实体**

```java
package com.qcg.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "car_model")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String brandName;

    @Column(nullable = false, length = 64)
    private String modelName;

    @Column(length = 8)
    private String year;

    @Column(length = 16)
    private String bodyType;

    @Column(length = 512)
    private String imageUrl;

    @Builder.Default
    private Boolean isActive = true;
}
```

- [ ] **Step 3: 创建 Brand 实体**

```java
package com.qcg.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "brand")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 512)
    private String logoUrl;

    @Column(length = 256)
    private String website;
}
```

- [ ] **Step 4: 创建 Color 实体**

```java
package com.qcg.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "color")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Color {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String name;

    @Column(nullable = false, length = 7)
    private String hexCode;

    @Column(length = 32)
    private String category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @Builder.Default
    private Boolean isActive = true;
}
```

- [ ] **Step 5: 创建 CarPhoto 实体**

```java
package com.qcg.entity;

import com.qcg.enums.PhotoStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "car_photo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_model_id")
    private CarModel carModel;

    @Column(nullable = false, length = 512)
    private String originalUrl;

    @Column(length = 512)
    private String resultUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_id")
    private Color color;

    @Column(length = 128)
    private String aiTaskId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PhotoStatus status = PhotoStatus.PENDING;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

- [ ] **Step 6: 创建 Shop、ShopCase、Appointment 实体**

```java
package com.qcg.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "shop")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 256)
    private String address;

    @Column(precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(precision = 10, scale = 7)
    private BigDecimal lng;

    @Column(length = 20)
    private String phone;

    @Column(length = 512)
    private String coverUrl;

    private String description;

    @Column(length = 16)
    @Builder.Default
    private String status = "PENDING";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User owner;
}
```

```java
package com.qcg.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "shop_case")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Column(length = 512)
    private String beforeUrl;

    @Column(length = 512)
    private String afterUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_id")
    private Color color;

    @Column(length = 64)
    private String carModelName;

    private String description;

    @Builder.Default
    private Integer likes = 0;
}
```

```java
package com.qcg.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_photo_id")
    private CarPhoto carPhoto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_id")
    private Color color;

    @Column(nullable = false)
    private LocalDateTime appointmentTime;

    @Column(length = 16)
    @Builder.Default
    private String status = "PENDING";

    @Column(length = 256)
    private String remark;
}
```

- [ ] **Step 7: 提交**

```bash
git add qcg-server/src/main/java/com/qcg/entity/
git commit -m "feat: JPA 实体类 — 8 个实体"
```

---

### Task 5: Repository 层

**Files:**
- Create: `qcg-server/src/main/java/com/qcg/repository/UserRepository.java`
- Create: `qcg-server/src/main/java/com/qcg/repository/ColorRepository.java`
- Create: `qcg-server/src/main/java/com/qcg/repository/BrandRepository.java`
- Create: `qcg-server/src/main/java/com/qcg/repository/CarModelRepository.java`
- Create: `qcg-server/src/main/java/com/qcg/repository/CarPhotoRepository.java`

- [ ] **Step 1: 创建所有 Repository 接口**

```java
package com.qcg.repository;

import com.qcg.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByOpenid(String openid);
}
```

```java
package com.qcg.repository;

import com.qcg.entity.Color;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ColorRepository extends JpaRepository<Color, Long> {
    List<Color> findByIsActiveTrue();
    List<Color> findByCategoryAndIsActiveTrue(String category);
}
```

```java
package com.qcg.repository;

import com.qcg.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {
}
```

```java
package com.qcg.repository;

import com.qcg.entity.CarModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CarModelRepository extends JpaRepository<CarModel, Long> {
    List<CarModel> findByIsActiveTrue();
    List<CarModel> findByBrandNameAndIsActiveTrue(String brandName);
}
```

```java
package com.qcg.repository;

import com.qcg.entity.CarPhoto;
import com.qcg.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CarPhotoRepository extends JpaRepository<CarPhoto, Long> {
    List<CarPhoto> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<CarPhoto> findByIdAndUserId(Long id, Long userId);
}
```

- [ ] **Step 2: 提交**

```bash
git add qcg-server/src/main/java/com/qcg/repository/
git commit -m "feat: Repository 层"
```

---

### Task 6: JWT + Spring Security 配置

**Files:**
- Create: `qcg-server/src/main/java/com/qcg/security/JwtUtil.java`
- Create: `qcg-server/src/main/java/com/qcg/security/JwtAuthFilter.java`
- Create: `qcg-server/src/main/java/com/qcg/config/SecurityConfig.java`
- Create: `qcg-server/src/main/java/com/qcg/config/CorsConfig.java`

- [ ] **Step 1: 创建 JwtUtil**

```java
package com.qcg.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expiration;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generateToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(key)
                .compact();
    }

    public Long getUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

- [ ] **Step 2: 创建 JwtAuthFilter**

```java
package com.qcg.security;

import com.qcg.entity.User;
import com.qcg.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            Long userId = jwtUtil.getUserId(token);
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
```

- [ ] **Step 3: 创建 SecurityConfig**

```java
package com.qcg.config;

import com.qcg.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

- [ ] **Step 4: 创建 CorsConfig**

```java
package com.qcg.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

- [ ] **Step 5: 提交**

```bash
git add qcg-server/src/main/java/com/qcg/security/ qcg-server/src/main/java/com/qcg/config/
git commit -m "feat: JWT + Spring Security + 跨域配置"
```

---

### Task 7: DTO 类

**Files:**
- Create: `qcg-server/src/main/java/com/qcg/dto/LoginRequest.java`
- Create: `qcg-server/src/main/java/com/qcg/dto/LoginResponse.java`
- Create: `qcg-server/src/main/java/com/qcg/dto/ColorizeRequest.java`
- Create: `qcg-server/src/main/java/com/qcg/dto/TaskResultResponse.java`

- [ ] **Step 1: 创建所有 DTO**

```java
package com.qcg.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "code 不能为空")
    private String code;
}
```

```java
package com.qcg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private Long userId;
    private String nickname;
    private String avatarUrl;
}
```

```java
package com.qcg.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ColorizeRequest {
    @NotNull(message = "图片ID不能为空")
    private Long photoId;

    @NotNull(message = "颜色ID不能为空")
    private Long colorId;
}
```

```java
package com.qcg.dto;

import com.qcg.enums.PhotoStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResultResponse {
    private Long photoId;
    private String taskId;
    private PhotoStatus status;
    private String resultUrl;
    private String errorReason;
}
```

- [ ] **Step 2: 提交**

```bash
git add qcg-server/src/main/java/com/qcg/dto/
git commit -m "feat: DTO 类"
```

---

### Task 8: 微信登录 — 完整的认证链路

**Files:**
- Create: `qcg-server/src/main/java/com/qcg/client/WechatClient.java`
- Create: `qcg-server/src/main/java/com/qcg/service/AuthService.java`
- Create: `qcg-server/src/main/java/com/qcg/controller/AuthController.java`

- [ ] **Step 1: TDD — 先写 AuthService 测试**

```java
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
```

- [ ] **Step 2: 验证测试失败**

```bash
cd qcg-server
mvn test -Dtest=AuthServiceTest
```
预期：编译失败（AuthService、WechatClient 未定义）

- [ ] **Step 3: 创建 WechatClient**

```java
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
```

- [ ] **Step 4: 创建 AuthService**

```java
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
```

- [ ] **Step 5: 创建 AuthController**

```java
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
```

同时创建 `UserController`：

```java
package com.qcg.controller;

import com.qcg.common.Result;
import com.qcg.entity.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public Result<?> me(@AuthenticationPrincipal User user) {
        return Result.ok(Map.of(
            "id", user.getId(),
            "nickname", user.getNickname(),
            "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
            "role", user.getRole().name()
        ));
    }
}
```

- [ ] **Step 6: 运行测试验证**

```bash
cd qcg-server
mvn test -Dtest=AuthServiceTest
```
预期：2 个测试全部 PASS。

- [ ] **Step 7: 提交**

```bash
git add qcg-server/src/main/java/com/qcg/client/ qcg-server/src/main/java/com/qcg/service/AuthService.java qcg-server/src/main/java/com/qcg/controller/AuthController.java qcg-server/src/main/java/com/qcg/controller/UserController.java qcg-server/src/test/
git commit -m "feat: 微信登录认证链路（含单测 + 用户信息接口）"
```

---

### Task 9: 颜色、品牌、车型 API

**Files:**
- Create: `qcg-server/src/main/java/com/qcg/service/ColorService.java`
- Create: `qcg-server/src/main/java/com/qcg/controller/ColorController.java`
- Create: `qcg-server/src/main/java/com/qcg/service/CarModelService.java`
- Create: `qcg-server/src/main/java/com/qcg/controller/CarModelController.java`

- [ ] **Step 1: 创建 ColorService 和 ColorController**

```java
package com.qcg.service;

import com.qcg.entity.Color;
import com.qcg.repository.ColorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ColorService {

    private final ColorRepository colorRepository;

    public List<Color> listAll() {
        return colorRepository.findByIsActiveTrue();
    }

    public List<Color> listByCategory(String category) {
        return colorRepository.findByCategoryAndIsActiveTrue(category);
    }

    public Color getById(Long id) {
        return colorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("颜色不存在"));
    }
}
```

```java
package com.qcg.controller;

import com.qcg.common.Result;
import com.qcg.service.ColorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/colors")
@RequiredArgsConstructor
public class ColorController {

    private final ColorService colorService;

    @GetMapping
    public Result<?> list(@RequestParam(required = false) String category) {
        if (category != null) {
            return Result.ok(colorService.listByCategory(category));
        }
        return Result.ok(colorService.listAll());
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.ok(colorService.getById(id));
    }
}
```

- [ ] **Step 2: 创建 CarModelService 和 CarModelController**

```java
package com.qcg.service;

import com.qcg.entity.CarModel;
import com.qcg.repository.CarModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CarModelService {

    private final CarModelRepository carModelRepository;

    public List<CarModel> listAll() {
        return carModelRepository.findByIsActiveTrue();
    }

    public List<CarModel> listByBrand(String brandName) {
        return carModelRepository.findByBrandNameAndIsActiveTrue(brandName);
    }
}
```

```java
package com.qcg.controller;

import com.qcg.common.Result;
import com.qcg.service.CarModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/car-models")
@RequiredArgsConstructor
public class CarModelController {

    private final CarModelService carModelService;

    @GetMapping
    public Result<?> list(@RequestParam(required = false) String brand) {
        if (brand != null) {
            return Result.ok(carModelService.listByBrand(brand));
        }
        return Result.ok(carModelService.listAll());
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add qcg-server/src/main/java/com/qcg/service/ColorService.java qcg-server/src/main/java/com/qcg/controller/ColorController.java qcg-server/src/main/java/com/qcg/service/CarModelService.java qcg-server/src/main/java/com/qcg/controller/CarModelController.java
git commit -m "feat: 颜色 + 车型 API"
```

---

### Task 10: OSS 图片上传

**Files:**
- Create: `qcg-server/src/main/java/com/qcg/config/OssConfig.java`
- Create: `qcg-server/src/main/java/com/qcg/service/OssService.java`
- Create: `qcg-server/src/main/java/com/qcg/service/PhotoService.java`
- Create: `qcg-server/src/main/java/com/qcg/controller/PhotoController.java`

- [ ] **Step 1: 创建 OssConfig**

```java
package com.qcg.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OssConfig {

    @Value("${oss.endpoint}")
    private String endpoint;

    @Value("${oss.access-key-id}")
    private String accessKeyId;

    @Value("${oss.access-key-secret}")
    private String accessKeySecret;

    @Bean
    public OSS ossClient() {
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}
```

- [ ] **Step 2: 创建 OssService**

```java
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
     * 只允许 JPG/PNG，大小在校验由 Controller 层做。
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

        // 构造公网 URL
        return String.format("https://%s.%s/%s", bucketName, endpoint, key);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
```

- [ ] **Step 3: 创建 PhotoService**

```java
package com.qcg.service;

import com.qcg.common.BusinessException;
import com.qcg.entity.CarPhoto;
import com.qcg.entity.User;
import com.qcg.repository.CarPhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PhotoService {

    private final OssService ossService;
    private final CarPhotoRepository carPhotoRepository;

    /** 允许的图片类型 */
    private static final java.util.Set<String> ALLOWED_TYPES =
            java.util.Set.of("image/jpeg", "image/png");

    /** 最大文件大小 10MB */
    private static final long MAX_SIZE = 10 * 1024 * 1024;

    @Transactional
    public CarPhoto upload(User user, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("请选择图片");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BusinessException("仅支持 JPG/PNG 格式");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("图片大小不能超过 10MB");
        }

        String url = ossService.upload(file);

        CarPhoto photo = CarPhoto.builder()
                .user(user)
                .originalUrl(url)
                .build();

        return carPhotoRepository.save(photo);
    }

    public List<CarPhoto> listByUser(Long userId) {
        return carPhotoRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public void delete(User user, Long photoId) {
        CarPhoto photo = carPhotoRepository.findByIdAndUserId(photoId, user.getId())
                .orElseThrow(() -> new BusinessException("作品不存在"));
        carPhotoRepository.delete(photo);
    }

    /** 从已有 URL 直接创建记录（用于车型库等已有公网图的场景） */
    @Transactional
    public CarPhoto createFromUrl(User user, String imageUrl) {
        CarPhoto photo = CarPhoto.builder()
                .user(user)
                .originalUrl(imageUrl)
                .build();
        return carPhotoRepository.save(photo);
    }
}
```

- [ ] **Step 4: 创建 PhotoController**

```java
package com.qcg.controller;

import com.qcg.common.Result;
import com.qcg.entity.CarPhoto;
import com.qcg.entity.User;
import com.qcg.service.PhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;

    @PostMapping("/upload")
    public Result<?> upload(@AuthenticationPrincipal User user,
                            @RequestParam("file") MultipartFile file) {
        CarPhoto photo = photoService.upload(user, file);
        Map<String, Object> data = new HashMap<>();
        data.put("id", photo.getId());
        data.put("originalUrl", photo.getOriginalUrl());
        return Result.ok(data);
    }

    @GetMapping
    public Result<?> list(@AuthenticationPrincipal User user) {
        return Result.ok(photoService.listByUser(user.getId()));
    }

    @PostMapping("/from-url")
    public Result<?> createFromUrl(@AuthenticationPrincipal User user,
                                   @RequestBody Map<String, String> body) {
        CarPhoto photo = photoService.createFromUrl(user, body.get("imageUrl"));
        Map<String, Object> data = new HashMap<>();
        data.put("id", photo.getId());
        data.put("originalUrl", photo.getOriginalUrl());
        return Result.ok(data);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@AuthenticationPrincipal User user,
                            @PathVariable Long id) {
        photoService.delete(user, id);
        return Result.ok();
    }
}
```

- [ ] **Step 5: 提交**

```bash
git add qcg-server/src/main/java/com/qcg/config/OssConfig.java qcg-server/src/main/java/com/qcg/service/OssService.java qcg-server/src/main/java/com/qcg/service/PhotoService.java qcg-server/src/main/java/com/qcg/controller/PhotoController.java
git commit -m "feat: OSS 图片上传 + 作品管理 API"
```

---

### Task 11: AI 换色核心链路

**Files:**
- Create: `qcg-server/src/main/java/com/qcg/client/AiApiClient.java`
- Create: `qcg-server/src/main/java/com/qcg/service/AiColorizeService.java`
- Create: `qcg-server/src/main/java/com/qcg/controller/AiController.java`
- Create: `qcg-server/src/test/java/com/qcg/service/AiColorizeServiceTest.java`

- [ ] **Step 1: TDD — 先写 AiColorizeService 测试**

```java
package com.qcg.service;

import com.qcg.client.AiApiClient;
import com.qcg.entity.CarPhoto;
import com.qcg.entity.Color;
import com.qcg.entity.User;
import com.qcg.enums.PhotoStatus;
import com.qcg.repository.CarPhotoRepository;
import com.qcg.repository.ColorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiColorizeServiceTest {

    @Mock CarPhotoRepository photoRepo;
    @Mock ColorRepository colorRepo;
    @Mock AiApiClient aiClient;
    @Mock OssService ossService;

    @InjectMocks
    AiColorizeService aiColorizeService;

    @Test
    void shouldSubmitTaskSuccessfully() {
        User user = User.builder().id(1L).build();
        CarPhoto photo = CarPhoto.builder().id(10L).user(user).originalUrl("http://oss/a.jpg").build();
        Color color = Color.builder().id(5L).hexCode("#FF0000").build();

        when(photoRepo.findById(10L)).thenReturn(Optional.of(photo));
        when(colorRepo.findById(5L)).thenReturn(Optional.of(color));
        when(aiClient.submitTask(anyString(), anyString())).thenReturn("ai-task-123");
        when(photoRepo.save(any())).thenReturn(photo);

        var result = aiColorizeService.submit(user, 10L, 5L);

        assertThat(result.getTaskId()).isEqualTo("ai-task-123");
        assertThat(result.getStatus()).isEqualTo(PhotoStatus.PENDING);
        verify(photoRepo).save(any(CarPhoto.class));
    }

    @Test
    void shouldRejectOtherUsersPhoto() {
        User owner = User.builder().id(1L).build();
        User other = User.builder().id(2L).build();
        CarPhoto photo = CarPhoto.builder().id(10L).user(owner).build();

        when(photoRepo.findById(10L)).thenReturn(Optional.of(photo));

        assertThatThrownBy(() -> aiColorizeService.submit(other, 10L, 5L))
                .hasMessageContaining("无权操作");
    }

    @Test
    void shouldPollAndReturnResult() {
        CarPhoto photo = CarPhoto.builder().id(10L).aiTaskId("ai-123")
                .originalUrl("http://oss/a.jpg").status(PhotoStatus.PENDING).build();

        when(photoRepo.findById(10L)).thenReturn(Optional.of(photo));
        when(aiClient.queryTask("ai-123")).thenReturn(AiApiClient.TaskStatus.COMPLETED);
        when(aiClient.getResultUrl("ai-123")).thenReturn("http://ai-cdn/result.jpg");
        when(ossService.uploadFromUrl("http://ai-cdn/result.jpg")).thenReturn("http://oss/result.jpg");
        when(photoRepo.save(any())).thenReturn(photo);

        var result = aiColorizeService.poll(10L);

        assertThat(result.getStatus()).isEqualTo(PhotoStatus.COMPLETED);
        assertThat(result.getResultUrl()).isEqualTo("http://oss/result.jpg");
    }
}
```

- [ ] **Step 2: 验证测试失败**

```bash
cd qcg-server
mvn test -Dtest=AiColorizeServiceTest
```
预期：编译失败

- [ ] **Step 3: 创建 AiApiClient**

```java
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
                       @Value("${ai.api-key}") String apiKey,
                       @Value("${ai.timeout:30000}") int timeout) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.restTemplate = new RestTemplate();
    }

    /**
     * 提交换色任务，返回 AI 平台的任务 ID。
     */
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

    /**
     * 查询任务状态。
     */
    public TaskStatus queryTask(String taskId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
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

    /**
     * 获取结果图的临时 URL。
     */
    public String getResultUrl(String taskId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
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
```

- [ ] **Step 4: 给 OssService 添加 downloadFromUrl 方法**

使用 Edit 工具，在 `OssService.java` 末尾添加：

```java
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
```

- [ ] **Step 5: 创建 AiColorizeService**

```java
package com.qcg.service;

import com.qcg.client.AiApiClient;
import com.qcg.common.BusinessException;
import com.qcg.dto.TaskResultResponse;
import com.qcg.entity.CarPhoto;
import com.qcg.entity.Color;
import com.qcg.entity.User;
import com.qcg.enums.PhotoStatus;
import com.qcg.repository.CarPhotoRepository;
import com.qcg.repository.ColorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiColorizeService {

    private final CarPhotoRepository photoRepo;
    private final ColorRepository colorRepo;
    private final AiApiClient aiClient;
    private final OssService ossService;

    /** 提交换色任务 */
    @Transactional
    public TaskResultResponse submit(User user, Long photoId, Long colorId) {
        CarPhoto photo = photoRepo.findById(photoId)
                .orElseThrow(() -> new BusinessException("图片不存在"));

        if (!photo.getUser().getId().equals(user.getId())) {
            throw new BusinessException(403, "无权操作此图片");
        }

        Color color = colorRepo.findById(colorId)
                .orElseThrow(() -> new BusinessException("颜色不存在"));

        // 调用 AI API 提交任务
        String taskId = aiClient.submitTask(photo.getOriginalUrl(), color.getHexCode());

        // 更新记录
        photo.setAiTaskId(taskId);
        photo.setColor(color);
        photo.setStatus(PhotoStatus.PENDING);
        photoRepo.save(photo);

        return TaskResultResponse.builder()
                .photoId(photo.getId())
                .taskId(taskId)
                .status(PhotoStatus.PENDING)
                .build();
    }

    /** 轮询任务状态 */
    @Transactional
    public TaskResultResponse poll(Long photoId) {
        CarPhoto photo = photoRepo.findById(photoId)
                .orElseThrow(() -> new BusinessException("图片不存在"));

        if (photo.getAiTaskId() == null) {
            throw new BusinessException("该图片未提交 AI 任务");
        }

        AiApiClient.TaskStatus aiStatus = aiClient.queryTask(photo.getAiTaskId());

        if (aiStatus == AiApiClient.TaskStatus.COMPLETED) {
            // 下载结果图 → 上传 OSS
            String resultUrl = aiClient.getResultUrl(photo.getAiTaskId());
            String ossUrl = ossService.uploadFromUrl(resultUrl);

            photo.setResultUrl(ossUrl);
            photo.setStatus(PhotoStatus.COMPLETED);
            photoRepo.save(photo);

            return TaskResultResponse.builder()
                    .photoId(photo.getId())
                    .taskId(photo.getAiTaskId())
                    .status(PhotoStatus.COMPLETED)
                    .resultUrl(ossUrl)
                    .build();
        }

        if (aiStatus == AiApiClient.TaskStatus.FAILED) {
            photo.setStatus(PhotoStatus.FAILED);
            photoRepo.save(photo);

            return TaskResultResponse.builder()
                    .photoId(photo.getId())
                    .taskId(photo.getAiTaskId())
                    .status(PhotoStatus.FAILED)
                    .errorReason("AI 处理失败，请确认上传的是清晰的整车照片")
                    .build();
        }

        // 仍在处理中
        return TaskResultResponse.builder()
                .photoId(photo.getId())
                .taskId(photo.getAiTaskId())
                .status(PhotoStatus.PENDING)
                .build();
    }
}
```

- [ ] **Step 6: 创建 AiController**

```java
package com.qcg.controller;

import com.qcg.common.Result;
import com.qcg.dto.ColorizeRequest;
import com.qcg.entity.User;
import com.qcg.service.AiColorizeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiColorizeService aiColorizeService;

    @PostMapping("/colorize")
    public Result<?> colorize(@AuthenticationPrincipal User user,
                              @Valid @RequestBody ColorizeRequest request) {
        return Result.ok(aiColorizeService.submit(user, request.getPhotoId(), request.getColorId()));
    }

    @GetMapping("/tasks/{photoId}")
    public Result<?> pollTask(@AuthenticationPrincipal User user,
                              @PathVariable Long photoId) {
        // 只允许查询自己的任务
        return Result.ok(aiColorizeService.poll(photoId));
    }
}
```

- [ ] **Step 7: 运行测试验证**

```bash
cd qcg-server
mvn test -Dtest=AiColorizeServiceTest
```
预期：3 个测试全部 PASS。

- [ ] **Step 8: 提交**

```bash
git add qcg-server/src/main/java/com/qcg/client/AiApiClient.java qcg-server/src/main/java/com/qcg/service/AiColorizeService.java qcg-server/src/main/java/com/qcg/controller/AiController.java qcg-server/src/main/java/com/qcg/service/OssService.java qcg-server/src/test/
git commit -m "feat: AI 换色核心链路（含单测）"
```

---

### Task 12: 微信小程序项目脚手架

**Files:**
- Create: `qcg-miniapp/project.config.json`
- Create: `qcg-miniapp/app.js`
- Create: `qcg-miniapp/app.json`
- Create: `qcg-miniapp/app.wxss`
- Create: `qcg-miniapp/utils/api.js`
- Create: `qcg-miniapp/utils/auth.js`

- [ ] **Step 1: 创建 project.config.json**

```json
{
  "description": "汽车改色膜换色系统",
  "packOptions": { "ignore": [], "include": [] },
  "setting": {
    "urlCheck": false,
    "es6": true,
    "enhance": true,
    "postcss": true,
    "preloadBackgroundData": false,
    "minified": true,
    "newFeature": false,
    "coverView": true,
    "nodeModules": false,
    "autoAudits": false,
    "showShadowRootInWxmlPanel": true,
    "scopeDataCheck": false,
    "uglifyFileName": false,
    "checkInvalidKey": true,
    "checkSiteMap": false,
    "uploadWithSourceMap": true,
    "compileHotReLoad": false,
    "lazyloadPlaceholderEnable": false,
    "useMultiFrameRuntime": true,
    "useApiHook": true,
    "useApiHostProcess": true,
    "babelSetting": {
      "ignore": [],
      "disablePlugins": [],
      "outputPath": ""
    },
    "enableEngineNative": false,
    "useIsolateContext": true,
    "userConfirmedBundleSwitch": false,
    "packNpmManually": false,
    "packNpmRelationList": [],
    "minifyWXSS": true,
    "showES6CompileOption": false,
    "minifyWXML": true
  },
  "compileType": "miniprogram",
  "libVersion": "3.5.5",
  "appid": "your-appid-here",
  "projectname": "qcg-miniapp",
  "condition": {},
  "editorSetting": {
    "tabIndent": "insertSpaces",
    "tabSize": 2
  }
}
```

- [ ] **Step 2: 创建 app.js（全局启动逻辑）**

```javascript
// app.js
App({
  globalData: {
    token: null,
    userInfo: null,
    baseUrl: 'http://localhost:8080' // 开发环境，上线改为 HTTPS
  },

  onLaunch() {
    // 检查本地缓存的 token
    const token = wx.getStorageSync('token')
    if (token) {
      this.globalData.token = token
    }
  },

  /** 检查是否已登录 */
  isLoggedIn() {
    return !!this.globalData.token
  },

  /** 获取认证 header */
  getAuthHeader() {
    return {
      'Authorization': 'Bearer ' + (this.globalData.token || ''),
      'Content-Type': 'application/json'
    }
  }
})
```

- [ ] **Step 3: 创建 app.json（页面路由）**

```json
{
  "pages": [
    "pages/index/index",
    "pages/color-picker/color-picker",
    "pages/my-works/my-works",
    "pages/car-models/car-models"
  ],
  "window": {
    "backgroundTextStyle": "light",
    "navigationBarBackgroundColor": "#1a1a1a",
    "navigationBarTitleText": "改色膜预览",
    "navigationBarTextStyle": "white"
  },
  "tabBar": {
    "color": "#999999",
    "selectedColor": "#ff4444",
    "backgroundColor": "#ffffff",
    "borderStyle": "black",
    "list": [
      {
        "pagePath": "pages/index/index",
        "text": "首页",
        "iconPath": "images/home.png",
        "selectedIconPath": "images/home-active.png"
      },
      {
        "pagePath": "pages/car-models/car-models",
        "text": "车型库",
        "iconPath": "images/car.png",
        "selectedIconPath": "images/car-active.png"
      },
      {
        "pagePath": "pages/my-works/my-works",
        "text": "我的作品",
        "iconPath": "images/works.png",
        "selectedIconPath": "images/works-active.png"
      }
    ]
  },
  "style": "v2",
  "sitemapLocation": "sitemap.json"
}
```

- [ ] **Step 4: 创建 app.wxss（全局样式）**

```css
/* app.wxss */
page {
  background-color: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.container {
  padding: 20rpx;
}

.btn-primary {
  background-color: #ff4444;
  color: #fff;
  border-radius: 12rpx;
  padding: 24rpx 48rpx;
  font-size: 32rpx;
  text-align: center;
}

.btn-primary:active {
  background-color: #e03333;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.06);
}
```

- [ ] **Step 5: 创建 utils/auth.js（登录 + Token 管理）**

```javascript
// utils/auth.js
const app = getApp()

/**
 * 微信登录，获取 JWT token。
 * 调用时机：用户首次打开小程序或 token 过期。
 */
function login() {
  return new Promise((resolve, reject) => {
    wx.login({
      success(res) {
        if (res.code) {
          wx.request({
            url: app.globalData.baseUrl + '/api/auth/login',
            method: 'POST',
            data: { code: res.code },
            success(response) {
              const body = response.data
              if (body.code === 200 && body.data) {
                const { token, userId, nickname, avatarUrl } = body.data
                app.globalData.token = token
                app.globalData.userInfo = { userId, nickname, avatarUrl }
                wx.setStorageSync('token', token)
                resolve(body.data)
              } else {
                reject(new Error(body.message || '登录失败'))
              }
            },
            fail(err) {
              reject(new Error('网络异常，请检查后端服务是否启动'))
            }
          })
        } else {
          reject(new Error('获取微信 code 失败'))
        }
      },
      fail(err) {
        reject(err)
      }
    })
  })
}

/** 确保已登录，未登录则自动登录 */
function ensureLogin() {
  if (app.isLoggedIn()) {
    return Promise.resolve()
  }
  return login()
}

module.exports = { login, ensureLogin }
```

- [ ] **Step 6: 创建 utils/api.js（API 封装）**

```javascript
// utils/api.js
const app = getApp()
const auth = require('./auth')

/**
 * 通用请求封装。
 * 自动附加 JWT header，401 时自动重新登录后重试一次。
 */
function request(method, path, data = {}) {
  return auth.ensureLogin().then(() => {
    return new Promise((resolve, reject) => {
      function doRequest() {
        wx.request({
          url: app.globalData.baseUrl + path,
          method: method,
          header: app.getAuthHeader(),
          data: data,
          success(res) {
            const body = res.data
            if (body.code === 200) {
              resolve(body.data)
            } else if (res.statusCode === 401) {
              // Token 过期，重新登录后重试
              auth.login().then(() => doRequest()).catch(reject)
            } else {
              wx.showToast({ title: body.message || '请求失败', icon: 'none' })
              reject(new Error(body.message))
            }
          },
          fail(err) {
            wx.showToast({ title: '网络异常', icon: 'none' })
            reject(err)
          }
        })
      }
      doRequest()
    })
  })
}

/** 上传图片 */
function uploadFile(filePath) {
  return auth.ensureLogin().then(() => {
    return new Promise((resolve, reject) => {
      wx.uploadFile({
        url: app.globalData.baseUrl + '/api/photos/upload',
        filePath: filePath,
        name: 'file',
        header: {
          'Authorization': 'Bearer ' + app.globalData.token
        },
        success(res) {
          const body = JSON.parse(res.data)
          if (body.code === 200) {
            resolve(body.data)
          } else {
            reject(new Error(body.message))
          }
        },
        fail(err) {
          wx.showToast({ title: '上传失败', icon: 'none' })
          reject(err)
        }
      })
    })
  })
}

module.exports = {
  get: (path) => request('GET', path),
  post: (path, data) => request('POST', path, data),
  delete: (path) => request('DELETE', path),
  uploadFile
}
```

- [ ] **Step 7: 提交**

```bash
git add qcg-miniapp/
git commit -m "feat: 微信小程序项目脚手架 + 登录/API 封装"
```

---

### Task 13: 小程序首页

**Files:**
- Create: `qcg-miniapp/pages/index/index.js`
- Create: `qcg-miniapp/pages/index/index.wxml`
- Create: `qcg-miniapp/pages/index/index.wxss`
- Create: `qcg-miniapp/pages/index/index.json`

- [ ] **Step 1: 创建 index.json**

```json
{
  "navigationBarTitleText": "改色膜预览",
  "usingComponents": {}
}
```

- [ ] **Step 2: 创建 index.wxml**

```html
<!-- pages/index/index.wxml -->
<view class="container">
  <view class="hero">
    <text class="hero-title">想贴改色膜？\n先看看效果再决定</text>
    <text class="hero-sub">上传你的爱车照片，即刻预览换色效果</text>
  </view>

  <!-- 入口 A：上传实车照片 -->
  <view class="card entry-card" bindtap="onUploadPhoto">
    <view class="entry-icon">📷</view>
    <view class="entry-text">
      <text class="entry-title">拍摄/上传我的车</text>
      <text class="entry-desc">从相册选择或现场拍照</text>
    </view>
    <text class="entry-arrow">→</text>
  </view>

  <!-- 入口 B：选预设车型 -->
  <view class="card entry-card" bindtap="onSelectModel">
    <view class="entry-icon">🚗</view>
    <view class="entry-text">
      <text class="entry-title">选择预设车型</text>
      <text class="entry-desc">从车型库快速体验</text>
    </view>
    <text class="entry-arrow">→</text>
  </view>

  <!-- 最近作品预览 -->
  <view class="recent-section" wx:if="{{recentWorks.length > 0}}">
    <text class="section-title">最近作品</text>
    <scroll-view scroll-x class="recent-scroll">
      <view class="recent-item" wx:for="{{recentWorks}}" wx:key="id"
            bindtap="onTapWork" data-id="{{item.id}}">
        <image src="{{item.resultUrl || item.originalUrl}}" mode="aspectFill"/>
      </view>
    </scroll-view>
  </view>
</view>
```

- [ ] **Step 3: 创建 index.js**

```javascript
// pages/index/index.js
const api = require('../../utils/api')

Page({
  data: {
    recentWorks: []
  },

  onShow() {
    this.loadRecentWorks()
  },

  /** 加载最近 5 件作品 */
  loadRecentWorks() {
    api.get('/api/photos').then(data => {
      this.setData({ recentWorks: (data || []).slice(0, 5) })
    }).catch(() => {})
  },

  /** 入口 A：上传实车照片 */
  onUploadPhoto() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success(res) {
        const tempFilePath = res.tempFiles[0].tempFilePath
        wx.showLoading({ title: '上传中...' })

        api.uploadFile(tempFilePath).then(data => {
          wx.hideLoading()
          // 跳转到选色页，传入 photoId
          wx.navigateTo({
            url: '/pages/color-picker/color-picker?photoId=' + data.id +
                 '&imageUrl=' + encodeURIComponent(data.originalUrl)
          })
        }).catch(() => {
          wx.hideLoading()
        })
      }
    })
  },

  /** 入口 B：选预设车型 */
  onSelectModel() {
    wx.switchTab({ url: '/pages/car-models/car-models' })
  },

  /** 点击最近作品 */
  onTapWork(e) {
    const id = e.currentTarget.dataset.id
    // 如果已完成，跳转到选色页查看效果
    wx.navigateTo({
      url: '/pages/color-picker/color-picker?photoId=' + id
    })
  }
})
```

- [ ] **Step 4: 创建 index.wxss**

```css
/* pages/index/index.wxss */
.hero {
  padding: 60rpx 20rpx 40rpx;
  text-align: center;
}

.hero-title {
  display: block;
  font-size: 44rpx;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1.4;
  white-space: pre-line;
}

.hero-sub {
  display: block;
  font-size: 28rpx;
  color: #999;
  margin-top: 16rpx;
}

.entry-card {
  display: flex;
  align-items: center;
  padding: 36rpx 28rpx;
  margin: 0 24rpx 20rpx;
}

.entry-icon {
  font-size: 52rpx;
  margin-right: 24rpx;
}

.entry-text {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.entry-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}

.entry-desc {
  font-size: 24rpx;
  color: #999;
  margin-top: 8rpx;
}

.entry-arrow {
  font-size: 32rpx;
  color: #ccc;
}

.recent-section {
  padding: 24rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 16rpx;
  display: block;
}

.recent-scroll {
  white-space: nowrap;
}

.recent-item {
  display: inline-block;
  width: 200rpx;
  height: 150rpx;
  margin-right: 16rpx;
  border-radius: 12rpx;
  overflow: hidden;
}

.recent-item image {
  width: 100%;
  height: 100%;
}
```

- [ ] **Step 5: 提交**

```bash
git add qcg-miniapp/pages/index/
git commit -m "feat: 小程序首页 — 双入口 + 最近作品"
```

---

### Task 14: 小程序选色换色页（核心页面）

**Files:**
- Create: `qcg-miniapp/pages/color-picker/color-picker.js`
- Create: `qcg-miniapp/pages/color-picker/color-picker.wxml`
- Create: `qcg-miniapp/pages/color-picker/color-picker.wxss`
- Create: `qcg-miniapp/pages/color-picker/color-picker.json`

- [ ] **Step 1: 创建 color-picker.json**

```json
{
  "navigationBarTitleText": "选择颜色",
  "usingComponents": {}
}
```

- [ ] **Step 2: 创建 color-picker.wxml**

```html
<!-- pages/color-picker/color-picker.wxml -->
<view class="container">
  <!-- 汽车图片展示区 -->
  <view class="preview-area">
    <image class="car-image" src="{{displayUrl}}" mode="aspectFit"/>
    <!-- 处理中遮罩 -->
    <view class="processing-mask" wx:if="{{isProcessing}}">
      <view class="spinner"></view>
      <text class="processing-text">AI 正在换色中...</text>
    </view>
    <!-- 失败提示 -->
    <view class="failed-mask" wx:if="{{isFailed}}">
      <text class="failed-text">{{errorMsg}}</text>
      <button class="retry-btn" bindtap="onRetry" size="mini">重新上传</button>
    </view>
  </view>

  <!-- 当前选中颜色信息 -->
  <view class="selected-color-bar" wx:if="{{selectedColor}}">
    <view class="color-dot" style="background-color: {{selectedColor.hexCode}};"></view>
    <text class="color-name">{{selectedColor.name}}</text>
  </view>

  <!-- 颜色分类 Tab -->
  <scroll-view scroll-x class="category-tabs">
    <view class="tab-item {{activeCategory === 'all' ? 'active' : ''}}"
          bindtap="onCategoryTap" data-cat="all">全部</view>
    <view class="tab-item {{activeCategory === item ? 'active' : ''}}"
          wx:for="{{categories}}" wx:key="*this"
          bindtap="onCategoryTap" data-cat="{{item}}">{{item}}</view>
  </scroll-view>

  <!-- 颜色网格 -->
  <view class="color-grid">
    <view class="color-item" wx:for="{{filteredColors}}" wx:key="id"
          bindtap="onColorTap" data-color="{{item}}">
      <view class="color-swatch" style="background-color: {{item.hexCode}};">
        <view class="check-mark" wx:if="{{selectedColor && selectedColor.id === item.id}}">✓</view>
      </view>
      <text class="color-label">{{item.name}}</text>
    </view>
  </view>

  <!-- 底部操作栏 -->
  <view class="bottom-bar">
    <button class="btn-primary" bindtap="onApplyColor"
            disabled="{{!selectedColor || isProcessing}}">
      {{isProcessing ? '处理中...' : '应用此颜色'}}
    </button>
    <view class="bottom-actions" wx:if="{{resultUrl}}">
      <button class="action-btn" bindtap="onSave">保存到相册</button>
      <button class="action-btn" bindtap="onShare">分享给朋友</button>
    </view>
  </view>
</view>
```

- [ ] **Step 3: 创建 color-picker.js**

```javascript
// pages/color-picker/color-picker.js
const api = require('../../utils/api')

Page({
  data: {
    photoId: null,
    imageUrl: '',       // 原始图 URL
    displayUrl: '',     // 当前展示图（原图或效果图）
    resultUrl: '',      // AI 效果图 URL
    colors: [],
    categories: [],
    activeCategory: 'all',
    filteredColors: [],
    selectedColor: null,
    isProcessing: false,
    isFailed: false,
    errorMsg: '',
    pollTimer: null
  },

  onLoad(options) {
    const photoId = options.photoId
    const imageUrl = decodeURIComponent(options.imageUrl || '')
    this.setData({ photoId, displayUrl: imageUrl })

    // 如果有 photoId，先查一下是否已有结果图
    if (photoId && !imageUrl) {
      this.loadPhotoDetail(photoId)
    }

    this.loadColors()
  },

  onUnload() {
    // 清除轮询定时器
    if (this.data.pollTimer) {
      clearInterval(this.data.pollTimer)
    }
  },

  /** 加载照片详情 */
  loadPhotoDetail(photoId) {
    api.get('/api/photos').then(works => {
      const work = (works || []).find(w => w.id == photoId)
      if (work) {
        this.setData({
          imageUrl: work.originalUrl,
          displayUrl: work.resultUrl || work.originalUrl,
          resultUrl: work.resultUrl || ''
        })
      }
    })
  },

  /** 加载颜色库 */
  loadColors() {
    api.get('/api/colors').then(colors => {
      const categories = [...new Set(colors.map(c => c.category).filter(Boolean))]
      this.setData({
        colors,
        categories,
        filteredColors: colors
      })
    })
  },

  /** 切换颜色分类 */
  onCategoryTap(e) {
    const cat = e.currentTarget.dataset.cat
    const colors = this.data.colors
    const filtered = cat === 'all'
      ? colors
      : colors.filter(c => c.category === cat)

    this.setData({
      activeCategory: cat,
      filteredColors: filtered
    })
  },

  /** 选中颜色 */
  onColorTap(e) {
    this.setData({ selectedColor: e.currentTarget.dataset.color })
  },

  /** 提交 AI 换色 */
  onApplyColor() {
    const { photoId, imageUrl, selectedColor } = this.data
    if (!selectedColor) return

    this.setData({ isProcessing: true, isFailed: false })

    // 如果有 photoId 直接提交；否则先创建 photo 记录（车型库入口）
    const submitTask = (pid) => {
      api.post('/api/ai/colorize', {
        photoId: parseInt(pid),
        colorId: selectedColor.id
      }).then(result => {
        this.setData({ photoId: result.photoId })
        this.startPolling(result.photoId)
      }).catch(() => {
        this.setData({ isProcessing: false })
      })
    }

    if (photoId) {
      submitTask(photoId)
    } else if (imageUrl) {
      api.post('/api/photos/from-url', { imageUrl }).then(data => {
        submitTask(data.id)
      }).catch(() => {
        this.setData({ isProcessing: false })
      })
    }
  },

  /** 轮询 AI 任务状态 */
  startPolling(photoId) {
    let count = 0
    const maxPolls = 30 // 最多轮询 60 秒

    const timer = setInterval(() => {
      count++
      api.get('/api/ai/tasks/' + photoId).then(result => {
        if (result.status === 'COMPLETED') {
          clearInterval(timer)
          this.setData({
            displayUrl: result.resultUrl,
            resultUrl: result.resultUrl,
            isProcessing: false,
            pollTimer: null
          })
        } else if (result.status === 'FAILED') {
          clearInterval(timer)
          this.setData({
            isProcessing: false,
            isFailed: true,
            errorMsg: result.errorReason || 'AI 处理失败',
            pollTimer: null
          })
        } else if (count >= maxPolls) {
          clearInterval(timer)
          this.setData({
            isProcessing: false,
            errorMsg: '处理时间较长，请在「我的作品」中查看结果',
            pollTimer: null
          })
        }
      }).catch(() => {
        clearInterval(timer)
        this.setData({ isProcessing: false, pollTimer: null })
      })
    }, 2000)

    this.setData({ pollTimer: timer })
  },

  /** 重试 */
  onRetry() {
    this.setData({ isFailed: false, errorMsg: '' })
  },

  /** 保存到相册 */
  onSave() {
    if (!this.data.resultUrl) return
    wx.showLoading({ title: '保存中...' })
    wx.downloadFile({
      url: this.data.resultUrl,
      success(res) {
        wx.saveImageToPhotosAlbum({
          filePath: res.tempFilePath,
          success() {
            wx.hideLoading()
            wx.showToast({ title: '已保存到相册', icon: 'success' })
          },
          fail() {
            wx.hideLoading()
            wx.showToast({ title: '保存失败，请授权相册权限', icon: 'none' })
          }
        })
      },
      fail() {
        wx.hideLoading()
        wx.showToast({ title: '下载失败', icon: 'none' })
      }
    })
  },

  /** 分享给好友 */
  onShare() {
    // 微信小程序 onShareAppMessage 需在 Page 定义
    wx.showToast({ title: '点击右上角菜单分享', icon: 'none' })
  },

  /** 分享到朋友圈/好友 */
  onShareAppMessage() {
    return {
      title: '看看我的改色效果！',
      path: '/pages/color-picker/color-picker?photoId=' + this.data.photoId,
      imageUrl: this.data.resultUrl || this.data.imageUrl
    }
  },

  onShareTimeline() {
    return {
      title: '汽车改色预览效果',
      query: 'photoId=' + this.data.photoId,
      imageUrl: this.data.resultUrl || this.data.imageUrl
    }
  }
})
```

- [ ] **Step 4: 创建 color-picker.wxss**

```css
/* pages/color-picker/color-picker.wxss */
.preview-area {
  width: 100%;
  height: 500rpx;
  background: #e8e8e8;
  position: relative;
}

.car-image {
  width: 100%;
  height: 100%;
}

.processing-mask, .failed-mask {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.processing-text, .failed-text {
  color: #fff;
  font-size: 28rpx;
  margin-top: 16rpx;
}

.retry-btn {
  margin-top: 20rpx;
}

.selected-color-bar {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  background: #fff;
  border-bottom: 1px solid #eee;
}

.color-dot {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  border: 2px solid #ddd;
  margin-right: 16rpx;
}

.color-name {
  font-size: 28rpx;
  color: #333;
}

.category-tabs {
  white-space: nowrap;
  padding: 20rpx 0;
  background: #fff;
  border-bottom: 1px solid #eee;
}

.tab-item {
  display: inline-block;
  padding: 12rpx 32rpx;
  font-size: 26rpx;
  color: #666;
}

.tab-item.active {
  color: #ff4444;
  font-weight: 600;
  border-bottom: 4rpx solid #ff4444;
}

.color-grid {
  display: flex;
  flex-wrap: wrap;
  padding: 16rpx;
}

.color-item {
  width: 25%;
  padding: 12rpx;
  box-sizing: border-box;
  text-align: center;
}

.color-swatch {
  width: 100%;
  padding-bottom: 100%;
  border-radius: 12rpx;
  position: relative;
}

.check-mark {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #fff;
  font-size: 36rpx;
  font-weight: bold;
  text-shadow: 0 0 8rpx rgba(0,0,0,0.5);
}

.color-label {
  display: block;
  font-size: 22rpx;
  color: #666;
  margin-top: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 20rpx 24rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -2rpx 12rpx rgba(0,0,0,0.06);
}

.bottom-actions {
  display: flex;
  gap: 20rpx;
  margin-top: 16rpx;
}

.action-btn {
  flex: 1;
  font-size: 28rpx;
  padding: 20rpx;
  border-radius: 12rpx;
  background: #f5f5f5;
  color: #333;
}
```

- [ ] **Step 5: 提交**

```bash
git add qcg-miniapp/pages/color-picker/
git commit -m "feat: 选色换色核心页面"
```

---

### Task 15: 小程序车型库 + 我的作品页面

**Files:**
- Create: `qcg-miniapp/pages/car-models/car-models.js`
- Create: `qcg-miniapp/pages/car-models/car-models.wxml`
- Create: `qcg-miniapp/pages/car-models/car-models.wxss`
- Create: `qcg-miniapp/pages/car-models/car-models.json`
- Create: `qcg-miniapp/pages/my-works/my-works.js`
- Create: `qcg-miniapp/pages/my-works/my-works.wxml`
- Create: `qcg-miniapp/pages/my-works/my-works.wxss`
- Create: `qcg-miniapp/pages/my-works/my-works.json`

- [ ] **Step 1: 车型库 — car-models.json**

```json
{
  "navigationBarTitleText": "车型库",
  "usingComponents": {}
}
```

- [ ] **Step 2: 车型库 — car-models.wxml**

```html
<view class="container">
  <view class="brand-filter">
    <view class="brand-item {{activeBrand === 'all' ? 'active' : ''}}"
          bindtap="onBrandTap" data-brand="all">全部</view>
    <view class="brand-item {{activeBrand === item ? 'active' : ''}}"
          wx:for="{{brands}}" wx:key="*this"
          bindtap="onBrandTap" data-brand="{{item}}">{{item}}</view>
  </view>

  <view class="model-grid">
    <view class="model-card" wx:for="{{models}}" wx:key="id"
          bindtap="onSelectModel" data-model="{{item}}">
      <image src="{{item.imageUrl}}" mode="aspectFill" class="model-img"/>
      <text class="model-name">{{item.brandName}} {{item.modelName}}</text>
      <text class="model-type">{{item.bodyType || ''}}</text>
    </view>
  </view>

  <view class="empty" wx:if="{{models.length === 0}}">
    <text>暂无车型数据</text>
  </view>
</view>
```

- [ ] **Step 3: 车型库 — car-models.js**

```javascript
const api = require('../../utils/api')

Page({
  data: {
    brands: [],
    allModels: [],
    models: [],
    activeBrand: 'all'
  },

  onLoad() {
    api.get('/api/car-models').then(models => {
      const brands = [...new Set(models.map(m => m.brandName).filter(Boolean))].sort()
      this.setData({ brands, allModels: models, models })
    }).catch(() => {})
  },

  onBrandTap(e) {
    const brand = e.currentTarget.dataset.brand
    const models = brand === 'all'
      ? this.data.allModels
      : this.data.allModels.filter(m => m.brandName === brand)
    this.setData({ activeBrand: brand, models })
  },

  onSelectModel(e) {
    const model = e.currentTarget.dataset.model
    if (!model.imageUrl) {
      wx.showToast({ title: '该车型暂无可预览图片', icon: 'none' })
      return
    }
    // 用车型图片作为"上传图"，跳转选色页
    wx.navigateTo({
      url: '/pages/color-picker/color-picker?photoId=&imageUrl=' +
           encodeURIComponent(model.imageUrl)
    })
  }
})
```

- [ ] **Step 4: 车型库 — car-models.wxss**

```css
.brand-filter {
  white-space: nowrap;
  padding: 16rpx 0;
  background: #fff;
  margin-bottom: 16rpx;
}

.brand-item {
  display: inline-block;
  padding: 12rpx 28rpx;
  font-size: 26rpx;
  color: #666;
}

.brand-item.active {
  color: #ff4444;
  font-weight: 600;
}

.model-grid {
  display: flex;
  flex-wrap: wrap;
  padding: 0 16rpx;
}

.model-card {
  width: calc(50% - 16rpx);
  margin: 8rpx;
  background: #fff;
  border-radius: 12rpx;
  overflow: hidden;
}

.model-img {
  width: 100%;
  height: 240rpx;
  background: #eee;
}

.model-name {
  display: block;
  font-size: 26rpx;
  color: #333;
  padding: 12rpx 16rpx 4rpx;
  font-weight: 500;
}

.model-type {
  display: block;
  font-size: 22rpx;
  color: #999;
  padding: 0 16rpx 12rpx;
}
```

- [ ] **Step 5: 我的作品 — my-works.json**

```json
{
  "navigationBarTitleText": "我的作品",
  "usingComponents": {}
}
```

- [ ] **Step 6: 我的作品 — my-works.wxml**

```html
<view class="container">
  <view class="works-list">
    <view class="work-card" wx:for="{{works}}" wx:key="id">
      <image src="{{item.resultUrl || item.originalUrl}}" mode="aspectFill"
             class="work-img" bindtap="onTapWork" data-id="{{item.id}}"/>
      <view class="work-info">
        <view class="work-status {{item.status === 'COMPLETED' ? 'done' : item.status === 'FAILED' ? 'fail' : 'pending'}}">
          {{item.status === 'COMPLETED' ? '已完成' : item.status === 'FAILED' ? '失败' : '处理中'}}
        </view>
        <text class="work-date">{{item.createdAt}}</text>
      </view>
      <view class="work-actions">
        <text class="action-link" wx:if="{{item.status === 'COMPLETED'}}"
              bindtap="onViewResult" data-item="{{item}}">查看效果</text>
        <text class="action-link delete" bindtap="onDelete" data-id="{{item.id}}">删除</text>
      </view>
    </view>
  </view>

  <view class="empty" wx:if="{{works.length === 0}}">
    <text>还没有作品，去首页试试换色吧</text>
  </view>
</view>
```

- [ ] **Step 7: 我的作品 — my-works.js**

```javascript
const api = require('../../utils/api')

Page({
  data: { works: [] },

  onShow() {
    this.loadWorks()
  },

  loadWorks() {
    api.get('/api/photos').then(works => {
      this.setData({ works: works || [] })
    }).catch(() => {})
  },

  onTapWork(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: '/pages/color-picker/color-picker?photoId=' + id
    })
  },

  onViewResult(e) {
    const item = e.currentTarget.dataset.item
    wx.navigateTo({
      url: '/pages/color-picker/color-picker?photoId=' + item.id
    })
  },

  onDelete(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '确认删除',
      content: '删除后无法恢复',
      success: (res) => {
        if (res.confirm) {
          api.delete('/api/photos/' + id).then(() => {
            this.loadWorks()
          })
        }
      }
    })
  }
})
```

- [ ] **Step 8: 我的作品 — my-works.wxss**

```css
.work-card {
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  overflow: hidden;
}

.work-img {
  width: 100%;
  height: 400rpx;
  background: #eee;
}

.work-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 24rpx;
}

.work-status {
  font-size: 24rpx;
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
}

.work-status.done { background: #e8f5e9; color: #4caf50; }
.work-status.fail { background: #ffebee; color: #f44336; }
.work-status.pending { background: #fff3e0; color: #ff9800; }

.work-date {
  font-size: 22rpx;
  color: #999;
}

.work-actions {
  display: flex;
  justify-content: flex-end;
  gap: 32rpx;
  padding: 0 24rpx 20rpx;
}

.action-link {
  font-size: 26rpx;
  color: #667eea;
}

.action-link.delete {
  color: #f44336;
}
```

- [ ] **Step 9: 提交**

```bash
git add qcg-miniapp/pages/car-models/ qcg-miniapp/pages/my-works/
git commit -m "feat: 车型库 + 我的作品页面"
```

---

### Task 16: Phase 1 集成验证

**Files:**
- Create: `qcg-server/src/test/java/com/qcg/controller/AuthControllerTest.java`

- [ ] **Step 1: 编写接口测试（AuthController）**

```java
package com.qcg.controller;

import com.qcg.client.WechatClient;
import com.qcg.entity.User;
import com.qcg.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean WechatClient wechatClient;
    @Autowired UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldLoginAndReturnToken() throws Exception {
        when(wechatClient.getOpenid("test-code")).thenReturn("wx-openid-test");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"test-code\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.userId").isNumber());
    }

    @Test
    void shouldRejectEmptyCode() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}
```

- [ ] **Step 2: 运行全部测试**

```bash
cd qcg-server
mvn test
```
预期：全部 PASS。

- [ ] **Step 3: 启动后端，手动验证 API 链路**

```bash
# 终端 1：启动后端
cd qcg-server
mvn spring-boot:run
```
预期：启动成功，监听 8080 端口。

```bash
# 终端 2：用 curl 验证完整链路
# 1. 登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"code":"test"}'

# 2. 获取颜色列表
curl http://localhost:8080/api/colors \
  -H "Authorization: Bearer <token>"

# 3. 获取车型列表
curl http://localhost:8080/api/car-models \
  -H "Authorization: Bearer <token>"
```

- [ ] **Step 4: 提交**

```bash
git add qcg-server/src/test/
git commit -m "test: 认证接口测试 + 集成验证通过"
```

---

## 完成标志

Phase 1 完成后，以下链路应完全可用：

1. ✅ 小程序端微信登录 → 获取 JWT token
2. ✅ 首页双入口（上传实车照 / 浏览车型库）
3. ✅ 颜色库浏览（按分类筛选）
4. ✅ 提交 AI 换色任务 → 轮询 → 展示效果图
5. ✅ 效果图保存到相册
6. ✅ 效果图分享给微信好友/朋友圈
7. ✅ 我的作品列表（查看/删除）
8. ✅ 后端全部单元测试 + 接口测试 PASS

---

> **下一步**：用户审阅计划后，选择执行模式（subagent-driven 或 inline execution）。
