# SketchSync - Firebase 配置指南

## 步骤 1: 创建 Firebase 项目

1. 访问 [Firebase Console](https://console.firebase.google.com/)
2. 点击 "添加项目"
3. 项目名称填写 "SketchSync"
4. 可以选择禁用 Google Analytics（不影响功能）
5. 点击 "创建项目"

## 步骤 2: 添加 Android 应用

1. 在 Firebase 控制台，点击 Android 图标添加应用
2. 填写以下信息：
   - **Android 包名**: `com.sketchsync`
   - **应用别名**: SketchSync
   - **调试签名证书 SHA-1**: （可选，Google登录需要）
3. 点击 "注册应用"
4. 下载 `google-services.json` 文件
5. 将文件放入 `app/` 目录（替换 `google-services.json.template`）

## 步骤 3: 启用 Firebase 服务

### Authentication
1. 在左侧菜单选择 "Authentication"
2. 点击 "开始使用"
3. 在 "登录方法" 标签页启用：
   - 电子邮件/密码
   - Google（可选）

### Realtime Database
1. 在左侧菜单选择 "Realtime Database"
2. 点击 "创建数据库"
3. 选择数据库位置（推荐选择离你最近的）
4. 选择 "以测试模式启动"（开发时）
5. 设置安全规则（生产环境请修改）：
```json
{
  "rules": {
    "rooms": {
      "$roomId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    }
  }
}
```

### Firestore
1. 在左侧菜单选择 "Firestore Database"
2. 点击 "创建数据库"
3. 选择 "以测试模式启动"
4. 设置安全规则：
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    match /rooms/{roomId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
  }
}
```

### Storage
1. 在左侧菜单选择 "Storage"
2. 点击 "开始使用"
3. 选择 "以测试模式启动"

## 步骤 4: 配置完成

确保 `app/google-services.json` 文件存在且内容正确。

---

# Agora 配置指南

## 步骤 1: 创建 Agora 账号

1. 访问 [Agora Console](https://console.agora.io/)
2. 注册账号并登录
3. 验证邮箱

## 步骤 2: 创建项目

1. 在控制台点击 "创建项目"
2. 项目名称填写 "SketchSync"
3. 鉴权机制选择 "APP ID"（测试阶段）
4. 点击 "提交"

## 步骤 3: 获取 App ID

1. 在项目列表找到 SketchSync
2. 复制 App ID

## 步骤 4: 配置 App ID

1. 打开 `local.properties` 文件
2. 添加以下行：
```
AGORA_APP_ID=你的App ID
```

## 注意事项

- Agora 每月提供 10,000 分钟免费额度
- 生产环境建议使用 Token 鉴权
- 详细文档：[Agora官方文档](https://docs.agora.io/)

---

# 运行项目

## 前置要求

1. Android Studio Hedgehog (2023.1.1) 或更高版本
2. JDK 17+
3. Android SDK (API 24+)

## 运行步骤

1. 在 Android Studio 中打开项目
2. 等待 Gradle 同步完成
3. 确保 `google-services.json` 已配置
4. 确保 `local.properties` 中有 `AGORA_APP_ID`
5. 连接 Android 设备或启动模拟器
6. 点击运行按钮 ▶️

## 常见问题

### Gradle 同步失败
- 检查网络连接
- 尝试使用 VPN
- 清理 Gradle 缓存：File > Invalidate Caches

### Firebase 连接失败
- 检查 `google-services.json` 是否正确
- 确保 Firebase 项目中的包名是 `com.sketchsync`

### Agora 初始化失败
- 检查 App ID 是否正确
- 确保网络连接正常
