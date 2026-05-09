package com.minimind.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.minimind.app.navigation.Routes
import com.minimind.app.ui.home.HomeScreen
import com.minimind.app.ui.inference.InferenceScreen
import com.minimind.app.ui.training.*
import com.minimind.app.ui.models.ModelsScreen
import com.minimind.app.ui.datasets.DatasetsScreen
import com.minimind.app.ui.settings.SettingsScreen
import com.minimind.app.ui.theme.MiniMindTheme

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiniMindTheme {
                MiniMindMainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniMindMainScreen() {
    val navController = rememberNavController()
    val bottomItems = listOf(
        BottomNavItem(Routes.HOME, "开始", Icons.Default.Home),
        BottomNavItem(Routes.INFERENCE, "推理", Icons.Default.Psychology),
        BottomNavItem(Routes.TRAINING, "训练", Icons.Default.School)
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showBottomBar = bottomItems.any { it.route == currentDestination?.route }

            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onNavigateToInference = { navController.navigate(Routes.INFERENCE) },
                    onNavigateToTraining = { navController.navigate(Routes.TRAINING) },
                    onNavigateToModels = { navController.navigate(Routes.MODELS) },
                    onNavigateToDatasets = { navController.navigate(Routes.DATASETS) },
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }
            composable(Routes.INFERENCE) {
                InferenceScreen()
            }
            composable(Routes.TRAINING) {
                TrainingScreen(
                    onNavigateToPretrain = { navController.navigate(Routes.PRETRAIN_CONFIG) },
                    onNavigateToSft = { navController.navigate(Routes.SFT_CONFIG) },
                    onNavigateToDistillation = { navController.navigate(Routes.DISTILLATION_CONFIG) },
                    onNavigateToLora = { navController.navigate(Routes.LORA_CONFIG) },
                    onNavigateToRl = { navController.navigate(Routes.RL_CONFIG) },
                    onNavigateToAgent = { navController.navigate(Routes.AGENT_CONFIG) },
                    onNavigateToMonitor = { taskId -> navController.navigate(Routes.trainingMonitor(taskId)) }
                )
            }
            composable(Routes.PRETRAIN_CONFIG) {
                PretrainConfigScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SFT_CONFIG) {
                SftConfigScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.DISTILLATION_CONFIG) {
                DistillationConfigScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.LORA_CONFIG) {
                LoraConfigScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.RL_CONFIG) {
                RlConfigScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.AGENT_CONFIG) {
                AgentConfigScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToEnvEditor = { navController.navigate(Routes.AGENT_ENV_EDITOR) }
                )
            }
            composable(Routes.TRAINING_MONITOR) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
                TrainingMonitorScreen(
                    taskId = taskId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.AGENT_ENV_EDITOR) {
                AgentEnvEditorScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.MODELS) {
                ModelsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.DATASETS) {
                DatasetsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
