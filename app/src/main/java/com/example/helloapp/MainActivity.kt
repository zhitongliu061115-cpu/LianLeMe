package com.example.helloapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.helloapp.data.AccountRepository
import com.example.helloapp.data.UserRepository
import com.example.helloapp.model.TrainingItem
import com.example.helloapp.ui.auth.AuthScreen
import com.example.helloapp.ui.coach.AICoachScreen
import com.example.helloapp.ui.home.HomeScreen
import com.example.helloapp.ui.onboarding.OnboardingScreen
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

    // 流程：未登录→登录页 | 已登录无资料→引导页 | 已登录有资料→主页
    val isLoggedIn = currentUsername != null
    // profileVersion 参与计算，确保保存后能触发 recompose
    val hasProfile = currentUsername?.let {
        profileVersion; userRepo.hasProfile(it)
    } ?: false

    when {
        !isLoggedIn -> AuthScreen(
            onSuccess = {
                currentUsername = accountRepo.currentAccount()?.username
                settingsRefreshKey++
            },
            onBack = { /* 未登录不允许返回，留在登录页 */ }
        )
        !hasProfile -> OnboardingScreen(
            onFinish = { profile ->
                currentUsername?.let { username ->
                    userRepo.save(username, profile)
                    // 把引导页数据同步写入 Account（设置页直接可见）
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
                }
                profileVersion++
                settingsRefreshKey++
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
                }
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
                },
                refreshKey = settingsRefreshKey
            )
        }
    }
}


