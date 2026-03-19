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
import com.example.helloapp.ui.settings.OppoHealthScreen
import com.example.helloapp.ui.settings.SettingsScreen
import com.example.helloapp.ui.theme.HelloAppTheme
import com.example.helloapp.ui.training.TrainingScreen
import com.example.helloapp.viewmodel.AICoachViewModel

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider

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
    var showOppoHealth by remember { mutableStateOf(false) }
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
                showOppoHealth = false
                settingsRefreshKey++
            },
            onBack = { /* 未登录不允许返回，留在登录页 */ }
        )

        isGeneratingPlan -> PlanGeneratingScreen()

        !hasProfile -> OnboardingScreen(
            onFinish = { profile ->
                val username = currentUsername ?: return@OnboardingScreen

                // 1. 显示加载动画
                isGeneratingPlan = true

                // 2. 启动协程进行网络请求
                scope.launch {
                    try {
                        // 因为 userRepo.save 变成了 suspend 网络请求，所以会在这里挂起等待
                        val saveResult = userRepo.save(username, profile)

                        saveResult.onSuccess {
                            profileVersion++
                            settingsRefreshKey++

                            // 3. 更新本地账户资料缓存
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

                            // 4. 调用后端生成专属计划并落盘 (耗时操作)
                            val planResult = planRepo.generateAndSaveUserPlan(username, profile)
                            planResult.onFailure {
                                Log.e("FitnessApp", "生成计划失败: ${it.message}", it)
                            }

                            // 5. 通知首页刷新
                            planGenerationFinishedToken++
                        }.onFailure { e ->
                            Log.e("FitnessApp", "保存档案到云端失败: ${e.message}", e)
                            // 这里可以考虑加一个 Toast 提示用户保存失败
                        }
                    } finally {
                        // 6. 无论成功失败，关闭加载动画，进入首页
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

        showOppoHealth -> OppoHealthScreen(
            onBack = { showOppoHealth = false }
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

            1 -> {
                val coachFactory = remember(currentUsername) {
                    AICoachViewModel.Factory(currentUsername ?: "guest")
                }

                val coachViewModel: AICoachViewModel = viewModel(
                    key = "coach_${currentUsername ?: "guest"}",
                    factory = coachFactory
                )

                AICoachScreen(
                    selectedNavItem = selectedNavItem,
                    onNavItemSelected = { selectedNavItem = it },
                    viewModel = coachViewModel
                )
            }

            2 -> SettingsScreen(
                selectedNavItem = selectedNavItem,
                onNavItemSelected = { selectedNavItem = it },
                onLoginClick = { /* 已登录，不需要 */ },
                onOpenOppoHealth = {
                    showOppoHealth = true
                },
                onLogout = {
                    currentUsername = null
                    selectedNavItem = 0
                    showTrainingScreen = false
                    showOppoHealth = false
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
