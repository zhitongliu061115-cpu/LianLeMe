# 练了么 - AI智能健身助手

一款基于AI的智能健身应用，提供个性化训练计划生成、语音对话教练、实时训练追踪等功能。

## 功能特性

- **用户认证系统** - 注册/登录，密码SHA-256加密
- **智能引导流程** - 采集用户体征和健身目标
- **AI训练计划生成** - 根据用户信息自动生成个性化14天训练计划
- **语音对话教练** - 集成讯飞语音识别和TTS，支持语音交互
- **训练执行追踪** - 实时动作计数、组数管理、摄像头反馈
- **日历视图** - 35天滚动日历查看每日训练安排
- **个人中心** - 资料编辑、头像上传、设置管理

## 技术栈

### 前端框架
- **Kotlin** - 主开发语言
- **Jetpack Compose** - 声明式UI框架
- **Material3** - UI设计系统
- **Coil** - 图片加载库

### 架构组件
- **MVVM架构** - 清晰的分层设计
- **ViewModel + StateFlow** - 响应式状态管理
- **Kotlin Coroutines** - 异步编程
- **Repository模式** - 数据访问层抽象

### 核心依赖
- **CameraX** - 摄像头功能
- **OkHttp** - HTTP网络请求
- **Gson** - JSON序列化
- **讯飞SparkChain SDK** - 语音识别
- **Android TTS** - 文字转语音

### 开发环境
- **compileSdk**: 34
- **minSdk**: 24 (Android 7.0+)
- **targetSdk**: 34
- **Kotlin**: 2.0.21
- **AGP**: 9.0.0

## 快速开始

### 1. 环境准备

```bash
# 克隆项目
git clone <repository-url>
cd lianleMe

# 确保安装了以下工具
- Android Studio Ladybug | 2024.1.1 或更高版本
- JDK 11 或更高版本
- Android SDK 34
```

### 2. 配置讯飞语音SDK

讯飞SDK配置已内置在 `app/build.gradle.kts:19-21`：
- APP_ID: cab4f3e8
- API_KEY: 已配置
- API_SECRET: 已配置

SDK文件位于 `app/libs/` 目录：
- `SparkChain.aar` - 语音识别SDK
- `Codec.aar` - 编解码库

### 3. 配置后端服务

后端服务配置在 `app/src/main/java/com/example/helloapp/data/remote/AiConfig.kt:3-5`：

```kotlin
const val SERVER_IP = "62.234.36.130"
const val SERVER_PORT = "8000"
const val BASE_URL = "http://$SERVER_IP:$SERVER_PORT"
```

### 4. 构建运行

```bash
# 同步Gradle依赖
./gradlew build

# 运行到连接的设备/模拟器
./gradlew installDebug
```

或在Android Studio中：
1. 打开项目
2. 等待Gradle同步完成
3. 点击运行按钮（绿色三角）

## 项目结构

```
app/src/main/java/com/example/helloapp/
├── MainActivity.kt                    # 应用入口，路由逻辑
├── MyApp.kt                          # Application类，初始化SDK
│
├── data/                             # 数据层
│   ├── AccountRepository.kt          # 账户数据仓库
│   ├── UserRepository.kt             # 用户档案仓库
│   ├── PlanRepository.kt             # 训练计划仓库
│   ├── repository/
│   │   └── CoachRepository.kt        # AI教练对话仓库
│   └── remote/
│       ├── AiApiService.kt           # HTTP客户端封装
│       └── AiConfig.kt               # API配置
│
├── model/                            # 数据模型
│   ├── Account.kt                    # 账户模型
│   ├── UserProfile.kt                # 用户档案
│   ├── TrainingItem.kt               # 训练项目
│   ├── ChatMessage.kt                # 聊天消息
│   └── ai/
│       ├── ApiResponse.kt            # API响应封装
│       └── GeneratedPlan.kt          # AI生成的计划
│
├── viewmodel/                        # ViewModel层
│   ├── HomeViewModel.kt              # 主页VM
│   ├── TrainingViewModel.kt          # 训练VM
│   └── AICoachViewModel.kt           # AI教练VM
│
├── ui/                               # UI层
│   ├── theme/                        # 主题配置
│   ├── auth/                         # 登录/注册
│   │   ├── AuthScreen.kt
│   │   └── AuthForms.kt
│   ├── onboarding/                   # 用户引导
│   │   ├── OnboardingScreen.kt       # 4步引导流程
│   │   ├── OnboardingComponents.kt
│   │   └── PlanGeneratingScreen.kt   # 计划生成动画
│   ├── home/                         # 主页
│   │   ├── HomeScreen.kt             # 主页容器
│   │   ├── GreetingHeader.kt         # 问候语
│   │   ├── WeekCalendar.kt           # 35天日历
│   │   ├── TrainingList.kt           # 训练列表
│   │   ├── TrainingCard.kt           # 训练卡片
│   │   └── ActionButtons.kt          # 操作按钮
│   ├── coach/                        # AI教练
│   │   ├── AICoachScreen.kt          # 对话界面
│   │   └── ChatMessageItem.kt        # 消息组件
│   ├── training/                     # 训练执行
│   │   ├── TrainingScreen.kt         # 训练主界面
│   │   └── CameraPreview.kt          # 摄像头预览
│   └── settings/                     # 设置
│       └── SettingsScreen.kt         # 设置页面
│
└── speech/                           # 语音功能
    ├── SpeechRecognizerManager.kt    # 语音识别
    └── TextToSpeechManager.kt        # 语音合成
```

