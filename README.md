# 🐾 OpenClaw Android

AI 智能助手 Android 客户端 —— 基于 Jetpack Compose + Material 3 构建的现代化 AI 聊天应用。

## ✨ 功能特性

### 🤖 AI 对话
- 支持 OpenAI 兼容 API（OpenAI / DeepSeek / Ollama / Azure 等）
- SSE 流式实时输出，打字机效果
- 多轮对话自动维护上下文
- **Markdown 渲染** — 标题、加粗、斜体、代码块、列表、引用
- **停止生成** — 随时中断 AI 输出
- 错误消息一键重试

### 🖼️ 图片分享
- 从相册选择图片发送
- 图片自动压缩 + Base64 编码
- 支持多模态 API（GPT-4V、Claude 3 等）
- 消息气泡内图片预览

### 🎙️ 语音输入
- Android SpeechRecognizer 集成
- 一键语音转文字发送
- 脉冲动画录音指示
- 自动请求麦克风权限

### 💬 对话管理
- 侧边栏新建/切换/删除多组对话
- 左滑删除对话（Swipe-to-Dismiss）
- 消息复制、时间戳显示
- 回到底部浮动按钮

### ⚙️ 灵活配置
- 自定义 API 端点、Key、模型、系统提示词
- API Key 安全遮罩显示
- DataStore 持久化存储

### 🎨 Material 3
- Dynamic Color（Material You）取色
- 深色模式（跟随系统 / 手动切换）
- 消息入场动画、打字指示器动画
- 脉冲呼吸灯 AI 头像

## 📸 架构

```
MVVM + Compose + Coroutines + Flow
├── ui/
│   ├── theme/          # Material 3 主题（颜色、字体、动画）
│   ├── screens/        # 页面（Chat、Settings）
│   ├── components/     # 组件（MessageBubble、InputBar、MarkdownText）
│   └── navigation/     # Navigation Compose 路由
├── data/
│   ├── model/          # 数据模型（支持多模态 ContentPart）
│   ├── api/            # OpenAI 兼容 API 客户端（SSE 流式 + 取消）
│   └── repository/     # 数据仓库（对话、设置、图片）
├── viewmodel/          # ViewModel 层（状态管理、业务逻辑）
└── OpenClawApp.kt      # Application
```

## 🚀 快速开始

### 1. 环境要求

- Android Studio Ladybug (2024.2.1) 或更高
- JDK 17+
- Android SDK 35

### 2. 打开项目

```bash
# 在 Android Studio 中打开
# File > Open > 选择 openclaw-android 目录
```

### 3. 配置 API

首次启动后，点击右上角 ⚙️ 设置：

| 配置项 | 示例 |
|--------|------|
| **API 端点** | `https://api.openai.com/v1` |
| **API Key** | `sk-xxxx` |
| **模型** | `gpt-4o-mini` |

支持任何 OpenAI 兼容 API：
- OpenAI: `https://api.openai.com/v1`
- DeepSeek: `https://api.deepseek.com/v1`
- 本地 Ollama: `http://localhost:11434/v1`
- Azure OpenAI: `https://xxx.openai.azure.com/openai/deployments/xxx/v1`

### 4. 构建运行

```bash
# Debug 版本
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug

# 运行单元测试
./gradlew test

# 运行 Android UI 测试
./gradlew connectedAndroidTest
```

## 🧪 测试

### 单元测试
```bash
./gradlew test
```

| 测试文件 | 覆盖内容 |
|----------|----------|
| `ConversationRepositoryTest` | 对话管理、消息增删、边界情况 |
| `OpenClawApiTest` | API 调用、流式响应、错误处理 |
| `ChatMessageTest` | 数据模型、默认值、复制语义 |

### UI 测试
```bash
./gradlew connectedAndroidTest
```

| 测试文件 | 覆盖内容 |
|----------|----------|
| `ChatScreenTest` | 消息气泡、Markdown 渲染、错误重试 |
| `InputBarTest` | 输入框、语音按钮、图片预览 |

## 🛠 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Kotlin | 2.1.0 | 语言 |
| Jetpack Compose | BOM 2024.12 | UI 框架 |
| Material 3 | Latest | 设计系统 |
| Navigation Compose | 2.8.5 | 路由导航 |
| OkHttp | 4.12.0 | HTTP 客户端 |
| Gson | 2.11.0 | JSON 序列化 |
| DataStore | 1.1.1 | 持久化存储 |
| Lifecycle | 2.8.7 | ViewModel + Flow |
| Coil | 2.7.0 | 图片加载 |
| Multiplatform Markdown | 0.26.0 | Markdown 渲染 |
| MockWebServer | 4.12.0 | API 测试 |
| Turbine | 1.2.0 | Flow 测试 |

## 📝 自定义

### 修改默认模型
编辑 `SettingsRepository.kt` 中的默认值：
```kotlin
val model: Flow<String> = context.dataStore.data.map { it[MODEL] ?: "gpt-4o-mini" }
```

### 修改主题色
编辑 `Color.kt` 中的颜色常量。

### 添加更多模型选项
在 `SettingsScreen.kt` 中添加下拉选择器。

## 📜 License

MIT License

---

> 🐾 Built with OpenClaw
