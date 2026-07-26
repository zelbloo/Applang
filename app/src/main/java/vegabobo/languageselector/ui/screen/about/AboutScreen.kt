package vegabobo.languageselector.ui.screen.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.mikepenz.aboutlibraries.Libs
import vegabobo.languageselector.BuildConfig
import vegabobo.languageselector.R
import vegabobo.languageselector.ui.components.BackButton
import vegabobo.languageselector.ui.components.Title
import vegabobo.languageselector.ui.screen.BaseScreen
import vegabobo.languageselector.ui.screen.main.getAppIcon

private const val REPOSITORY_URL = "https://github.com/VegaBobo/Language-Selector"

@Composable
fun AboutScreen(
    navigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    // Parsed once instead of on every recomposition.
    val libraries = remember(context) {
        Libs.Builder()
            .withJson(
                context.resources.openRawResource(R.raw.aboutlibraries)
                    .bufferedReader()
                    .use { it.readText() }
            )
            .build()
            .libraries
    }

    BaseScreen(
        title = stringResource(R.string.about),
        navIcon = { BackButton { navigateBack() } }
    ) { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {
            item {
                val icon = remember {
                    context.packageManager
                        .getAppIcon(context.applicationInfo)
                        .toBitmap()
                        .asImageBitmap()
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        modifier = Modifier.size(88.dp),
                        bitmap = icon,
                        // Decorative: the app name is right below it.
                        contentDescription = null
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = stringResource(R.string.version).format(
                            BuildConfig.VERSION_NAME,
                            BuildConfig.VERSION_CODE
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item { Title(stringResource(R.string.app)) }
            item {
                PreferenceItem(
                    title = stringResource(R.string.ghrepo),
                    description = stringResource(R.string.view_source),
                    onClick = { uriHandler.openUri(REPOSITORY_URL) }
                )
            }

            item { Title(stringResource(R.string.deps_libs)) }
            items(libraries.size) { index ->
                val library = libraries[index]
                val url = library.website?.takeIf { it.isNotBlank() }
                PreferenceItem(
                    title = library.name,
                    // joinToString, so two licenses no longer render as "Apache-2.0MIT".
                    description = library.licenses.joinToString { it.name },
                    onClick = url?.let { { uriHandler.openUri(it) } }
                )
            }
        }
    }
}

@Composable
fun PreferenceItem(
    title: String,
    description: String,
    onClick: (() -> Unit)? = null
) {
    ListItem(
        // Only clickable when there is somewhere to go; entries without a website used to show
        // a ripple and then do nothing.
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(title) },
        supportingContent = if (description.isNotBlank()) {
            { Text(description) }
        } else {
            null
        }
    )
}
