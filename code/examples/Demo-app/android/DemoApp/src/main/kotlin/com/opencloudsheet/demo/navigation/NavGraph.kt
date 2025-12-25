package com.opencloudsheet.demo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
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

    fun sheets(workbookId: String, workbookName: String): String {
        return "sheets/$workbookId/$workbookName"
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
                }
            )
        }
    }
}