## 核心功能实现

### 1. 用户认证流程

**入口**: `ui/auth/AuthScreen.kt:60-124`

```kotlin
// 注册流程
accountRepo.register(username, password, displayName)
  .onSuccess {
    // 注册成功后自动登录
    onLoginSuccess(it)
  }

// 登录验证
accountRepo.login(username, password)
  .onSuccess { account ->
    onLoginSuccess(account)
  }
```

**密码加密**: `data/AccountRepository.kt:17`
```kotlin
private fun hashPassword(password: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(password.toByteArray())
        .joinToString("") { "%02x".format(it) }
}
```

### 2. AI训练计划生成

**入口**: `MainActivity.kt:137-165`

**流程说明**:
1. 用户完成4步引导（性别、年龄、体重、健身目标等）
2. 点击"完成"触发计划生成1+
3. 显示加载动画 `PlanGeneratingScreen.kt`
4. 后台并发执行：
   - 保存用户档案到云端 `/save_profile`
   - 调用AI生成训练计划 `/generate_plan`
5. 计划生成完成后进入主页

**关键代码**:
```kotlin
// 保存档案
userRepo.save(username, profile)

// 生成计划
planRepo.generateAndSaveUserPlan(username, profile)
```

**API实现**: `data/PlanRepository.kt:25-56`

**请求格式**:
```json
{
  "user_id": "username",
  "profile": {
    "gender": "male",
    "height_cm": 175.0,
    "weight_kg": 70.0,
    "age": 25,
    "goals": "减脂,增肌",
    "focus_areas": "腹部,手臂",
    "workout_types": "力量,有氧"
  }
}
```

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "plan_days": 14,
    "start_date": "2026-03-17",
    "days": [
      {
        "day_index": 1,
        "title": "上肢训练日",
        "items": [
          {
            "name": "俯卧撑",
            "sets": 3,
            "reps": "15次",
            "rest_seconds": 60,
            "alternatives": ["跪姿俯卧撑"]
          }
        ],
        "notes": "注意动作标准"
      }
    ],
    "overall_advice": "循序渐进，注意休息"
  }
}
```

### 3. 语音对话功能

#### 语音识别

**实现**: `speech/SpeechRecognizerManager.kt`

**核心技术**:
- 讯飞SparkChain ASR SDK
- AudioRecord音频采集（16kHz, MONO, PCM_16BIT）
- 流式实时识别

**使用示例**:
```kotlin
val speechManager = SpeechRecognizerManager(context)

// 开始识别
speechManager.startRecognition(
    onPartialResult = { text ->
        // 实时显示识别中的文本
        updateInputText(text)
    },
    onFinalResult = { text ->
        // 最终识别结果
        sendMessage(text)
    },
    onError = { error ->
        showError(error)
    }
)

// 停止识别
speechManager.stopRecognition()
```

**工作流程**:
1. 初始化ASR会话 `ASR("zh_cn", "iat", "mandarin")`
2. 启动AudioRecord录音
3. 循环读取音频数据并写入ASR
4. 接收识别结果回调
5. 停止录音并等待最终结果

#### 语音合成（TTS）

**实现**: `speech/TextToSpeechManager.kt`

**使用示例**:
```kotlin
val ttsManager = TextToSpeechManager(context)

// 播报文本
ttsManager.speak("你好，我是你的AI健身教练", flush = true)

// 停止播报
ttsManager.stop()

// 释放资源
ttsManager.shutdown()
```

#### AI对话集成

**界面**: `ui/coach/AICoachScreen.kt:60-250`

**ViewModel**: `viewmodel/AICoachViewModel.kt`

**功能特点**:
- 文字输入和语音输入双模式
- 实时显示识别中的文本
- 语音播报开关控制
- 推荐提问快捷按钮
- 聊天历史管理

**对话流程**:
```kotlin
// 1. 用户点击麦克风
onMicClick() -> requestPermission() -> startRecognition()

