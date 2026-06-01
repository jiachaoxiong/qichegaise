# 汽车改色膜换色系统 — 设计规格说明书

> 日期：2026-06-01
> 状态：设计完成，待用户审阅

---

## 1. 项目概述

汽车改色膜换色系统（qichegaise）是一个微信小程序应用，允许车主上传汽车照片、通过 AI 云端技术预览不同颜色改色膜的效果，并保存/分享效果图。后续阶段逐步加入门店生态和在线预约功能。

### 1.1 核心价值

- **车主**：在贴膜前就能看到真实换色效果，降低决策风险
- **门店**：获得展示窗口和获客渠道
- **平台运营**：连接车主和门店，形成交易闭环

### 1.2 技术栈

| 层级 | 技术 | 版本建议 |
|------|------|----------|
| 小程序前端 | 微信小程序原生框架 | — |
| 后端框架 | Java / Spring Boot | 3.x |
| 数据库 | MySQL | 8.0 |
| ORM | Spring Data JPA / Hibernate | — |
| 安全框架 | Spring Security + JWT | — |
| 对象存储 | 阿里云 OSS / 腾讯云 COS | — |
| AI 图像处理 | 云端 AI API（待具体选型） | — |
| 管理后台前端 | Vue 3 + Element Plus | — |

---

## 2. 分阶段交付计划

### Phase 1：换色工具核心（MVP）

**目标**：用户能完成"上传/选车 → AI 换色 → 保存分享"的完整闭环。

- 微信授权登录
- 上传汽车照片（拍照 / 相册）
- 预设车型库浏览
- 内置颜色库（~100 色）
- AI 车身分割 + 颜色渲染
- 保存效果图到"我的作品"
- 分享到微信好友 / 朋友圈 / 生成海报

### Phase 2：门店生态

**目标**：贴膜门店入驻平台，上传案例，吸引车主。

- 门店注册/入驻（关联 user 表，role=shop）
- 门店主页（名称、地址、电话、封面）
- 上传施工案例（Before/After 对比图）
- 品牌色卡数据对接
- 车主浏览门店和案例
- 收藏门店

### Phase 3：预约 + 管理后台

**目标**：线上预约连接车主和门店，Web 后台运营管理。

**小程序新增：**
- 预约门店功能（选门店 → 选颜色 → 选时间 → 提交）
- 查看预约状态
- 门店端查看/管理预约列表

**Web 管理后台 (Vue 3)：**
- 颜色 CRUD 管理
- 车型库管理
- 门店入驻审核
- 案例内容审核
- 用户管理
- 预约管理
- 数据统计看板

---

## 3. 系统架构

```
┌─────────────────────────────────────────────┐
│              微信小程序 (C端)                  │
│  Phase1:换色工具 | Phase2:门店 | Phase3:预约   │
└──────────────────┬──────────────────────────┘
                   │ HTTPS / REST API
┌──────────────────▼──────────────────────────┐
│           Spring Boot 后端服务                │
│  用户模块 │ 图片模块 │ AI代理 │ 资源模块        │
│  门店模块 │ 预约模块 │ 管理后台模块             │
└──────┬──────────────┬────────────┬───────────┘
       │              │            │
  ┌────▼────┐   ┌────▼────┐  ┌───▼──────┐
  │  MySQL   │   │  OSS    │  │ AI API   │
  │  (数据)   │   │ (图片)  │  │ (换色)   │
  └─────────┘   └─────────┘  └──────────┘
       ▲
┌──────┴──────────────────────────────────────┐
│        Vue 3 管理后台 (Web端, Phase 3)         │
│    颜色管理 │ 门店审核 │ 数据统计               │
└─────────────────────────────────────────────┘
```

### 3.1 设计原则

- **Phase 1 建全表**：所有表在 Phase 1 建好，后续只加字段不拆表
- **扩展预留**：user.role 支持多角色，color.brand_id 预留品牌关联
- **前后端分离**：小程序和后台通过 REST API 通信，JWT 鉴权
- **异步优先**：AI 任务异步处理，不阻塞用户操作

---

## 4. 数据模型

### 4.1 表结构总览

| 表名 | 阶段 | 说明 |
|------|------|------|
| user | P1 | 统一用户表（车主/门店主/管理员） |
| car_model | P1 | 预设车型库 |
| color | P1 | 颜色库（含品牌色卡关联） |
| brand | P1 | 改色膜品牌 |
| car_photo | P1 | 用户上传的车照和 AI 效果图 |
| shop | P1(预留) | 门店信息，Phase 2 启用 |
| shop_case | P1(预留) | 门店施工案例，Phase 2 启用 |
| appointment | P1(预留) | 预约记录，Phase 3 启用 |

