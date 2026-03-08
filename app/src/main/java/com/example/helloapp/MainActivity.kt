package com.example.helloapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

import com.example.helloapp.data.AccountRepository
import com.example.helloapp.data.PlanRepository
import com.example.helloapp.data.UserRepository
import com.example.helloapp.model.TrainingItem
import com.example.helloapp.ui.auth.AuthScreen
import com.example.helloapp.ui.coach.AICoachScreen
import com.example.helloapp.ui.home.HomeScreen
import com.example.helloapp.ui.onboarding.OnboardingScreen
import com.example.helloapp.ui.onboarding.PlanGeneratingScreen
import com.example.helloapp.ui.settings.SettingsScreen
import com.example.helloapp.ui.theme.HelloAppTheme
import com.example.helloapp.ui.training.TrainingScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HelloAppTheme {
                FitnessApp()
            }
        }
    }
}

@Composable
fun FitnessApp() {
    val context = LocalContext.current
    val userRepo = remember { UserRepository(context) }
    val accountRepo = remember { AccountRepository(context) }
    val planRepo = remember { PlanRepository(context) }
    val scope = rememberCoroutineScope()

    // 核心状态：当前登录账号
    var currentUsername by remember {
        mutableStateOf(accountRepo.currentAccount()?.username)
    }

    var profileVersion by remember { mutableStateOf(0) }
    var selectedNavItem by remember { mutableStateOf(0) }
    var showTrainingScreen by remember { mutableStateOf(false) }
    var settingsRefreshKey by remember { mutableStateOf(0) }
    var currentExercises by remember { mutableStateOf<List<TrainingItem>>(emptyList()) }
    var currentStartIndex by remember { mutableStateOf(0) }

    // 计划生成状态
    var isGeneratingPlan by remember { mutableStateOf(false) }
    var planGenerationFinishedToken by remember { mutableStateOf(0) }

    // 流程：未登录→登录页 | 已登录无资料→引导页 | 生成中→生成页 | 已登录有资料→主页
    val isLoggedIn = currentUsername != null
    val hasProfile = currentUsername?.let {
        profileVersion
        userRepo.hasProfile(it)
    } ?: false

    when {
        !isLoggedIn -> AuthScreen(
            onSuccess = {
                currentUsername = accountRepo.currentAccount()?.username
                selectedNavItem = 0
                showTrainingScreen = false
                settingsRefreshKey++
            },
            onBack = { /* 未登录不允许返回，留在登录页 */ }
        )

        isGeneratingPlan -> PlanGeneratingScreen()

        !hasProfile -> OnboardingScreen(
            onFinish = { profile ->
                val username = currentUsername ?: return@OnboardingScreen

                // 先切到“生成中”界面，避免先进入首页看到默认计划
                isGeneratingPlan = true

                scope.launch {
                    try {
                        // 1. 先保存画像
                        userRepo.save(username, profile)
                        profileVersion++
                        settingsRefreshKey++

                        // 2. 同步账户资料，供设置页展示
                        accountRepo.currentAccount()?.let { acct ->
                            accountRepo.updateAccount(
                                acct.copy(
                                    gender = profile.gender,
                                    age = profile.age.toString(),
                                    heightCm = profile.heightCm.toInt().toString(),
                                    weightKg = profile.weightKg.toInt().toString(),
                                    goal = profile.goals.joinToString("、")
                                )
                            )
                        }

                        // 3. 调用后端生成专属计划并落盘
                        val result = planRepo.generateAndSaveUserPlan(username, profile)

                        result.onFailure {
                            Log.e("FitnessApp", "生成计划失败: ${it.message}", it)
                        }

                        // 4. 生成结束后通知首页刷新一次
                        planGenerationFinishedToken++
                    } finally {
                        // 无论成功还是失败，都结束生成页
                        // 成功 -> 首页优先读到用户计划
                        // 失败 -> 首页回退默认计划
                        isGeneratingPlan = false
                    }
                }
            }
        )

        showTrainingScreen -> TrainingScreen(
            exercises = currentExercises,
            startIndex = currentStartIndex,
            onBack = { showTrainingScreen = false }
        )

        else -> when (selectedNavItem) {
            0 -> HomeScreen(
                selectedNavItem = selectedNavItem,
                onNavItemSelected = { selectedNavItem = it },
                onStartTraining = { exercises, startIndex ->
                    currentExercises = exercises
                    currentStartIndex = startIndex
                    showTrainingScreen = true
                },
                username = currentUsername,
                refreshKey = planGenerationFinishedToken
            )

            1 -> AICoachScreen(
                selectedNavItem = selectedNavItem,
                onNavItemSelected = { selectedNavItem = it }
            )

            2 -> SettingsScreen(
                selectedNavItem = selectedNavItem,
                onNavItemSelected = { selectedNavItem = it },
                onLoginClick = { /* 已登录，不需要 */ },
                onLogout = {
                    currentUsername = null
                    selectedNavItem = 0
                    showTrainingScreen = false
                    currentExercises = emptyList()
                    currentStartIndex = 0
                    isGeneratingPlan = false
                    planGenerationFinishedToken = 0
                },
                refreshKey = settingsRefreshKey
            )
        }
    }
}