// 2. 识别完成自动发送
onFinalResult(text) -> sendMessage(text)

// 3. 收到AI回复
coachRepo.sendMessage(message, history)
  .onSuccess { reply ->
    // 添加消息到列表
    addMessage(reply, isUser = false)
    // 自动播报（如果开启）
    if (ttsEnabled) tts.speak(reply)
  }
```

### 4. 训练执行功能

**界面**: `ui/training/TrainingScreen.kt:40-320`

**ViewModel**: `viewmodel/TrainingViewModel.kt`

**功能说明**:
- 实时动作计数/倒计时显示
- 多组训练管理（3组，每组休息60秒）
- 训练进度追踪（当前组/总组数）
- 摄像头实时预览（CameraX）
- 暂停/继续/结束控制

**状态管理**:
```kotlin
data class TrainingState(
    val isActive: Boolean,        // 训练中
    val isPaused: Boolean,         // 暂停
    val currentSet: Int,           // 当前组数
    val totalSets: Int,            // 总组数
    val currentCount: Int,         // 当前计数
    val targetCount: Int,          // 目标次数
    val restSeconds: Int           // 休息秒数
)
```

**训练流程**:
```kotlin
// 1. 开始训练
startTraining() -> isActive = true -> 启动摄像头

// 2. 完成一组
completeSet() ->
  if (currentSet < totalSets) {
    进入休息倒计时 -> 自动开始下一组
  } else {
    训练完成 -> 显示总结
  }

// 3. 暂停/继续
togglePause() -> isPaused = !isPaused

// 4. 结束训练
finishTraining() -> isActive = false -> 关闭摄像头
```

### 5. 日历与计划匹配

**日历组件**: `ui/home/WeekCalendar.kt:15-100`

**特点**:
- 35天滚动日历（今天前7天，后27天）
- 显示星期和日期
- 高亮今天
- 点击切换日期

**计划匹配逻辑**: `data/PlanRepository.kt:88-116`

**匹配规则**:
1. 加载AI生成的计划
2. 计算当前日期与计划开始日期的偏移量
3. 查找对应天数的训练内容
4. 如果没有AI计划或超出计划天数，使用默认周计划
5. 转换为 `TrainingItem` 列表显示

**代码示例**:
```kotlin
suspend fun getPlanForDate(username: String, date: Calendar): List<TrainingItem> {
    // 尝试加载AI生成计划
    val generatedPlan = loadUserGeneratedPlan(username).getOrNull()

    if (generatedPlan != null) {
        val planDay = findGeneratedDay(generatedPlan, date)
        if (planDay != null) {
            return planDay.toTrainingItems()
        }
    }

    // 回退到默认周计划
    return getFallbackPlanForDay(date.get(Calendar.DAY_OF_WEEK))
}
```

## API接口文档

### 基础URL
```
http://62.234.36.130:8000
```

### 1. 健康检查
```http
GET /ping
```

### 2. 用户注册
```http
POST /register
Content-Type: application/json

{
  "username": "string",
  "password_hash": "string",      // SHA-256哈希
  "display_name": "string",
  "avatar_path": "string"
}

Response:
{
  "code": 200,
  "msg": "success",
  "data": { ... }
}
```

### 3. 用户登录
```http
POST /login
Content-Type: application/json

{
  "username": "string",
  "password_hash": "string"
}
```

### 4. 保存用户档案
```http
POST /save_profile
Content-Type: application/json

{
  "user_id": "string",
  "gender": "male|female",
  "age": 25,
  "height_cm": 175.0,
  "weight_kg": 70.0,
  "goals": ["减脂", "增肌"],
  "focus_areas": ["腹部", "手臂"],
  "workout_types": ["力量", "有氧"]
}
```

### 5. 生成训练计划
```http
POST /generate_plan
Content-Type: application/json

{
  "user_id": "string",
  "profile": {
    "gender": "male",
    "height_cm": 175.0,
    "weight_kg": 70.0,
    "age": 25,
    "goals": "减脂,增肌",
    "focus_areas": "腹部,手臂",
    "workout_types": "力量,有氧"
  }
}

Response:
{
  "code": 200,
  "msg": "success",
  "data": {
    "plan_days": 14,
    "start_date": "2026-03-17",
    "days": [ ... ],
    "overall_advice": "string"
  }
}
```

### 6. AI对话
```http
POST /chat
Content-Type: application/json

{
  "user_id": "string",
  "message": "string",
  "history": [
    {"role": "user", "content": "你好"},
    {"role": "assistant", "content": "你好，有什么可以帮助你的？"}
  ]
}