### 4.2 字段明细

**user**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| openid | varchar(64) | 微信 openid，唯一 |
| nickname | varchar(64) | 微信昵称 |
| avatar_url | varchar(512) | 头像 URL |
| phone | varchar(20) | 手机号 |
| role | enum('user','shop','admin') | 角色，默认 user |
| created_at | datetime | 注册时间 |

**car_model**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| brand_name | varchar(32) | 品牌（如 奔驰） |
| model_name | varchar(64) | 车型（如 C260L） |
| year | varchar(8) | 年款 |
| body_type | varchar(16) | 车型类别（SUV/轿车/MPV/跑车） |
| image_url | varchar(512) | 车型展示图 |
| is_active | tinyint | 是否启用 |

**color**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| name | varchar(32) | 颜色名称 |
| hex_code | varchar(7) | 色值，如 #FF0000 |
| category | varchar(32) | 分类（哑光/亮光/金属/珠光/渐变） |
| brand_id | bigint, nullable | 关联品牌（Phase 2） |
| is_active | tinyint | 是否启用 |

**brand**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| name | varchar(64) | 品牌名（3M、艾利等） |
| logo_url | varchar(512) | 品牌 Logo |
| website | varchar(256) | 官网 |

**car_photo**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| user_id | bigint | 关联用户 |
| car_model_id | bigint, nullable | 关联预设车型（可为空） |
| original_url | varchar(512) | 原始图片 URL（OSS） |
| result_url | varchar(512) | 效果图 URL（OSS） |
| color_id | bigint | 选择的颜色 |
| ai_task_id | varchar(128) | AI 任务 ID |
| status | enum('pending','completed','failed') | 处理状态 |
| created_at | datetime | 创建时间 |

**shop** (Phase 2 启用)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| name | varchar(64) | 门店名称 |
| address | varchar(256) | 详细地址 |
| lat | decimal(10,7) | 纬度 |
| lng | decimal(10,7) | 经度 |
| phone | varchar(20) | 联系电话 |
| cover_url | varchar(512) | 门头照 |
| description | text | 门店介绍 |
| status | enum('pending','approved','rejected') | 审核状态 |
| owner_user_id | bigint | 店主关联 user |

**shop_case** (Phase 2 启用)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| shop_id | bigint | 关联门店 |
| before_url | varchar(512) | 施工前照片 |
| after_url | varchar(512) | 施工后照片 |
| color_id | bigint | 使用的颜色 |
| car_model | varchar(64) | 施工车型（文本描述） |
| description | text | 案例说明 |
| likes | int | 点赞数 |

**appointment** (Phase 3 启用)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| user_id | bigint | 预约用户 |
| shop_id | bigint | 预约门店 |
| car_photo_id | bigint, nullable | 关联效果图 |
| color_id | bigint | 意向颜色 |
| appointment_time | datetime | 预约时间 |
| status | enum('pending','confirmed','cancelled','completed') | 状态 |
| remark | varchar(256) | 备注 |

---

## 5. Phase 1 API 设计

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | /api/auth/login | 微信 code 换取 JWT token | 否 |
| GET | /api/users/me | 当前用户信息 | 是 |
| GET | /api/colors | 颜色列表（分页、按分类/品牌筛选） | 是 |
| GET | /api/colors/:id | 颜色详情 | 是 |
| GET | /api/car-models | 车型列表（按品牌/车系筛选） | 是 |
| POST | /api/photos/upload | 上传车照 | 是 |
| POST | /api/ai/colorize | 提交 AI 换色任务 | 是 |
| GET | /api/ai/tasks/:id | 查询 AI 任务进度 | 是 |
| GET | /api/photos | 我的作品列表 | 是 |
| DELETE | /api/photos/:id | 删除作品 | 是 |

### 5.1 AI 换色异步流程

1. 小程序调用 `POST /api/ai/colorize`，传入 photo_id 和 color_id
2. 后端保存记录，调用云端 AI API 提交任务，获得 task_id
3. 后端立即返回 `{task_id, status: "pending"}`
4. 小程序每 2 秒调用 `GET /api/ai/tasks/:id` 轮询
5. 状态流转：`pending → completed` 或 `pending → failed`
6. completed 时返回 `result_url`，小程序展示效果图
7. 轮询超时（60s）后停止，提示用户稍后在"我的作品"查看

### 5.2 AI 任务状态机

```
pending ──→ completed
pending ──→ failed
```

- **pending**：任务已创建，等待 AI 处理
- **completed**：成功，result_url 已就绪
- **failed**：失败，返回 error_reason（无法识别车身 / AI 超时 / 服务异常）

