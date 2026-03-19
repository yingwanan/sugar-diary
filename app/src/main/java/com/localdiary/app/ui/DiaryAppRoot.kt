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
import com.localdiary.app.ui.screen.EditorScreen
import com.localdiary.app.ui.screen.SettingsScreen
import com.localdiary.app.ui.screen.TimelineScreen
import com.localdiary.app.ui.screen.ViewerScreen
import com.localdiary.app.ui.viewmodel.BrowserViewModel
import com.localdiary.app.ui.viewmodel.EditorViewModel
import com.localdiary.app.ui.viewmodel.SettingsViewModel
import com.localdiary.app.ui.viewmodel.TimelineViewModel
import com.localdiary.app.ui.viewmodel.ViewerViewModel
import kotlinx.coroutines.flow.collectLatest

private const val TIMELINE_ROUTE = "timeline"
private const val BROWSER_ROUTE = "browser"
private const val SETTINGS_ROUTE = "settings"
private const val EDITOR_ROUTE = "editor"
private const val VIEWER_ROUTE = "viewer"

@Composable
fun DiaryAppRoot(
    container: AppContainer,
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val topLevelRoutes = listOf(TIMELINE_ROUTE, BROWSER_ROUTE, SETTINGS_ROUTE)

    LaunchedEffect(container.uiMessageManager) {
        container.uiMessageManager.messages.collectLatest { event ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(event.message)
        }
    }

    Scaffold(
        bottomBar = {
            if (currentDestination?.route in topLevelRoutes) {
                NavigationBar {
                    listOf(
                        TIMELINE_ROUTE to "时间轴",
                        BROWSER_ROUTE to "浏览",
                        SETTINGS_ROUTE to "设置",
                    ).forEach { (route, label) ->
                        val selected = currentDestination?.hierarchy?.any { it.route == route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Text(
                                    when (route) {
                                        TIMELINE_ROUTE -> "记"
                                        BROWSER_ROUTE -> "览"
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
