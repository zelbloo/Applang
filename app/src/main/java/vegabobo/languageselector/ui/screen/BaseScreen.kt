package vegabobo.languageselector.ui.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow

/**
 * Scaffold for the secondary screens: a collapsing large top app bar over scrollable content.
 * The content receives the scaffold padding and is expected to apply it as its contentPadding.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseScreen(
    title: String,
    modifier: Modifier = Modifier,
    navIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(text = title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = navIcon,
                actions = actions,
                scrollBehavior = scrollBehavior
            )
        },
        content = content
    )
}
