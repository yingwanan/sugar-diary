package com.localdiary.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.localdiary.app.ui.designsystem.organism.AppBottomBar
import com.localdiary.app.ui.designsystem.organism.BottomBarItem
import com.localdiary.app.ui.navigation.DiaryRoutes
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

@Composable
fun DiaryAppRoot(
    container: AppContainer,
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val bottomBarItems = remember {
        listOf(
            BottomBarItem(
                route = DiaryRoutes.TIMELINE,
                label = "时间轴",
                icon = Icons.AutoMirrored.Outlined.MenuBook,
                selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
            ),
            BottomBarItem(
                route = DiaryRoutes.BROWSER,
                label = "浏览",
                icon = Icons.Outlined.Search,
                selectedIcon = Icons.Filled.Search,
            ),
            BottomBarItem(
                route = DiaryRoutes.EMOTION,
                label = "心理",
                icon = Icons.Outlined.Psychology,
                selectedIcon = Icons.Filled.Psychology,
            ),
            BottomBarItem(
                route = DiaryRoutes.SETTINGS,
                label = "设置",
                icon = Icons.Outlined.Settings,
                selectedIcon = Icons.Filled.Settings,
            ),
        )
    }
    val selectedTopLevelRoute = DiaryRoutes.topLevelRoutes.firstOrNull { route ->
        currentDestination?.hierarchy?.any { it.route == route } == true
    }

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
            if (selectedTopLevelRoute != null) {
                AppBottomBar(
                    selectedRoute = selectedTopLevelRoute,
                    onNavigate = ::navigateToTopLevel,
                    items = bottomBarItems,
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            NavHost(
                navController = navController,
                startDestination = DiaryRoutes.TIMELINE,
            ) {
                composable(DiaryRoutes.TIMELINE) {
                    val viewModel: TimelineViewModel = viewModel(
                        factory = TimelineViewModel.factory(container.diaryRepository, container.uiMessageManager),
                    )
                    TimelineScreen(
                        viewModel = viewModel,
                        onOpenEntry = { entryId ->
                            navController.navigate(DiaryRoutes.editor(entryId))
                        },
                    )
                }
                composable(DiaryRoutes.BROWSER) {
                    val viewModel: BrowserViewModel = viewModel(
                        factory = BrowserViewModel.factory(container.diaryRepository, container.uiMessageManager),
                    )
                    BrowserScreen(
                        viewModel = viewModel,
                        onOpenEntry = { entryId ->
                            navController.navigate(DiaryRoutes.viewer(entryId))
                        },
                    )
                }
                composable(DiaryRoutes.EMOTION) {
                    val viewModel: EmotionCenterViewModel = viewModel(
                        factory = EmotionCenterViewModel.factory(container.diaryRepository, container.uiMessageManager),
                    )
                    EmotionCenterScreen(
                        viewModel = viewModel,
                        onOpenEntry = { entryId ->
                            navController.navigate(DiaryRoutes.viewer(entryId))
                        },
                        onEditEntry = { entryId ->
                            navController.navigate(DiaryRoutes.editor(entryId))
                        },
                        onOpenEmotionDetail = { entryId ->
                            navController.navigate(DiaryRoutes.emotionDetail(entryId))
                        },
                        onOpenPsychologyChat = { entryId ->
                            navController.navigate(DiaryRoutes.psychologyChat(entryId))
                        },
                        onOpenReports = {
                            navController.navigate(DiaryRoutes.EMOTION_REPORTS)
                        },
                        onOpenProfile = {
                            navController.navigate(DiaryRoutes.PSYCHOLOGY_PROFILE)
                        },
                    )
                }
                composable(DiaryRoutes.PSYCHOLOGY_PROFILE) {
                    val viewModel: PsychologyProfileViewModel = viewModel(
                        factory = PsychologyProfileViewModel.factory(container.diaryRepository, container.uiMessageManager),
                    )
                    PsychologyProfileScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
                composable(DiaryRoutes.EMOTION_REPORTS) {
                    val viewModel: EmotionReportsViewModel = viewModel(
                        factory = EmotionReportsViewModel.factory(container.diaryRepository, container.uiMessageManager),
                    )
                    EmotionReportsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
                composable("${DiaryRoutes.EMOTION_DETAIL}/{entryId}") { entryBackStack ->
                    val entryId = entryBackStack.arguments?.getString("entryId").orEmpty()
                    val viewModel: EmotionDetailViewModel = viewModel(
                        key = "emotion-detail-$entryId",
                        factory = EmotionDetailViewModel.factory(container.diaryRepository, container.uiMessageManager, entryId),
                    )
                    EmotionDetailScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onOpenEntry = { targetEntryId ->
                            navController.navigate(DiaryRoutes.viewer(targetEntryId))
                        },
                        onEditEntry = { targetEntryId ->
                            navController.navigate(DiaryRoutes.editor(targetEntryId))
                        },
                        onOpenPsychologyChat = { targetEntryId ->
                            navController.navigate(DiaryRoutes.psychologyChat(targetEntryId))
                        },
                    )
                }
                composable("${DiaryRoutes.PSYCHOLOGY_CHAT}/{entryId}") { entryBackStack ->
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
                composable("${DiaryRoutes.VIEWER}/{entryId}") { entryBackStack ->
                    val entryId = entryBackStack.arguments?.getString("entryId").orEmpty()
                    val viewModel: ViewerViewModel = viewModel(
                        key = "viewer-$entryId",
                        factory = ViewerViewModel.factory(container.diaryRepository, container.uiMessageManager, entryId),
                    )
                    ViewerScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onEditEntry = { targetEntryId ->
                            navController.navigate(DiaryRoutes.editor(targetEntryId))
                        },
                        onOpenEmotionCenter = {
                            navigateToTopLevelFromDetail(DiaryRoutes.EMOTION)
                        },
                    )
                }
                composable("${DiaryRoutes.EDITOR}/{entryId}") { entryBackStack ->
                    val entryId = entryBackStack.arguments?.getString("entryId").orEmpty()
                    val viewModel: EditorViewModel = viewModel(
                        key = "editor-$entryId",
                        factory = EditorViewModel.factory(container.diaryRepository, container.uiMessageManager, entryId),
                    )
                    EditorScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onOpenEmotionCenter = {
                            navigateToTopLevelFromDetail(DiaryRoutes.EMOTION)
                        },
                        onOpenEmotionDetail = {
                            navController.navigate(DiaryRoutes.emotionDetail(entryId))
                        },
                    )
                }
                composable(DiaryRoutes.SETTINGS) {
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
