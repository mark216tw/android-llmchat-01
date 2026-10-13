package com.example.llmchat

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.llmchat.domain.model.ThemeMode
import com.example.llmchat.ui.chat.ChatScreen
import com.example.llmchat.ui.conversations.ConversationsScreen
import com.example.llmchat.ui.debug.DebugLogScreen
import com.example.llmchat.ui.navigation.Routes
import com.example.llmchat.ui.settings.AppSettingsScreen
import com.example.llmchat.ui.settings.AppSettingsViewModel
import com.example.llmchat.ui.settings.ProfileEditScreen
import com.example.llmchat.ui.settings.ProfileListScreen
import com.example.llmchat.ui.theme.LlmChatTheme
import com.example.llmchat.ui.theme.LocalChatFontSize
import com.example.llmchat.ui.theme.LocalTtsLocale
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: AppSettingsViewModel = hiltViewModel()
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            val chatFontSize by settingsViewModel.chatFontSize.collectAsStateWithLifecycle()
            val ttsLanguage by settingsViewModel.ttsLanguage.collectAsStateWithLifecycle()
            val ttsLocale = remember(ttsLanguage) {
                ttsLanguage.tag?.let { Locale.forLanguageTag(it) }
            }
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            LaunchedEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                )
            }
            LlmChatTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(
                    LocalChatFontSize provides chatFontSize.sp,
                    LocalTtsLocale provides ttsLocale
                ) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val expanded = LocalConfiguration.current.screenWidthDp >= 600
    val isTopLevel = currentRoute == Routes.CONVERSATIONS || currentRoute == Routes.PROFILES

    val navigateToTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val navHost: @Composable () -> Unit = {
        NavHost(
            navController = navController,
            startDestination = Routes.CONVERSATIONS,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable(Routes.CONVERSATIONS) {
                ConversationsScreen(navController = navController, expanded = expanded)
            }
            composable(Routes.PROFILES) {
                ProfileListScreen(
                    navController = navController,
                    applyWindowInsets = expanded
                )
            }
            composable(
                route = Routes.PROFILE_EDIT,
                arguments = listOf(navArgument(Routes.ARG_PROFILE_ID) { type = NavType.LongType })
            ) { entry ->
                ProfileEditScreen(
                    navController = navController,
                    profileId = entry.arguments?.getLong(Routes.ARG_PROFILE_ID) ?: -1L
                )
            }
            composable(
                route = Routes.CHAT,
                arguments = listOf(navArgument(Routes.ARG_CONVERSATION_ID) { type = NavType.LongType }),
                enterTransition = {
                    slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(220))
                },
                popExitTransition = {
                    slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(200))
                }
            ) { entry ->
                ChatScreen(
                    navController = navController,
                    conversationId = entry.arguments?.getLong(Routes.ARG_CONVERSATION_ID) ?: -1L
                )
            }
            composable(Routes.DEBUG) {
                DebugLogScreen(navController = navController)
            }
            composable(Routes.SETTINGS) {
                AppSettingsScreen(navController = navController)
            }
        }
    }

    if (expanded) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (isTopLevel) {
                NavigationRail {
                    NavigationRailItem(
                        selected = currentRoute == Routes.CONVERSATIONS,
                        onClick = { navigateToTopLevel(Routes.CONVERSATIONS) },
                        icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) },
                        label = { Text("對話") }
                    )
                    NavigationRailItem(
                        selected = currentRoute == Routes.PROFILES,
                        onClick = { navigateToTopLevel(Routes.PROFILES) },
                        icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                        label = { Text("API設定") }
                    )
                }
            }
            Box(modifier = Modifier.weight(1f)) { navHost() }
        }
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                AnimatedVisibility(
                    visible = isTopLevel,
                    enter = expandVertically(tween(220)) + fadeIn(tween(220)),
                    exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
                ) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentRoute == Routes.CONVERSATIONS,
                            onClick = { navigateToTopLevel(Routes.CONVERSATIONS) },
                            icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) },
                            label = { Text("對話") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == Routes.PROFILES,
                            onClick = { navigateToTopLevel(Routes.PROFILES) },
                            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                            label = { Text("API設定") }
                        )
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) { navHost() }
        }
    }
}
