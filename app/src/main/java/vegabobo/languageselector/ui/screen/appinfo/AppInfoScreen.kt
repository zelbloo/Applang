package vegabobo.languageselector.ui.screen.appinfo

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import vegabobo.languageselector.R
import vegabobo.languageselector.ui.components.BackButton
import vegabobo.languageselector.ui.components.LocaleItemList
import vegabobo.languageselector.ui.components.QuickTextButton
import vegabobo.languageselector.ui.components.Title
import vegabobo.languageselector.ui.screen.BaseScreen

@Composable
fun AppInfoScreen(
    appId: String,
    navigateBack: () -> Unit,
    appInfoVm: AppInfoVm = hiltViewModel(),
) {
    val uiState by appInfoVm.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(appId) {
        appInfoVm.initFromAppId(appId)
        appInfoVm.updatePinnedLangsFromSP()
    }

    BaseScreen(
        title = stringResource(R.string.app_language),
        navIcon = { BackButton { navigateBack() } }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            contentPadding = innerPadding,
            modifier = Modifier.animateContentSize()
        ) {
            item { AppHeader(uiState) }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickTextButton(
                        modifier = Modifier.weight(1f),
                        onClick = { appInfoVm.onClickOpen() },
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                        text = stringResource(R.string.open)
                    )
                    QuickTextButton(
                        modifier = Modifier.weight(1f),
                        onClick = { appInfoVm.onClickForceClose() },
                        icon = Icons.Outlined.Close,
                        text = stringResource(R.string.close)
                    )
                    QuickTextButton(
                        modifier = Modifier.weight(1f),
                        onClick = { appInfoVm.onClickSettings() },
                        icon = Icons.Outlined.Settings,
                        text = stringResource(R.string.settings)
                    )
                }
            }

            val selectedRegion = uiState.listOfAllLanguages.getOrNull(uiState.selectedLanguage)
            if (selectedRegion != null) {
                item { Title(stringResource(R.string.region)) }
                items(selectedRegion.locales.size) { index ->
                    val locale = selectedRegion.locales[index]
                    LocaleRow(
                        locale = locale,
                        uiState = uiState,
                        appInfoVm = appInfoVm,
                        onClick = {
                            appInfoVm.onClickLocale(locale)
                            appInfoVm.onBackWhenSelectedLang()
                            coroutineScope.launch { listState.scrollToItem(0) }
                        }
                    )
                }
            } else {
                if (uiState.listOfPinnedLanguages.isNotEmpty()) {
                    item { Title(stringResource(R.string.pinned)) }
                    items(uiState.listOfPinnedLanguages.size) { index ->
                        val locale = uiState.listOfPinnedLanguages[index]
                        LocaleRow(
                            locale = locale,
                            uiState = uiState,
                            appInfoVm = appInfoVm,
                            onClick = { appInfoVm.onClickLocale(locale) }
                        )
                    }
                }

                item { Title(stringResource(R.string.user_languages)) }
                item {
                    LocaleItemList(
                        itemText = stringResource(R.string.system_default),
                        isSelected = uiState.currentLanguage.isEmpty(),
                        onClick = { appInfoVm.onClickResetLang() }
                    )
                }
                items(uiState.listOfSuggestedLanguages.size) { index ->
                    val locale = uiState.listOfSuggestedLanguages[index]
                    LocaleRow(
                        locale = locale,
                        uiState = uiState,
                        appInfoVm = appInfoVm,
                        onClick = { appInfoVm.onClickLocale(locale) }
                    )
                }

                item { Title(stringResource(R.string.all_languages)) }
                items(uiState.listOfAllLanguages.size) { index ->
                    LocaleItemList(
                        itemText = uiState.listOfAllLanguages[index].language,
                        onClick = {
                            appInfoVm.onClickSingleLanguage(index)
                            coroutineScope.launch { listState.scrollToItem(0) }
                        }
                    )
                }
            }
        }
    }

    if (uiState.selectedLanguage != -1)
        BackHandler { appInfoVm.onBackWhenSelectedLang() }
}

@Composable
private fun LocaleRow(
    locale: SingleLocale,
    uiState: AppInfoState,
    appInfoVm: AppInfoVm,
    onClick: () -> Unit,
) {
    val isPinned = uiState.listOfPinnedLanguages.any { it.languageTag == locale.languageTag }
    LocaleItemList(
        itemText = locale.name,
        isSelected = locale.name == uiState.currentLanguage,
        isPinned = isPinned,
        onTogglePin = { appInfoVm.onTogglePin(locale) },
        onLongClick = { appInfoVm.onTogglePin(locale) },
        onClick = onClick
    )
}

@Composable
private fun AppHeader(uiState: AppInfoState) {
    val resources = LocalResources.current
    val icon = remember(uiState.appIcon, resources) {
        uiState.appIcon?.toBitmap()?.asImageBitmap()
            ?: BitmapFactory.decodeResource(resources, R.drawable.icon_placeholder)
                .asImageBitmap()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier.size(64.dp),
            bitmap = icon,
            // Decorative: the app name is right next to it.
            contentDescription = null
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = uiState.appName,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = uiState.appPackage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = uiState.currentLanguage
                    .ifEmpty { stringResource(R.string.system_default) },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