Response:
{
  "code": 200,
  "msg": "success",
  "data": {
    "reply": "string"
  }
}
```

### 7. 更新设置
```http
POST /update_settings
Content-Type: application/json

{
  "username": "string",
  "display_name": "string",
  "avatar_path": "string",
  "signature": "string",
  "gender": "male",
  "age": "25",
  "height_cm": "175",
  "weight_kg": "70",
  "goals": "减脂,增肌"
}
```

## 开发指南

### 添加新的UI页面

1. 在 `ui/` 目录下创建新模块文件夹
2. 创建Screen.kt文件定义Composable
3. 如需状态管理，创建对应的ViewModel
4. 在 `MainActivity.kt` 中添加路由逻辑

示例：
```kotlin
// ui/newfeature/NewFeatureScreen.kt
@Composable
fun NewFeatureScreen(
    onBack: () -> Unit
) {
    Column {
        Text("New Feature")
    }
}

// MainActivity.kt
var showNewFeature by remember { mutableStateOf(false) }

if (showNewFeature) {
    NewFeatureScreen(onBack = { showNewFeature = false })
}
```

### 添加新的数据模型

1. 在 `model/` 目录下创建数据类
2. 添加JSON序列化注解（如需要）
3. 在对应的Repository中实现CRUD操作

### 调用后端API

1. 在 `data/repository/` 下创建新的Repository
2. 使用 `AiApiService` 发送请求
3. 使用 `ApiResponse<T>` 封装响应

示例：
```kotlin
class MyRepository(private val api: AiApiService) {
    suspend fun fetchData(): Result<MyData> = withContext(Dispatchers.IO) {
        runCatching {
            val json = api.get("/my-endpoint")
            val response: ApiResponse<MyData> = gson.fromJson(json, type)

            if (response.code != 200) {
                throw IllegalStateException(response.msg)
            }
            response.data
        }
    }
}
```

### 本地数据存储

项目使用基于文件的JSON存储：

```kotlin
// 保存数据
private fun saveData(context: Context, filename: String, data: Any) {
    val json = gson.toJson(data)
    val file = File(context.filesDir, filename)
    file.writeText(json)
}

// 读取数据
private fun loadData(context: Context, filename: String): MyData? {
    val file = File(context.filesDir, filename)
    if (!file.exists()) return null

    val json = file.readText()
    return gson.fromJson(json, MyData::class.java)
}
```

## 常见问题

### Q: 语音识别无法使用？
A: 检查以下几点：
1. 确保已授予录音权限（RECORD_AUDIO）
2. 确认讯飞SDK的APP_ID、API_KEY、API_SECRET配置正确
3. 检查网络连接是否正常
4. 查看Logcat中的错误日志

### Q: 训练计划没有显示？
A: 可能的原因：
1. 后端服务未启动或无法访问
2. AI计划生成失败，检查网络请求日志
3. 本地缓存损坏，清除应用数据重试

### Q: 摄像头预览黑屏？
A: 检查：
1. 是否授予相机权限（CAMERA）
2. 设备是否支持CameraX
3. 是否有其他应用占用摄像头

### Q: 如何修改后端服务地址？
A: 编辑 `data/remote/AiConfig.kt:3-5` 文件：
```kotlin
const val SERVER_IP = "your-server-ip"
const val SERVER_PORT = "your-port"
```

### Q: 如何自定义主题颜色？
A: 编辑 `ui/theme/Color.kt` 和 `ui/theme/Theme.kt` 文件

## 项目亮点

1. **完整的MVVM架构** - 清晰的分层设计，易于维护和测试
2. **纯Compose UI** - 使用最新的声明式UI框架
3. **语音交互** - 集成语音识别和TTS，提升用户体验
4. **AI驱动** - 个性化训练计划生成
5. **实时训练追踪** - 摄像头集成和进度管理
6. **状态驱动导航** - 轻量级路由方案
7. **协程异步** - 流畅的用户体验
8. **本地+云端存储** - 数据同步机制

## 未来优化建议

1. **数据库升级**: 使用Room替代文件存储，支持更复杂的查询
2. **依赖注入**: 引入Hilt简化依赖管理
3. **单元测试**: 增加测试覆盖率
4. **错误处理**: 统一错误处理和用户提示
5. **安全性**: 使用环境变量管理敏感信息
6. **国际化**: 支持多语言
7. **姿态识别**: 集成ML Kit实现动作标准度检测
8. **离线模式**: 支持无网络情况下使用基础功能

## 许可证

[在此添加许可证信息]

## 贡献

欢迎提交Issue和Pull Request！

---

**项目版本**: 1.0
**最后更新**: 2026-03-17
**维护者**: [在此添加维护者信息]
