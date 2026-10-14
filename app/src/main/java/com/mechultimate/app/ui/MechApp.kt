package com.mechultimate.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mechultimate.core.engine.Module

private data class TopDest(val route: String, val label: String, val icon: ImageVector)

private val topDests = listOf(
    TopDest("home", "Home", Icons.Outlined.Home),
    TopDest("search", "Search", Icons.Outlined.Search),
    TopDest("saved", "Saved", Icons.Outlined.Bookmarks),
    TopDest("converter", "Units", Icons.Outlined.SwapHoriz),
    TopDest("settings", "Settings", Icons.Outlined.Settings),
)

const val DISCLAIMER = "MechUltimate is an engineering aid. It does not replace the governing code or standard, " +
    "or the judgement of a qualified engineer. Verify every result against the current edition of the cited code " +
    "before using it for design, fabrication or certification. Not affiliated with or endorsed by ASME, API, ISO, " +
    "AWS, ASHRAE or any other standards body."

@Composable
fun MechApp(app: AppViewModel = viewModel()) {
    val settings by app.settings.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val s = settings ?: return

    if (!s.disclaimerAccepted) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Before you start") },
            text = { Text(DISCLAIMER) },
            confirmButton = { TextButton(onClick = { app.acceptDisclaimer() }) { Text("I understand") } },
        )
    }

    fun openCalc(id: String, uid: String? = null) {
        app.pushRecent(id)
        nav.navigate("calc/$id" + (uid?.let { "?uid=$it" } ?: ""))
    }

    Scaffold(
        bottomBar = {
            if (topDests.any { it.route == current }) {
                NavigationBar {
                    topDests.forEach { d ->
                        NavigationBarItem(
                            selected = current == d.route,
                            onClick = {
                                nav.navigate(d.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(d.icon, contentDescription = null) },
                            label = { Text(d.label) },
                        )
                    }
                }
            }
        },
    ) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
            composable("home") {
                HomeScreen(
                    settings = s,
                    onModule = { nav.navigate("module/${it.name}") },
                    onCalc = { openCalc(it) },
                    onSearch = { nav.navigate("search") },
                    onStandards = { nav.navigate("standards") },
                )
            }
            composable("module/{m}") { entry ->
                val module = Module.valueOf(entry.arguments?.getString("m") ?: Module.PIPING.name)
                ModuleScreen(module, s.favorites, onBack = { nav.popBackStack() }, onCalc = { openCalc(it) })
            }
            composable("search") { SearchScreen(onCalc = { openCalc(it) }) }
            composable("standards") { StandardsScreen(onBack = { nav.popBackStack() }, onCalc = { openCalc(it) }) }
            composable("converter") { ConverterScreen(s.unitSystem, s.sigFigs) }
            composable("saved") { SavedScreen(app, onOpen = { calcId, uid -> openCalc(calcId, uid) }) }
            composable("settings") { SettingsScreen(app, s) }
            composable(
                "calc/{id}?uid={uid}",
                arguments = listOf(navArgument("uid") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) { entry ->
                CalculatorScreen(
                    calcId = entry.arguments?.getString("id") ?: "",
                    uid = entry.arguments?.getString("uid"),
                    app = app,
                    settings = s,
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}
