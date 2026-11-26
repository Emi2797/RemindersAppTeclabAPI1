package com.example.remindersapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

object Routes {
    const val LIST = "reminder_list"
    const val CREATE = "reminder_create"
    const val DETAIL = "reminder_detail/{reminderId}"

    fun detailRoute(reminderId: Int) = "reminder_detail/$reminderId"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RemindersAppNavigation()
                }
            }
        }
    }
}

@Composable
fun RemindersAppNavigation() {
    val navController = rememberNavController()
    val sharedViewModel: ReminderViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Routes.LIST
    ) {
        composable(Routes.LIST) {
            ReminderListScreen(
                viewModel = sharedViewModel,
                onCrearRecordatorio = { navController.navigate(Routes.CREATE) },
                onRecordatorioClick = { id -> navController.navigate(Routes.detailRoute(id)) }
            )
        }

        composable(Routes.CREATE) {
            ReminderCreateScreen(
                viewModel = sharedViewModel,
                onGuardarSuccess = { navController.popBackStack() },
                onCancelar = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("reminderId") { type = NavType.IntType })
        ) { backStackEntry ->
            val reminderId = backStackEntry.arguments?.getInt("reminderId") ?: 0
            ReminderDetailScreen(
                reminderId = reminderId,
                viewModel = sharedViewModel,
                onVolver = { navController.popBackStack() }
            )
        }
    }
}