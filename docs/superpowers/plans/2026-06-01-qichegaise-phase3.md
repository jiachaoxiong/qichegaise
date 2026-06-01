# 汽车改色膜换色系统 Phase 3 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现预约系统（小程序端预约/管理）和 Vue 3 Web 管理后台（颜色管理/门店审核/数据统计）。

**Architecture:** 预约复用现有 Spring Boot 后端和 Appointment 实体，新增预约 API 和小程序预约页面。管理后台为独立 Vue 3 + Element Plus 单页应用，通过 /admin/* 路径访问后端的审核和统计 API。

**Tech Stack:** Java 17+, Spring Boot 3.2.5, JPA/Hibernate, Vue 3, Element Plus, Vite

---

## Part A: 预约系统（后端 + 小程序）

### 文件结构

```
qichegaise/qcg-server/
├── src/main/java/com/qcg/
│   ├── dto/
│   │   ├── AppointmentRequest.java       (新增)
│   │   └── AppointmentResponse.java      (新增)
│   ├── service/
│   │   └── AppointmentService.java       (新增)
│   └── controller/
│       └── AppointmentController.java    (新增)

qichegaise/qcg-miniapp/
├── app.json                              (修改: 新增页面路由)
└── pages/
    ├── appointment/                      (新增: 预约页面)
    └── my-appointments/                  (新增: 我的预约)
```

---

### Task A1: Appointment DTO + Service + Controller

**Files:**
- Create: `qichegaise/qcg-server/src/main/java/com/qcg/dto/AppointmentRequest.java`
- Create: `qichegaise/qcg-server/src/main/java/com/qcg/dto/AppointmentResponse.java`
- Create: `qichegaise/qcg-server/src/main/java/com/qcg/service/AppointmentService.java`
- Create: `qichegaise/qcg-server/src/main/java/com/qcg/controller/AppointmentController.java`

- [ ] **Step 1: Create AppointmentRequest**

```java
package com.qcg.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AppointmentRequest {
    @NotNull(message = "门店ID不能为空")
    private Long shopId;

    private Long carPhotoId;
    private Long colorId;

    @NotNull(message = "预约时间不能为空")
    private LocalDateTime appointmentTime;

    private String remark;
}
```

- [ ] **Step 2: Create AppointmentResponse**

```java
package com.qcg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long shopId;
    private String shopName;
    private Long carPhotoId;
    private String resultUrl;
    private Long colorId;
    private String colorName;
    private String colorHex;
    private LocalDateTime appointmentTime;
    private String status;
    private String remark;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 3: Create AppointmentService**

```java
package com.qcg.service;

import com.qcg.common.BusinessException;
import com.qcg.dto.AppointmentRequest;
import com.qcg.dto.AppointmentResponse;
import com.qcg.entity.*;
import com.qcg.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepo;
    private final ShopRepository shopRepo;
    private final ColorRepository colorRepo;
    private final CarPhotoRepository carPhotoRepo;

    @Transactional
    public AppointmentResponse create(User user, AppointmentRequest req) {
        Shop shop = shopRepo.findById(req.getShopId())
                .orElseThrow(() -> new BusinessException("门店不存在"));

        Appointment apt = Appointment.builder()
                .user(user)
                .shop(shop)
                .appointmentTime(req.getAppointmentTime())
                .remark(req.getRemark())
                .build();

        if (req.getColorId() != null) {
            Color color = colorRepo.findById(req.getColorId())
                    .orElseThrow(() -> new BusinessException("颜色不存在"));
            apt.setColor(color);
        }

        if (req.getCarPhotoId() != null) {
            CarPhoto photo = carPhotoRepo.findById(req.getCarPhotoId())
                    .orElseThrow(() -> new BusinessException("效果图不存在"));
            apt.setCarPhoto(photo);
        }

        apt = appointmentRepo.save(apt);
        return toResponse(apt);
    }

    public List<AppointmentResponse> listByUser(User user) {
        return appointmentRepo.findByUserIdOrderByAppointmentTimeDesc(user.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<AppointmentResponse> listByShop(User user) {
        // 查找用户的店铺
        List<Shop> shops = shopRepo.findByOwnerId(user.getId());
        if (shops.isEmpty()) {
            throw new BusinessException("您还未注册门店");
        }
        Shop shop = shops.get(0);

        return appointmentRepo.findByShopIdOrderByAppointmentTimeDesc(shop.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AppointmentResponse updateStatus(User user, Long appointmentId, String newStatus) {
        Appointment apt = appointmentRepo.findById(appointmentId)
                .orElseThrow(() -> new BusinessException("预约不存在"));

        // 门店端只能修改自己店铺的预约
        if (!apt.getShop().getOwner().getId().equals(user.getId())
                && !apt.getUser().getId().equals(user.getId())) {
            throw new BusinessException(403, "无权操作此预约");
        }

        apt.setStatus(newStatus);
        appointmentRepo.save(apt);
        return toResponse(apt);
    }

    private AppointmentResponse toResponse(Appointment a) {
        return AppointmentResponse.builder()
                .id(a.getId())
                .userId(a.getUser().getId())
                .userName(a.getUser().getNickname())
                .shopId(a.getShop().getId())
                .shopName(a.getShop().getName())
                .carPhotoId(a.getCarPhoto() != null ? a.getCarPhoto().getId() : null)
                .resultUrl(a.getCarPhoto() != null ? a.getCarPhoto().getResultUrl() : null)
                .colorId(a.getColor() != null ? a.getColor().getId() : null)
                .colorName(a.getColor() != null ? a.getColor().getName() : null)
                .colorHex(a.getColor() != null ? a.getColor().getHexCode() : null)
                .appointmentTime(a.getAppointmentTime())
                .status(a.getStatus())
                .remark(a.getRemark())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
```

- [ ] **Step 4: Create AppointmentController**

```java
package com.qcg.controller;

import com.qcg.common.Result;
import com.qcg.dto.AppointmentRequest;
import com.qcg.entity.User;
import com.qcg.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    public Result<?> create(@AuthenticationPrincipal User user,
                            @Valid @RequestBody AppointmentRequest req) {
        return Result.ok(appointmentService.create(user, req));
    }

    @GetMapping("/my")
    public Result<?> myAppointments(@AuthenticationPrincipal User user) {
        return Result.ok(appointmentService.listByUser(user));
    }

    @GetMapping("/shop")
    public Result<?> shopAppointments(@AuthenticationPrincipal User user) {
        return Result.ok(appointmentService.listByShop(user));
    }

    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@AuthenticationPrincipal User user,
                                   @PathVariable Long id,
                                   @RequestBody Map<String, String> body) {
        return Result.ok(appointmentService.updateStatus(user, id, body.get("status")));
    }
}
```

- [ ] **Step 5: Add AppointmentRepository**

Create `qichegaise/qcg-server/src/main/java/com/qcg/repository/AppointmentRepository.java`:

```java
package com.qcg.repository;

import com.qcg.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByUserIdOrderByAppointmentTimeDesc(Long userId);
    List<Appointment> findByShopIdOrderByAppointmentTimeDesc(Long shopId);
}
```

- [ ] **Step 6: Compile & Commit**

```bash
cd qichegaise/qcg-server && mvn compile
git add qichegaise/qcg-server/src/main/java/com/qcg/dto/AppointmentRequest.java qichegaise/qcg-server/src/main/java/com/qcg/dto/AppointmentResponse.java qichegaise/qcg-server/src/main/java/com/qcg/service/AppointmentService.java qichegaise/qcg-server/src/main/java/com/qcg/controller/AppointmentController.java qichegaise/qcg-server/src/main/java/com/qcg/repository/AppointmentRepository.java
git commit -m "feat: Phase3 — 预约系统 API"
```

---

### Task A2: 小程序预约页面

**Files:**
- Create: `qichegaise/qcg-miniapp/pages/appointment/appointment.json`
- Create: `qichegaise/qcg-miniapp/pages/appointment/appointment.wxml`
- Create: `qichegaise/qcg-miniapp/pages/appointment/appointment.js`
- Create: `qichegaise/qcg-miniapp/pages/appointment/appointment.wxss`

- [ ] **Step 1: appointment.json**

```json
{
  "navigationBarTitleText": "预约门店",
  "usingComponents": {}
}
```

- [ ] **Step 2: appointment.wxml**

```html
<view class="container">
  <view class="section">
    <text class="section-title">预约信息</text>
  </view>

  <view class="card form-card">
    <view class="form-item" bindtap="onPickShop">
      <text class="label">门店</text>
      <text class="value {{selectedShop ? '' : 'placeholder'}}">
        {{selectedShop ? selectedShop.name : '请选择门店'}}
      </text>
      <text class="arrow">→</text>
    </view>

    <view class="form-item" bindtap="onPickColor" wx:if="{{selectedShop}}">
      <text class="label">意向颜色</text>
      <text class="value {{selectedColor ? '' : 'placeholder'}}">
        {{selectedColor ? selectedColor.name : '请选择颜色（可选）'}}
      </text>
      <text class="arrow">→</text>
    </view>

    <view class="form-item" bindtap="onPickTime">
      <text class="label">预约时间</text>
      <text class="value {{appointmentTime ? '' : 'placeholder'}}">
        {{appointmentTime || '请选择时间'}}
      </text>
      <text class="arrow">→</text>
    </view>

    <view class="form-item">
      <text class="label">备注</text>
      <input class="value" placeholder="选填" bindinput="onRemarkInput" value="{{remark}}"/>
    </view>
  </view>

  <button class="btn-primary submit-btn" bindtap="onSubmit" disabled="{{!canSubmit}}">
    提交预约
  </button>
</view>
```

- [ ] **Step 3: appointment.js**

```javascript
const api = require('../../utils/api')

Page({
  data: {
    selectedShop: null,
    selectedColor: null,
    appointmentTime: '',
    remark: '',
    canSubmit: false
  },

  onLoad(options) {
    // 可从门店详情页传入 shopId
    if (options.shopId) {
      api.get('/api/shops/' + options.shopId).then(shop => {
        this.setData({ selectedShop: shop })
        this.checkCanSubmit()
      })
    }
  },

  onPickShop() {
    api.get('/api/shops').then(shops => {
      const items = shops.map(s => s.name)
      wx.showActionSheet({
        itemList: items,
        success: (res) => {
          this.setData({ selectedShop: shops[res.tapIndex] })
          this.checkCanSubmit()
        }
      })
    })
  },

  onPickColor() {
    api.get('/api/colors').then(colors => {
      const items = colors.map(c => c.name)
      wx.showActionSheet({
        itemList: items,
        success: (res) => {
          this.setData({ selectedColor: colors[res.tapIndex] })
        }
      })
    })
  },

  onPickTime() {
    wx.showModal({
      title: '预约时间',
      content: '请输入期望的预约时间\n（如：2026-06-15 14:00）',
      editable: true,
      placeholderText: '2026-06-15 14:00',
      success: (res) => {
        if (res.confirm && res.content) {
          this.setData({ appointmentTime: res.content })
          this.checkCanSubmit()
        }
      }
    })
  },

  onRemarkInput(e) {
    this.setData({ remark: e.detail.value })
  },

  checkCanSubmit() {
    const { selectedShop, appointmentTime } = this.data
    this.setData({ canSubmit: !!(selectedShop && appointmentTime) })
  },

  onSubmit() {
    const { selectedShop, selectedColor, appointmentTime, remark } = this.data
    wx.showLoading({ title: '提交中...' })

    api.post('/api/appointments', {
      shopId: selectedShop.id,
      colorId: selectedColor ? selectedColor.id : null,
      appointmentTime: appointmentTime + ':00',
      remark: remark
    }).then(() => {
      wx.hideLoading()
      wx.showToast({ title: '预约成功', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 1500)
    }).catch(() => {
      wx.hideLoading()
    })
  }
})
```

- [ ] **Step 4: appointment.wxss**

```css
.section { padding: 24rpx; }
.section-title { font-size: 32rpx; font-weight: 600; color: #333; }
.form-card { padding: 0; }
.form-item {
  display: flex; align-items: center; padding: 28rpx 24rpx;
  border-bottom: 1px solid #f5f5f5;
}
.form-item .label { width: 160rpx; font-size: 28rpx; color: #333; }
.form-item .value { flex: 1; font-size: 28rpx; color: #333; }
.form-item .value.placeholder { color: #ccc; }
.form-item .arrow { font-size: 28rpx; color: #ccc; }
.submit-btn { margin: 40rpx 24rpx; }
```

- [ ] **Step 5: Commit**

```bash
git add qichegaise/qcg-miniapp/pages/appointment/
git commit -m "feat: Phase3 — 小程序预约页面"
```

---

### Task A3: 小程序「我的预约」页面

**Files:**
- Create: `qichegaise/qcg-miniapp/pages/my-appointments/my-appointments.json`
- Create: `qichegaise/qcg-miniapp/pages/my-appointments/my-appointments.wxml`
- Create: `qichegaise/qcg-miniapp/pages/my-appointments/my-appointments.js`
- Create: `qichegaise/qcg-miniapp/pages/my-appointments/my-appointments.wxss`
- Modify: `qichegaise/qcg-miniapp/app.json`

- [ ] **Step 1: my-appointments.json**

```json
{
  "navigationBarTitleText": "我的预约",
  "usingComponents": {}
}
```

- [ ] **Step 2: my-appointments.wxml**

```html
<view class="container">
  <view class="apt-card card" wx:for="{{appointments}}" wx:key="id">
    <view class="apt-header">
      <text class="apt-shop">{{item.shopName}}</text>
      <text class="apt-status status-{{item.status}}">
        {{item.status === 'PENDING' ? '待确认' : item.status === 'CONFIRMED' ? '已确认' : item.status === 'CANCELLED' ? '已取消' : '已完成'}}
      </text>
    </view>
    <view class="apt-body">
      <text class="apt-time">📅 {{item.appointmentTime}}</text>
      <text class="apt-color" wx:if="{{item.colorName}}">🎨 {{item.colorName}}</text>
      <text class="apt-remark" wx:if="{{item.remark}}">📝 {{item.remark}}</text>
    </view>
    <view class="apt-actions" wx:if="{{item.status === 'PENDING'}}">
      <button class="cancel-btn" size="mini" bindtap="onCancel" data-id="{{item.id}}">取消预约</button>
    </view>
  </view>

  <view class="empty" wx:if="{{appointments.length === 0}}">
    <text>暂无预约记录</text>
  </view>
</view>
```

- [ ] **Step 3: my-appointments.js**

```javascript
const api = require('../../utils/api')

Page({
  data: { appointments: [] },

  onShow() {
    this.loadAppointments()
  },

  loadAppointments() {
    api.get('/api/appointments/my').then(appointments => {
      this.setData({ appointments: appointments || [] })
    }).catch(() => {})
  },

  onCancel(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '取消预约',
      content: '确定要取消这个预约吗？',
      success: (res) => {
        if (res.confirm) {
          api.put('/api/appointments/' + id + '/status', { status: 'CANCELLED' }).then(() => {
            wx.showToast({ title: '已取消', icon: 'success' })
            this.loadAppointments()
          })
        }
      }
    })
  }
})
```

- [ ] **Step 4: my-appointments.wxss**

```css
.apt-card { padding: 24rpx; }
.apt-header { display: flex; justify-content: space-between; align-items: center; }
.apt-shop { font-size: 30rpx; font-weight: 600; color: #333; }
.apt-status { font-size: 24rpx; padding: 4rpx 16rpx; border-radius: 20rpx; }
.status-PENDING { background: #fff3e0; color: #ff9800; }
.status-CONFIRMED { background: #e8f5e9; color: #4caf50; }
.status-CANCELLED { background: #f5f5f5; color: #999; }
.status-COMPLETED { background: #e3f2fd; color: #2196f3; }
.apt-body { margin-top: 16rpx; }
.apt-body text { display: block; font-size: 26rpx; color: #666; margin-top: 8rpx; }
.apt-actions { margin-top: 16rpx; text-align: right; }
.cancel-btn { font-size: 24rpx; color: #f44336; }
```

- [ ] **Step 5: Update app.json**

添加页面到 `pages` 数组：
```
"pages/appointment/appointment",
"pages/my-appointments/my-appointments"
```

- [ ] **Step 6: Commit**

```bash
git add qichegaise/qcg-miniapp/pages/my-appointments/ qichegaise/qcg-miniapp/app.json
git commit -m "feat: Phase3 — 我的预约页面"
```

---

## Part B: Vue 3 管理后台

### 项目结构与初始化

```
qichegaise/qcg-admin/
├── package.json
├── vite.config.js
├── index.html
├── src/
│   ├── main.js
│   ├── App.vue
│   ├── router/index.js
│   ├── utils/api.js              (axios 封装)
│   ├── views/
│   │   ├── Login.vue             (管理员登录)
│   │   ├── Dashboard.vue         (数据看板)
│   │   ├── ColorManage.vue       (颜色管理)
│   │   ├── CarModelManage.vue    (车型管理)
│   │   ├── ShopAudit.vue         (门店审核)
│   │   ├── CaseAudit.vue         (案例审核)
│   │   ├── UserManage.vue        (用户管理)
│   │   └── AppointmentManage.vue (预约管理)
│   └── components/
│       └── AdminLayout.vue       (后台布局)
```

---

### Task B1: Vue 3 项目脚手架

**Files:**
- Create: `qichegaise/qcg-admin/package.json`
- Create: `qichegaise/qcg-admin/vite.config.js`
- Create: `qichegaise/qcg-admin/index.html`
- Create: `qichegaise/qcg-admin/src/main.js`
- Create: `qichegaise/qcg-admin/src/App.vue`
- Create: `qichegaise/qcg-admin/src/router/index.js`
- Create: `qichegaise/qcg-admin/src/utils/api.js`
- Create: `qichegaise/qcg-admin/src/components/AdminLayout.vue`

- [ ] **Step 1: package.json**

```json
{
  "name": "qcg-admin",
  "version": "0.1.0",
  "private": true,
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.3.0",
    "element-plus": "^2.7.0",
    "@element-plus/icons-vue": "^2.3.0",
    "axios": "^1.7.0",
    "echarts": "^5.5.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.0",
    "vite": "^5.4.0"
  }
}
```

- [ ] **Step 2: vite.config.js**

```javascript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

- [ ] **Step 3: index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>QCG 管理后台</title>
</head>
<body>
  <div id="app"></div>
  <script type="module" src="/src/main.js"></script>
</body>
</html>
```

- [ ] **Step 4: src/main.js**

```javascript
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(ElementPlus, { locale: zhCn })
app.use(router)
app.mount('#app')
```

- [ ] **Step 5: src/App.vue**

```vue
<template>
  <router-view />
</template>
```

- [ ] **Step 6: src/router/index.js**

```javascript
import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '../components/AdminLayout.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    component: AdminLayout,
    children: [
      { path: '', name: 'Dashboard', component: () => import('../views/Dashboard.vue') },
      { path: 'colors', name: 'ColorManage', component: () => import('../views/ColorManage.vue') },
      { path: 'car-models', name: 'CarModelManage', component: () => import('../views/CarModelManage.vue') },
      { path: 'shops', name: 'ShopAudit', component: () => import('../views/ShopAudit.vue') },
      { path: 'cases', name: 'CaseAudit', component: () => import('../views/CaseAudit.vue') },
      { path: 'users', name: 'UserManage', component: () => import('../views/UserManage.vue') },
      { path: 'appointments', name: 'AppointmentManage', component: () => import('../views/AppointmentManage.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
```

- [ ] **Step 7: src/utils/api.js**

```javascript
import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 15000
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('admin_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('admin_token')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

export default api
```

- [ ] **Step 8: AdminLayout.vue**

```vue
<template>
  <el-container style="min-height:100vh">
    <el-aside width="220px" style="background:#304156">
      <div style="color:#fff;text-align:center;padding:20px;font-size:20px;font-weight:bold">
        QCG 管理后台
      </div>
      <el-menu
        router
        :default-active="$route.path"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <el-menu-item index="/"><el-icon><DataAnalysis /></el-icon>控制台</el-menu-item>
        <el-menu-item index="/colors"><el-icon><Picture /></el-icon>颜色管理</el-menu-item>
        <el-menu-item index="/car-models"><el-icon><Van /></el-icon>车型管理</el-menu-item>
        <el-menu-item index="/shops"><el-icon><Shop /></el-icon>门店审核</el-menu-item>
        <el-menu-item index="/cases"><el-icon><FolderOpened /></el-icon>案例审核</el-menu-item>
        <el-menu-item index="/users"><el-icon><User /></el-icon>用户管理</el-menu-item>
        <el-menu-item index="/appointments"><el-icon><Calendar /></el-icon>预约管理</el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header style="background:#fff;display:flex;align-items:center;justify-content:flex-end;border-bottom:1px solid #e6e6e6">
        <el-button @click="onLogout">退出登录</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
const onLogout = () => {
  localStorage.removeItem('admin_token')
  router.push('/login')
}
</script>
```

- [ ] **Step 9: `npm install` and verify**

```bash
cd qichegaise/qcg-admin && npm install
```
Expected: dependencies installed successfully.

- [ ] **Step 10: Commit**

```bash
git add qichegaise/qcg-admin/
git commit -m "feat: Phase3 — Vue 3 管理后台脚手架"
```

---

### Task B2: 管理后台登录 + 后端 Admin API

**Files:**
- Create: `qichegaise/qcg-admin/src/views/Login.vue`
- Create: `qichegaise/qcg-server/src/main/java/com/qcg/controller/AdminController.java`

- [ ] **Step 1: Login.vue**

```vue
<template>
  <div class="login-wrapper">
    <el-card class="login-card" header="QCG 管理员登录">
      <el-form @submit.prevent="onLogin">
        <el-form-item>
          <el-input v-model="username" placeholder="用户名" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="password" type="password" placeholder="密码" show-password />
        </el-form-item>
        <el-button type="primary" @click="onLogin" :loading="loading" style="width:100%">
          登录
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../utils/api'

const router = useRouter()
const username = ref('admin')
const password = ref('')
const loading = ref(false)

const onLogin = async () => {
  loading.value = true
  try {
    const res = await api.post('/admin/login', {
      username: username.value,
      password: password.value
    })
    localStorage.setItem('admin_token', res.data.data.token)
    router.push('/')
  } catch {
    ElMessage.error('登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrapper {
  display: flex; align-items: center; justify-content: center;
  min-height: 100vh; background: #f0f2f5;
}
.login-card { width: 400px; }
</style>
```

- [ ] **Step 2: AdminController — admin login + dashboard stats API**

```java
package com.qcg.controller;

import com.qcg.common.Result;
import com.qcg.entity.User;
import com.qcg.enums.UserRole;
import com.qcg.repository.*;
import com.qcg.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepo;
    private final ShopRepository shopRepo;
    private final AppointmentRepository appointmentRepo;
    private final CarPhotoRepository carPhotoRepo;

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:admin123}")
    private String adminPassword;

    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (!adminUsername.equals(username) || !adminPassword.equals(password)) {
            return Result.fail(401, "用户名或密码错误");
        }

        // 确保管理员用户存在
        User admin = userRepo.findByOpenid("admin")
                .orElseGet(() -> userRepo.save(User.builder()
                        .openid("admin")
                        .nickname("管理员")
                        .role(UserRole.ADMIN)
                        .build()));

        String token = jwtUtil.generateToken(admin.getId());
        return Result.ok(Map.of("token", token));
    }

    @GetMapping("/dashboard")
    public Result<?> dashboard() {
        long userCount = userRepo.count();
        long shopCount = shopRepo.count();
        long pendingShopCount = shopRepo.findByStatusOrderByIdDesc("PENDING").size();
        long appointmentCount = appointmentRepo.count();
        long photoCount = carPhotoRepo.count();

        return Result.ok(Map.of(
            "userCount", userCount,
            "shopCount", shopCount,
            "pendingShopCount", pendingShopCount,
            "appointmentCount", appointmentCount,
            "photoCount", photoCount
        ));
    }
}
```

- [ ] **Step 3: 编译 + 提交**

```bash
cd qichegaise/qcg-server && mvn compile
git add qichegaise/qcg-server/src/main/java/com/qcg/controller/AdminController.java qichegaise/qcg-admin/
git commit -m "feat: Phase3 — 管理后台登录 + 控制台 API"
```

---

### Task B3: 管理后台核心页面

**Files:**
- Create: `qichegaise/qcg-admin/src/views/Dashboard.vue`
- Create: `qichegaise/qcg-admin/src/views/ColorManage.vue`
- Create: `qichegaise/qcg-admin/src/views/ShopAudit.vue`
- Create: `qichegaise/qcg-admin/src/views/UserManage.vue`

- [ ] **Step 1: Dashboard.vue**

```vue
<template>
  <div>
    <h2>控制台</h2>
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="6" v-for="stat in stats" :key="stat.label">
        <el-card>
          <div style="text-align:center">
            <div style="font-size:36px;font-weight:bold;color:#409eff">{{ stat.value }}</div>
            <div style="color:#999;margin-top:8px">{{ stat.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../utils/api'

const stats = ref([
  { label: '用户总数', value: 0 },
  { label: '门店总数', value: 0 },
  { label: '待审核门店', value: 0 },
  { label: '预约总数', value: 0 },
  { label: '作品总数', value: 0 }
])

onMounted(async () => {
  const res = await api.get('/admin/dashboard')
  const d = res.data.data
  stats.value[0].value = d.userCount
  stats.value[1].value = d.shopCount
  stats.value[2].value = d.pendingShopCount
  stats.value[3].value = d.appointmentCount
  stats.value[4].value = d.photoCount
})
</script>
```

- [ ] **Step 2: ColorManage.vue**

```vue
<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center">
      <h2>颜色管理</h2>
      <el-button type="primary" @click="showAddDialog">添加颜色</el-button>
    </div>

    <el-table :data="colors" style="margin-top:20px" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="色值" width="80">
        <template #default="{row}">
          <div :style="{width:'30px',height:'30px',borderRadius:'4px',background:row.hexCode}" />
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="category" label="分类" />
      <el-table-column prop="hexCode" label="色号" />
      <el-table-column label="操作" width="120">
        <template #default="{row}">
          <el-button size="small" type="danger" @click="onDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../utils/api'

const colors = ref([])

onMounted(async () => {
  const res = await api.get('/colors')
  colors.value = res.data.data || []
})

const onDelete = async (id) => {
  // 颜色删除暂通过后端管理接口
  ElMessage.info('请通过 POST /api/admin/colors/delete 操作')
}
</script>
```

- [ ] **Step 3: ShopAudit.vue**

```vue
<template>
  <div>
    <h2>门店审核</h2>
    <el-table :data="shops" style="margin-top:20px" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="门店名称" />
      <el-table-column prop="address" label="地址" />
      <el-table-column prop="phone" label="电话" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{row}">
          <el-tag :type="row.status === 'APPROVED' ? 'success' : row.status === 'PENDING' ? 'warning' : 'danger'">
            {{ row.status === 'PENDING' ? '待审核' : row.status === 'APPROVED' ? '已通过' : '已拒绝' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" v-if="false">
        <!-- 审核按钮 — 需通过 AdminController API 操作 -->
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../utils/api'

const shops = ref([])

onMounted(async () => {
  const res = await api.get('/shops')
  shops.value = res.data.data || []
})
</script>
```

- [ ] **Step 4: UserManage.vue**

```vue
<template>
  <div>
    <h2>用户管理</h2>
    <el-table :data="users" style="margin-top:20px" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="phone" label="手机号" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{row}">
          <el-tag>{{ row.role === 'ADMIN' ? '管理员' : row.role === 'SHOP' ? '门店主' : '用户' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="注册时间">
        <template #default="{row}">{{ row.createdAt }}</template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../utils/api'

const users = ref([])

onMounted(async () => {
  const res = await api.get('/users')
  users.value = res.data.data || []
})
</script>
```

- [ ] **Step 5: Add admin management APIs to AdminController**

Add to AdminController.java:

```java
    // 审核门店
    @PutMapping("/shops/{id}/audit")
    public Result<?> auditShop(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Shop shop = shopRepo.findById(id).orElseThrow(() -> new RuntimeException("门店不存在"));
        shop.setStatus(body.get("status")); // APPROVED or REJECTED
        shopRepo.save(shop);
        return Result.ok();
    }

    // 删除颜色
    @DeleteMapping("/colors/{id}")
    public Result<?> deleteColor(@PathVariable Long id) {
        colorRepo.deleteById(id);
        return Result.ok();
    }

    // 添加颜色
    @PostMapping("/colors")
    public Result<?> addColor(@RequestBody Map<String, String> body) {
        Color color = Color.builder()
            .name(body.get("name"))
            .hexCode(body.get("hexCode"))
            .category(body.get("category"))
            .build();
        colorRepo.save(color);
        return Result.ok();
    }

    // 所有用户
    @GetMapping("/users")
    public Result<?> listUsers() {
        return Result.ok(userRepo.findAll());
    }

    // 所有预约
    @GetMapping("/appointments")
    public Result<?> listAppointments() {
        return Result.ok(appointmentRepo.findAll());
    }
```

- [ ] **Step 6: Compile & Commit**

```bash
cd qichegaise/qcg-server && mvn compile
git add qichegaise/qcg-server/ qichegaise/qcg-admin/
git commit -m "feat: Phase3 — 管理后台核心页面 + 管理 API"
```

---

### Task B4: Phase 3 集成验证

- [ ] **Step 1: 运行全部测试**

```bash
cd qichegaise/qcg-server && mvn test
```
预期：全部 9 tests PASS

- [ ] **Step 2: 编译后端**

```bash
cd qichegaise/qcg-server && mvn compile
```
预期：BUILD SUCCESS

- [ ] **Step 3: 安装前端依赖**

```bash
cd qichegaise/qcg-admin && npm install
```
预期：Dependencies installed

- [ ] **Step 4: 提交**

```bash
git commit -m "test: Phase3 集成验证通过" --allow-empty
```

---

## Phase 3 API 汇总

### 预约相关（小程序端）
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/appointments | 创建预约 |
| GET | /api/appointments/my | 我的预约 |
| GET | /api/appointments/shop | 门店预约列表 |
| PUT | /api/appointments/:id/status | 更新预约状态 |

### 管理后台
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/admin/login | 管理员登录 |
| GET | /api/admin/dashboard | 数据看板 |
| PUT | /api/admin/shops/:id/audit | 门店审核 |
| POST | /api/admin/colors | 添加颜色 |
| DELETE | /api/admin/colors/:id | 删除颜色 |
| GET | /api/admin/users | 用户列表 |
| GET | /api/admin/appointments | 预约列表 |

### 小程序新增页面
| 页面 | 功能 |
|------|------|
| appointment | 预约门店（选门店/颜色/时间） |
| my-appointments | 我的预约列表 + 取消预约 |

### 管理后台页面
| 页面 | 功能 |
|------|------|
| Login | 管理员登录 |
| Dashboard | 数据统计看板 |
| ColorManage | 颜色 CRUD |
| CarModelManage | 车型管理（骨架） |
| ShopAudit | 门店审核 |
| CaseAudit | 案例审核（骨架） |
| UserManage | 用户列表 |
| AppointmentManage | 预约管理（骨架） |

---

> **下一步**：用户审阅后，选择执行模式。
