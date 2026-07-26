package vegabobo.languageselector.ui.screen.main

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopSearchBar
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import vegabobo.languageselector.R
import vegabobo.languageselector.ui.components.AppListItem
import vegabobo.languageselector.ui.components.FilterLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navigateToAppScreen: (String) -> Unit,
    navigateToAbout: () -> Unit,
    mainScreenVm: MainScreenVm = hiltViewModel(),
) {
    val uiState by mainScreenVm.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()
    val searchBarScrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    var showSystemAppsInSearch by rememberSaveable { mutableStateOf(false) }
    var onlyModifiedInSearch by rememberSaveable { mutableStateOf(false) }

    val searchQuery = textFieldState.text.toString()
    val homeApps = remember(uiState.listOfApps, uiState.isShowSystemAppsHome) {
        uiState.listOfApps.filterForHome(uiState.isShowSystemAppsHome)
    }
    val searchResults = remember(
        uiState.listOfApps, searchQuery, showSystemAppsInSearch, onlyModifiedInSearch
    ) {
        uiState.listOfApps.filterForSearch(
            query = searchQuery,
            showSystemApps = showSystemAppsInSearch,
            onlyModified = onlyModifiedInSearch,
        )
    }

    val movedToTopMessage = stringResource(R.string.moved_to_top)
    val movedToBottomMessage = stringResource(R.string.moved_to_bottom)
    val showActionLabel = stringResource(R.string.action_show)

    LaunchedEffect(Unit) { mainScreenVm.reloadLastSelectedItem() }

    LaunchedEffect(searchBarState.targetValue) {
        if (searchBarState.targetValue == SearchBarValue.Expanded) mainScreenVm.updateHistory()
    }

    LaunchedEffect(uiState.snackBarDisplay) {
        val message = when (uiState.snackBarDisplay) {
            SnackBarDisplay.MOVED_TO_TOP -> movedToTopMessage
            SnackBarDisplay.MOVED_TO_BOTTOM -> movedToBottomMessage
            SnackBarDisplay.NONE -> return@LaunchedEffect
        }
        // Reset first, so a dismissed snackbar cannot be shown again on the next recomposition.
        mainScreenVm.resetSnackBarDisplay()
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = showActionLabel
        )
        if (result == SnackbarResult.ActionPerformed) {
            val index = homeApps.indexOfFirst { it.pkg == mainScreenVm.lastSelectedApp?.pkg }
            if (index != -1) lazyListState.animateScrollToItem(index)
        }
    }

    fun openApp(app: AppInfo) {
        mainScreenVm.onClickApp(app)
        navigateToAppScreen(app.pkg)
    }

    val inputField = @Composable {
        SearchBarDefaults.InputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            onSearch = { scope.launch { searchBarState.animateToCollapsed() } },
            placeholder = { Text(stringResource(R.string.search)) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (searchBarState.targetValue == SearchBarValue.Expanded) {
                    if (searchQuery.isNotEmpty())
                        IconButton(onClick = { textFieldState.clearText() }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.clear)
                            )
                        }
                } else {
                    SearchBarActions(
                        isDropdownVisible = uiState.isDropdownVisible,
                        isShowingSystemApps = uiState.isShowSystemAppsHome,
                        onToggleDropdown = { mainScreenVm.toggleDropdown() },
                        onClickToggleSystemApps = { mainScreenVm.toggleSystemAppsVisibility() },
                        onClickAbout = navigateToAbout
                    )
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(searchBarScrollBehavior.nestedScrollConnection),
        topBar = {
            TopSearchBar(
                state = searchBarState,
                inputField = inputField,
                scrollBehavior = searchBarScrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when {
            uiState.isLoading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

            homeApps.isEmpty() ->
                CenteredMessage(
                    text = stringResource(R.string.no_apps),
                    modifier = Modifier.padding(innerPadding)
                )

            else ->
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = innerPadding
                ) {
                    items(
                        count = homeApps.size,
                        key = { homeApps[it].pkg }
                    ) { index ->
                        val app = homeApps[index]
                        AppListItem(app = app, onClickApp = { openApp(app) })
                    }
                }
        }
    }

    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = inputField
    ) {
        SearchResults(
            query = searchQuery,
            results = searchResults,
            history = uiState.history,
            showSystemApps = showSystemAppsInSearch,
            onlyModified = onlyModifiedInSearch,
            onToggleShowSystemApps = { showSystemAppsInSearch = !showSystemAppsInSearch },
            onToggleOnlyModified = { onlyModifiedInSearch = !onlyModifiedInSearch },
            onClickClearHistory = { mainScreenVm.onClickClear() },
            onClickApp = { scope.launch { searchBarState.animateToCollapsed() }; openApp(it) }
        )
    }

    if (uiState.operationMode == OperationMode.NONE && !uiState.isLoading)
        ShizukuRequiredWarning { mainScreenVm.onClickProceedShizuku() }
}

@Composable
private fun SearchResults(
    query: String,
    results: List<AppInfo>,
    history: List<AppInfo>,
    showSystemApps: Boolean,
    onlyModified: Boolean,
    onToggleShowSystemApps: () -> Unit,
    onToggleOnlyModified: () -> Unit,
    onClickClearHistory: () -> Unit,
    onClickApp: (AppInfo) -> Unit,
) {
    if (query.isBlank()) {
        if (history.isEmpty()) {
            CenteredMessage(stringResource(R.string.search_empty_hint))
            return
        }
        LazyColumn {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.search_recent),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = onClickClearHistory) {
                        Text(stringResource(R.string.clear))
                    }
                }
            }
            items(count = history.size, key = { history[it].pkg }) { index ->
                val app = history[index]
                AppListItem(app = app, onClickApp = { onClickApp(app) })
            }
        }
        return
    }

    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterLabel(
            title = stringResource(R.string.filter_system_apps),
            isSelected = showSystemApps,
            onClick = onToggleShowSystemApps
        )
        FilterLabel(
            title = stringResource(R.string.filter_modified),
            isSelected = onlyModified,
            onClick = onToggleOnlyModified
        )
    }

    if (results.isEmpty()) {
        CenteredMessage(stringResource(R.string.search_no_results))
        return
    }

    LazyColumn {
        items(count = results.size, key = { results[it].pkg }) { index ->
            val app = results[index]
            AppListItem(app = app, onClickApp = { onClickApp(app) })
        }
    }
}

@Composable
private fun CenteredMessage(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
