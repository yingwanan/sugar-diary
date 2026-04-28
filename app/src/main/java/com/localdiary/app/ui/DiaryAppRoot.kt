package com.localdiary.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.localdiary.app.di.AppContainer
import com.localdiary.app.ui.screen.BrowserScreen
import com.localdiary.app.ui.screen.EmotionCenterScreen
import com.localdiary.app.ui.screen.EmotionDetailScreen
import com.localdiary.app.ui.screen.EmotionReportsScreen
import com.localdiary.app.ui.screen.EditorScreen
import com.localdiary.app.ui.screen.PsychologyChatScreen
import com.localdiary.app.ui.screen.PsychologyProfileScreen
import com.localdiary.app.ui.screen.SettingsScreen
import com.localdiary.app.ui.screen.TimelineScreen
import com.localdiary.app.ui.screen.ViewerScreen
import com.localdiary.app.ui.viewmodel.BrowserViewModel
import com.localdiary.app.ui.viewmodel.EmotionCenterViewModel
import com.localdiary.app.ui.viewmodel.EmotionDetailViewModel
import com.localdiary.app.ui.viewmodel.EmotionReportsViewModel
import com.localdiary.app.ui.viewmodel.EditorViewModel
import com.localdiary.app.ui.viewmodel.PsychologyChatViewModel
import com.localdiary.app.ui.viewmodel.PsychologyProfileViewModel
import com.localdiary.app.ui.viewmodel.SettingsViewModel
import com.localdiary.app.ui.viewmodel.TimelineViewModel
import com.localdiary.app.ui.viewmodel.ViewerViewModel
import kotlinx.coroutines.flow.collectLatest

private const val TIMELINE_ROUTE = "timeline"
private const val BROWSER_ROUTE = "browser"
private const val EMOTION_ROUTE = "emotion"
private const val SETTINGS_ROUTE = "settings"
private const val EDITOR_ROUTE = "editor"
private const val VIEWER_ROUTE = "viewer"
private const val EMOTION_DETAIL_ROUTE = "emotion-detail"
private const val EMOTION_REPORTS_ROUTE = "emotion-reports"
private const val PSYCHOLOGY_CHAT_ROUTE = "psychology-chat"
private const val PSYCHOLOGY_PROFILE_ROUTE = "psychology-profile"

