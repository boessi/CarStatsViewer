package com.ixam97.carStatsViewer.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.SettingsViewModel
import com.ixam97.carStatsViewer.compose.components.SideTab
import com.ixam97.carStatsViewer.compose.components.SideTabLayout
import com.ixam97.carStatsViewer.compose.screens.settings.About
import com.ixam97.carStatsViewer.compose.screens.settings.ApiSettings
import com.ixam97.carStatsViewer.compose.screens.settings.AppearanceSettings
import com.ixam97.carStatsViewer.compose.screens.settings.Changelog
import com.ixam97.carStatsViewer.compose.screens.settings.DevSettings
import com.ixam97.carStatsViewer.compose.screens.settings.GeneralSettings
import com.ixam97.carStatsViewer.compose.screens.settings.PrivacySettings
import com.ixam97.carStatsViewer.compose.screens.settings.apis.ABRPSettings
import com.ixam97.carStatsViewer.compose.screens.settings.apis.HTTPSettings

object SettingsScreens {
    const val GENERAL = "General"
    const val APPEARANCE = "Appearance"
    const val PRIVACY = "Privacy"
    const val APIS = "APIs"
    const val APIS_ABRP = "APIs_ABRP"
    const val APIS_HTTP = "APIs_HTTP"
    const val ABOUT = "About"
    const val ABOUT_CHANGELOG = "About_Changelog"
    const val DEV = "Dev"
    const val MAPBOX_TEST = "MapboxTest"
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {

    val tabsList = mutableListOf(
        SideTab(
            tabTitle = "General",
            tabIcon = Icons.Outlined.Settings,
            route = SettingsScreens.GENERAL,
            type = SideTab.Type.Tab,
            content = { GeneralSettings(viewModel = viewModel) }
        ),
        SideTab(
            tabTitle = "Appearance",
            route = SettingsScreens.APPEARANCE,
            tabIcon = Icons.Outlined.Create,
            type = SideTab.Type.Tab,
            content = { AppearanceSettings(viewModel = viewModel) }
        ),
        SideTab(
            tabTitle = "Privacy and location",
            route = SettingsScreens.PRIVACY,
            tabIcon = Icons.Outlined.LocationOn,
            type = SideTab.Type.Tab,
            content = { PrivacySettings(viewModel = viewModel) }
        ),
        SideTab(
            tabTitle = stringResource(R.string.settings_apis_title),
            route = SettingsScreens.APIS,
            tabIcon = Icons.Outlined.Share,
            type = SideTab.Type.Tab,
            content = { navController -> ApiSettings(viewModel = viewModel, navController = navController) }
        ),
        SideTab(
            tabTitle = "About Car Stats Viewer",
            tabIcon = Icons.Outlined.Info,
            route = SettingsScreens.ABOUT,
            type = SideTab.Type.Tab,
            content = { navController -> About(navController = navController, viewModel) }
        ),
        SideTab(
            tabTitle = "Changelog",
            route = SettingsScreens.ABOUT_CHANGELOG,
            type = SideTab.Type.Detail,
            content = { Changelog() }
        ),
        SideTab(
            tabTitle = stringResource(R.string.settings_apis_abrp),
            route = SettingsScreens.APIS_ABRP,
            type = SideTab.Type.Detail,
            content = { ABRPSettings() }
        ),
        SideTab(
            tabTitle = stringResource(R.string.settings_apis_http),
            route = SettingsScreens.APIS_HTTP,
            type = SideTab.Type.Detail,
            content = { HTTPSettings() }
        ),
        SideTab(
            tabTitle = "Developer settings",
            tabIcon = Icons.Outlined.Build,
            route = SettingsScreens.DEV,
            type = SideTab.Type.Tab,
            content = { DevSettings() },
            enabled = viewModel.isDevEnabled
        ),
        SideTab(
            tabTitle = "Mapbox Test",
            route = SettingsScreens.MAPBOX_TEST,
            type = SideTab.Type.Tab,
            content = { MapboxScreen() }
        )
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            // .padding(10.dp)
            // .clip(RoundedCornerShape(25.dp))
            .background(MaterialTheme.colors.background)
    ){
        SideTabLayout(
            tabs = tabsList,
            topLevelTitle = stringResource(id = R.string.settings_title),
            topLevelBackAction = {viewModel.finishActivity()}
        ) //, tabsColumnBackground = Color.Black)
    }

}

