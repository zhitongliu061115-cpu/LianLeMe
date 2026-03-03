# AGENTS.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## 项目概述

HelloApp 是一个 Android 健身训练 App，使用 Kotlin + Jetpack Compose 构建，采用 MVVM 架构。主要功能包括用户注册/登录、个人资料引导采集、按周训练计划展示、实时训练界面（含摄像头预览）和 AI 教练聊天。

## 构建

- **构建系统**: Gradle (Kotlin DSL)，通过 `gradlew.bat`（Windows）或 `./gradlew`（Linux/Mac）执行
- **编译**: `gradlew assembleDebug`
- **运行测试**: `gradlew testDebugUnitTest`（单元测试），`gradlew connectedDebugAndroidTest`（仪器测试）
- **单个测试**: `gradlew testDebugUnitTest --tests "com.example.helloapp.ExampleUnitTest"`
- **SDK 要求**: compileSdk 34, minSdk 24, Java 11
- 需要设置 `JAVA_HOME` 和 Android SDK（`local.properties` 中配置 `sdk.dir`）

## 架构

### 导航流程（无 Navigation 库，纯状态驱动）

`MainActivity` → `FitnessApp` Composable 通过 `when` 表达式控制全局页面流转：

1. **未登录** → `AuthScreen`（登录/注册）
2. **已登录但无 profile** → `OnboardingScreen`（4 步引导采集性别/年龄/体重/身高/目标）
3. **训练中** → `TrainingScreen`（`showTrainingScreen` 标志位控制）
4. **主页面** → 底部导航切换 `HomeScreen` / `AICoachScreen` / `SettingsScreen`

页面之间不使用 Jetpack Navigation，而是在 `FitnessApp()` 中用 `var showTrainingScreen`、`var selectedNavItem` 等 `mutableStateOf` 控制。

### 数据层（data/）

- **`AccountRepository`**: 基于 JSON 文件的账号系统。`accounts.json` 存全部账号，`current_account.json` 存当前登录态。密码使用 SHA-256 哈希。
- **`UserRepository`**: 以 `user_profile_{username}.json` 按用户存储个人资料。支持 draft 暂存。
- **`PlanRepository`**: 从 `assets/training_plan.json` 加载周训练计划，首次读取时拷贝到 `filesDir`。按星期几（0=周一...6=周日）查询当天动作列表。

所有 Repository 均使用 Gson 进行 JSON 序列化，数据存储在 `context.filesDir` 下。

### 模型层（model/）

- **`Account`**: 用户账号信息（用户名、密码哈希、头像路径、身体数据等）
- **`UserProfile`**: 引导页采集的完整用户画像（性别、年龄、目标、重点部位、运动偏好）
- **`TrainingItem`**: 单个训练动作（title, details, icon）。`details` 字段格式为 `"3组 | 15次/组"` 或 `"1组 | 30分钟"`，通过 `toSpec()` 方法解析为结构化的 `ExerciseSpec`
- **`ExerciseSpec`**: 从 details 解析出的训练参数（组数、每组次数或时长、是否按时间计）
- **`DailyPlan` / `WeeklyPlan`**: 每日/每周训练计划结构，与 `training_plan.json` 对应

### ViewModel 层（viewmodel/）

- **`HomeViewModel`**（AndroidViewModel）: 管理首页状态。维护 35 天日历范围（以今天为中心），通过 `PlanRepository` 加载选中日期的训练列表。
- **`TrainingViewModel`**: 管理训练执行状态。通过 `init(exercises, startIndex)` 初始化，支持：
  - 多组计数（currentSet / totalSets）和每组内次数或倒计时
  - 完成一组后自动进入下一组
  - 完成一个动作后自动过渡到下一个动作（2 秒过渡动画）
  - 全部动作完成后标记训练结束
  - 次数型动作以 `repDurationSec`（3 秒）模拟每次完成；时间型动作按秒倒计时
- **`AICoachViewModel`**: AI 教练聊天的消息管理（模拟回复）

### UI 层（ui/）

- **`ui/home/`**: `HomeScreen` → `GreetingHeader`（问候+时间）、`WeekCalendar`（35 天水平滚动日历）、`TrainingList`（当天动作卡片列表，空则显示休息日）、`ActionButtons`（自由训练/开始训练/模拟测试）
- **`ui/training/`**: `TrainingScreen`（训练主界面）+ `CameraPreview`（CameraX 前置摄像头）。顶部显示动作名、组数进度、进度条；中部摄像头画面；底部统计框+控制按钮
- **`ui/auth/`**: 登录/注册页面
- **`ui/onboarding/`**: 4 步新用户引导
- **`ui/coach/`**: AI 教练聊天界面
- **`ui/settings/`**: 设置页面（账号信息展示/修改）
- **`ui/components/`**: `BottomNavigation`（底部导航栏）、`DayItem`（日历中的单日组件）
- **`ui/theme/`**: Material3 主题定义（Color, Theme, Type）

### 训练计划数据格式

`assets/training_plan.json` 结构：
```json
{
  "days": [
    {
      "day": "monday",
      "exercises": [
        {"title": "核心肌群激活", "details": "3组 | 15次/组", "icon": "🧘"}
      ]
    }
  ]
}
```

`details` 字段有三种格式：
- 次数型: `"3组 | 15次/组"` — 3 组，每组 15 次
- 时间型: `"1组 | 30分钟"` — 1 组，持续 30 分钟
- 距离型: `"6组 | 200米/组"` — 按次数型处理

## 关键依赖

- Jetpack Compose (BOM 2024.02.00) + Material3
- CameraX 1.3.1（前置摄像头预览）
- Gson 2.10.1（JSON 序列化）
- Coil 2.6.0（图片加载）
- ViewModel Compose 2.7.0