@Composable
fun DiaryAppRoot(
    container: AppContainer,
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val topLevelRoutes = listOf(TIMELINE_ROUTE, BROWSER_ROUTE, EMOTION_ROUTE, SETTINGS_ROUTE)

    LaunchedEffect(container.uiMessageManager) {
        container.uiMessageManager.messages.collectLatest { event ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(event.message)
        }
    }

    fun navigateToTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToTopLevelFromDetail(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = false
            }
            launchSingleTop = true
            restoreState = false
        }
    }

    Scaffold(
        bottomBar = {
            if (currentDestination?.route in topLevelRoutes) {
                NavigationBar {
                    listOf(
                        TIMELINE_ROUTE to "时间轴",
                        BROWSER_ROUTE to "浏览",
                        EMOTION_ROUTE to "心理",
                        SETTINGS_ROUTE to "设置",
                    ).forEach { (route, label) ->
                        val selected = currentDestination?.hierarchy?.any { it.route == route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateToTopLevel(route) },
                            icon = {
                                Text(
                                    when (route) {
                                        TIMELINE_ROUTE -> "记"
                                        BROWSER_ROUTE -> "览"
                                        EMOTION_ROUTE -> "心"
                                        else -> "设"
                                    },
                                )
                            },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            NavHost(
                navController = navController,
                startDestination = TIMELINE_ROUTE,
            ) {
                composable(TIMELINE_ROUTE) {
                    val viewModel: TimelineViewModel = viewModel(
                        factory = TimelineViewModel.factory(container.diaryRepository, container.uiMessageManager),
                    )
                    TimelineScreen(
                        viewModel = viewModel,
                        onOpenEntry = { entryId ->
                            navController.navigate("$EDITOR_ROUTE/$entryId")
                        },
                    )
                }
                composable(BROWSER_ROUTE) {
                    val viewModel: BrowserViewModel = viewModel(
                        factory = BrowserViewModel.factory(container.diaryRepository, container.uiMessageManager),
                    )
                    BrowserScreen(
                        viewModel = viewModel,
                        onOpenEntry = { entryId ->
                            navController.navigate("$VIEWER_ROUTE/$entryId")
                        },
                    )
                }
                composable(EMOTION_ROUTE) {
                    val viewModel: EmotionCenterViewModel = viewModel(
                        factory = EmotionCenterViewModel.factory(container.diaryRepository, container.uiMessageManager),
                    )
                    EmotionCenterScreen(
                        viewModel = viewModel,
                        onOpenEntry = { entryId ->
                            navController.navigate("$VIEWER_ROUTE/$entryId")
                        },
                        onEditEntry = { entryId ->
                            navController.navigate("$EDITOR_ROUTE/$entryId")
                        },
                        onOpenEmotionDetail = { entryId ->
                            navController.navigate("$EMOTION_DETAIL_ROUTE/$entryId")
                        },
                        onOpenPsychologyChat = { entryId ->
                            navController.navigate("$PSYCHOLOGY_CHAT_ROUTE/$entryId")
                        },
                        onOpenReports = {
                            navController.navigate(EMOTION_REPORTS_ROUTE)
                        },
                        onOpenProfile = {
                            navController.navigate(PSYCHOLOGY_PROFILE_ROUTE)
                        },
                    )
                }
                composable(PSYCHOLOGY_PROFILE_ROUTE) {
                    val viewModel: PsychologyProfileViewModel = viewModel(
                        factory = PsychologyProfileViewModel.factory(container.diaryRepository, container.uiMessageManager),
                    )
                    PsychologyProfileScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
                composable(EMOTION_REPORTS_ROUTE) {
                    val viewModel: EmotionReportsViewModel = viewModel(
                        factory = EmotionReportsViewModel.factory(container.diaryRepository, container.uiMessageManager),
                    )
                    EmotionReportsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
                composable("$EMOTION_DETAIL_ROUTE/{entryId}") { entryBackStack ->
                    val entryId = entryBackStack.arguments?.getString("entryId").orEmpty()
                    val viewModel: EmotionDetailViewModel = viewModel(
                        key = "emotion-detail-$entryId",
                        factory = EmotionDetailViewModel.factory(container.diaryRepository, container.uiMessageManager, entryId),
                    )
                    EmotionDetailScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onOpenEntry = { targetEntryId ->
                            navController.navigate("$VIEWER_ROUTE/$targetEntryId")
                        },
                        onEditEntry = { targetEntryId ->
                            navController.navigate("$EDITOR_ROUTE/$targetEntryId")
                        },
                        onOpenPsychologyChat = { targetEntryId ->
                            navController.navigate("$PSYCHOLOGY_CHAT_ROUTE/$targetEntryId")
                        },
                    )
                }
                composable("$PSYCHOLOGY_CHAT_ROUTE/{entryId}") { entryBackStack ->
                    val entryId = entryBackStack.arguments?.getString("entryId").orEmpty()
                    val viewModel: PsychologyChatViewModel = viewModel(
                        key = "psychology-chat-$entryId",
                        factory = PsychologyChatViewModel.factory(container.diaryRepository, container.uiMessageManager, entryId),
                    )
                    PsychologyChatScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
                composable("$VIEWER_ROUTE/{entryId}") { entryBackStack ->
                    val entryId = entryBackStack.arguments?.getString("entryId").orEmpty()
                    val viewModel: ViewerViewModel = viewModel(
                        key = "viewer-$entryId",
                        factory = ViewerViewModel.factory(container.diaryRepository, container.uiMessageManager, entryId),
                    )
                    ViewerScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onEditEntry = { targetEntryId ->
                            navController.navigate("$EDITOR_ROUTE/$targetEntryId")
                        },
                        onOpenEmotionCenter = {
                            navigateToTopLevelFromDetail(EMOTION_ROUTE)
                        },
                    )
                }
                composable("$EDITOR_ROUTE/{entryId}") { entryBackStack ->
                    val entryId = entryBackStack.arguments?.getString("entryId").orEmpty()
                    val viewModel: EditorViewModel = viewModel(
                        key = "editor-$entryId",
                        factory = EditorViewModel.factory(container.diaryRepository, container.uiMessageManager, entryId),
                    )
                    EditorScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onOpenEmotionCenter = {
                            navigateToTopLevelFromDetail(EMOTION_ROUTE)
                        },
                        onOpenEmotionDetail = {
                            navController.navigate("$EMOTION_DETAIL_ROUTE/$entryId")
                        },
                    )
                }
                composable(SETTINGS_ROUTE) {
                    val viewModel: SettingsViewModel = viewModel(
                        factory = SettingsViewModel.factory(container.diaryRepository, container.uiMessageManager),
                    )
                    SettingsScreen(viewModel = viewModel)
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp),
            )
        }
    }
}
