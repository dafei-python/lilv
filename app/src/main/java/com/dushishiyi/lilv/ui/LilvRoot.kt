package com.dushishiyi.lilv.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dushishiyi.lilv.R
import com.dushishiyi.lilv.ui.about.AboutScreen
import com.dushishiyi.lilv.ui.deposit.DepositScreen
import com.dushishiyi.lilv.ui.loan.LoanScreen
import com.dushishiyi.lilv.ui.markets.MarketsScreen

@Composable
fun LilvRoot() {
    val navController = rememberNavController()
    val ratesVm: RatesViewModel = viewModel(factory = RatesViewModel.Factory)

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: LilvRoute.Deposit.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                val items = LilvRoute.all
                for (i in items.indices) {
                    val route = items[i] ?: continue
                    val selected = currentRoute == route.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(route.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            val icon = if (selected) route.iconFilled else route.iconOutlined
                            Icon(icon, contentDescription = null)
                        },
                        label = { Text(stringResource(route.labelRes)) },
                    )
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = LilvRoute.Deposit.route,
            modifier = Modifier.fillMaxSize().padding(inner),
        ) {
            composable(LilvRoute.Deposit.route) {
                DepositScreen(viewModel = ratesVm)
            }
            composable(LilvRoute.Loan.route) {
                LoanScreen(viewModel = ratesVm)
            }
            composable(LilvRoute.Markets.route) {
                MarketsScreen(viewModel = ratesVm)
            }
            composable(LilvRoute.About.route) {
                AboutScreen()
            }
        }
    }
}

sealed class LilvRoute(
    val route: String,
    val labelRes: Int,
    val iconOutlined: ImageVector,
    val iconFilled: ImageVector,
) {
    object Deposit : LilvRoute(
        "deposit", R.string.tab_deposit,
        Icons.Outlined.AccountBalance, Icons.Rounded.AccountBalance,
    )

    object Loan : LilvRoute(
        "loan", R.string.tab_loan,
        Icons.Outlined.AttachMoney, Icons.Rounded.AttachMoney,
    )

    object Markets : LilvRoute(
        "markets", R.string.tab_markets,
        Icons.Outlined.TrendingUp, Icons.Rounded.TrendingUp,
    )

    object About : LilvRoute(
        "about", R.string.tab_about,
        Icons.Outlined.Info, Icons.Rounded.Info,
    )

    companion object {
        // 用 getter 而非 val：避免 companion <clinit> 早于 sealed 子类 object 初始化导致首项为 null
        val all: List<LilvRoute>
            get() = listOf(Deposit, Loan, Markets, About)
    }
}
