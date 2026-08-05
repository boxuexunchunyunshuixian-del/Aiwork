# 🤖 AIWorkGroup - 安卓 APK 构建指南

## 方式一：GitHub Actions 自动构建（推荐，最简单）

### 步骤
1. **Fork 本项目** 到你的 GitHub 账号
2. 进入你 Fork 的仓库 → **Actions** 标签页
3. 点击左侧 **"Build APK"** → **Run workflow**
4. 等待约 3-5 分钟构建完成
5. 在 **Artifacts** 中下载 `aiworkgroup-debug-apk`（包含 app-debug.apk）
6. 将 APK 传到安卓手机安装即可

> 💡 以后每次推送代码到 main 分支，都会自动构建新的 APK

---

## 方式二：本地 Android Studio 构建

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更新版本
- JDK 17
- Android SDK API 35

### 步骤

#### 1. 导入项目
```bash
# 解压 ZIP 文件后，用 Android Studio 打开项目文件夹
File → Open → 选择 AIWorkGroup 文件夹
```

#### 2. 等待 Gradle 同步
首次打开时，Android Studio 会自动下载 Gradle 和依赖库，请耐心等待（需要翻墙或配置国内镜像）。

如果下载慢，可在 `gradle/wrapper/gradle-wrapper.properties` 中替换为腾讯镜像：
```properties
distributionUrl=https\://mirrors.cloud.tencent.com/gradle/gradle-8.4-bin.zip
```

并在 `settings.gradle.kts` 中添加阿里云镜像：
```kotlin
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
    }
}
```

#### 3. 构建 APK
```
Build → Build Bundle(s) / APK(s) → Build APK(s)
```

构建完成后，APK 位于：
```
app/build/outputs/apk/debug/app-debug.apk
```

#### 4. 安装到手机
- 开启手机的 **开发者模式** 和 **USB 调试**
- 用数据线连接电脑
- 点击 Android Studio 顶部的 **运行按钮**（▶️）
- 或手动复制 APK 到手机安装

---

## 方式三：命令行构建

```bash
# 1. 进入项目目录
cd AIWorkGroup

# 2. 赋予 gradlew 执行权限（Linux/Mac）
chmod +x gradlew

# 3. 构建 Debug APK
./gradlew assembleDebug

# 4. APK 输出位置
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 首次使用

1. 安装 APK 后打开应用
2. 点击右上角 **⚙️ 设置** 图标
3. 填写至少一个 AI 的 API Key（如 OpenAI 的 `sk-...`）
4. 点击 **保存配置**
5. 返回主界面，输入任务描述开始使用

---

## 常见问题

**Q: Gradle 同步失败？**
A: 检查网络连接，尝试替换为国内镜像源。

**Q: 编译报错 "Cannot find symbol"？**
A: 尝试 **File → Invalidate Caches → Invalidate and Restart**。

**Q: 安装时提示 "禁止安装未知来源应用"？**
A: 在手机设置中允许浏览器/文件管理器安装未知来源应用。

**Q: API Key 填了但无法使用？**
A: 检查网络是否能访问对应的 AI 平台，或尝试使用代理/BaseURL。
