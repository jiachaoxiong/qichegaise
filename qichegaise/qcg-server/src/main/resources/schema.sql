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
