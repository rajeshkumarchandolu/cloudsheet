package com.opencloudsheet.demo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.opencloudsheet.demo.screens.ExpensesScreen
import com.opencloudsheet.demo.screens.ListSheetsScreen
import com.opencloudsheet.demo.screens.ListWorkBooksScreen
import com.opencloudsheet.demo.screens.LoginScreen

/**
 * Navigation routes for the app.
 */
object Route {
    const val LOGIN = "login"
    const val WORKBOOKS = "workbooks"
    const val SHEETS = "sheets/{workbookId}/{workbookName}"
    const val EXPENSES = "expenses/{workbookId}/{workbookName}/{sheetId}/{sheetName}"

    fun sheets(workbookId: String, workbookName: String): String {
        return "sheets/$workbookId/$workbookName"
    }

    fun expenses(workbookId: String, workbookName: String, sheetId: String, sheetName: String): String {
        return "expenses/$workbookId/$workbookName/$sheetId/$sheetName"
    }
}

/**
 * Main navigation graph for the app.
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Route.LOGIN
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Route.LOGIN) {
            LoginScreen(
                onNavigateToWorkBooks = {
                    navController.navigate(Route.WORKBOOKS) {
                        popUpTo(Route.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.WORKBOOKS) {
            ListWorkBooksScreen(
                onWorkBookSelected = { workbookId, workbookName ->
                    navController.navigate(Route.sheets(workbookId, workbookName))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Route.SHEETS,
            arguments = listOf(
                navArgument("workbookId") { type = NavType.StringType },
                navArgument("workbookName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val workbookId = backStackEntry.arguments?.getString("workbookId") ?: ""
            val workbookName = backStackEntry.arguments?.getString("workbookName") ?: ""

            ListSheetsScreen(
                workbookId = workbookId,
                workbookName = workbookName,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSheetSelected = { sheetId, sheetName ->
                    navController.navigate(Route.expenses(workbookId, workbookName, sheetId, sheetName))
                }
            )
        }

        composable(
            route = Route.EXPENSES,
            arguments = listOf(
                navArgument("workbookId") { type = NavType.StringType },
                navArgument("workbookName") { type = NavType.StringType },
                navArgument("sheetId") { type = NavType.StringType },
                navArgument("sheetName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val workbookId = backStackEntry.arguments?.getString("workbookId") ?: ""
            val workbookName = backStackEntry.arguments?.getString("workbookName") ?: ""
            val sheetId = backStackEntry.arguments?.getString("sheetId") ?: ""
            val sheetName = backStackEntry.arguments?.getString("sheetName") ?: ""

            ExpensesScreen(
                workbookId = workbookId,
                sheetId = sheetId,
                sheetName = sheetName,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