---

## 6. 错误处理

| 场景 | HTTP 状态码 | 后端处理 | 小程序表现 |
|------|------------|----------|-----------|
| 图片格式/大小不合法 | 400 | 校验 MIME 类型 + 大小（≤10MB） | Toast 提示具体原因 |
| 未登录 | 401 | JWT 拦截器返回 401 | 跳转登录页 |
| 资源无权访问 | 403 | 用户只能操作自己的数据 | Toast "无权操作" |
| AI 无法识别车身 | 200 | status=failed, error_reason 说明原因 | "未识别到车辆，请上传清晰的整车照片" |
| AI API 超时 | 200 | 捕获 timeout 异常，标记 failed | "AI 服务繁忙，请稍后重试" |
| AI 额度耗尽 | 503 | 返回 503 + 运维告警 | "服务暂不可用，请稍后再试" |
| OSS 上传失败 | 500 | 记录日志 + 告警 | "上传失败，请重试" |

---

## 7. 安全设计

- **认证**：微信 code → openid → JWT token，有效期 7 天
- **图片校验**：校验文件 MIME 类型魔数，不仅靠扩展名
- **请求限流**：AI 换色接口令牌桶限流，单用户每分钟最多 5 次
- **数据隔离**：所有查询带 user_id 过滤，用户只能操作自己的数据
- **SQL 注入**：JPA 参数化查询，杜绝拼接 SQL
- **XSS**：输入过滤 + 输出转义

---

## 8. 测试策略

| 层级 | 工具 | 范围 |
|------|------|------|
| 单元测试 | JUnit 5 + Mockito | Service 层全覆盖，覆盖率 ≥ 70% |
| 接口测试 | Spring MockMvc | 所有 API 端点，含正常/异常入参 |
| 集成测试 | Testcontainers + MySQL | 数据库真实交互，OSS/AI 用 Mock |
| AI 模块专项 | WireMock | Mock AI API 的 6 种场景（正常/分割失败/超时/等） |

### AI 模块专项测试用例

| 用例 | 输入 | 预期结果 |
|------|------|---------|
| 正常换色 | 有效车照 + 有效颜色 | 返回 task_id，轮询后拿到 result_url |
| 非车照片 | 风景图 | AI 返回分割失败，status=failed |
| 超大文件 | >10MB 图片 | 后端 400 拒绝 |
| AI 超时 | Mock AI 30s 无响应 | 捕获超时，返回友好错误 |
| 并发提交 | 同用户 10 并发 | 全部正常处理，无数据错乱 |
| 未登录调用 | 无 Token | 401 |

---

## 9. 项目结构

```
qichegaise/
├── qcg-server/                 ← Spring Boot 后端
│   ├── src/main/java/com/qcg/
│   │   ├── QcgApplication.java
│   │   ├── config/             ← 安全、OSS、跨域配置
│   │   ├── controller/         ← REST 控制器 (Auth/User/Color/Photo/Ai/...)
│   │   ├── service/            ← 业务逻辑 (AiColorizeService ★)
│   │   ├── repository/         ← JPA Repository
│   │   ├── entity/             ← 实体类
│   │   ├── dto/                ← 请求/响应 DTO
│   │   ├── client/             ← 外部 API 客户端 (AiApiClient ★, WechatClient)
│   │   ├── common/             ← 异常处理/拦截器/工具类
│   │   └── enums/              ← 状态枚举
│   ├── src/main/resources/application.yml
│   └── src/test/
│
├── qcg-miniapp/                ← 微信小程序
│   ├── pages/
│   │   ├── index/              ← 首页
│   │   ├── color-picker/       ← ★ 选色换色页
│   │   ├── my-works/           ← 我的作品
│   │   └── car-models/         ← 车型库
│   ├── components/             ← 公共组件
│   └── utils/                  ← API 封装/工具函数
│
└── qcg-admin/                  ← Vue 3 管理后台 (Phase 3)
    └── src/views/
        ├── Dashboard.vue
        ├── ColorManage.vue
        ├── ShopAudit.vue
        └── ...
```

---

## 10. 待定事项

- **AI 云服务选型**：需评估阿里云视觉智能平台、腾讯云 AI、或专用汽车 AI 方案，比较分割精度和成本
- **预设车型库来源**：需爬取/购买标准车型数据库，或从公开渠道整理
- **地图服务**：门店地址选点需对接腾讯地图（微信小程序原生支持）
- **支付**（后续考虑）：若将来加入在线付款，需对接微信支付

---

> 下一步：用户审阅后，由 writing-plans 技能生成详细实施计划。
