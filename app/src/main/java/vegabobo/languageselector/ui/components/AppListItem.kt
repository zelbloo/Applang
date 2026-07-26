package vegabobo.languageselector.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import vegabobo.languageselector.R
import vegabobo.languageselector.ui.screen.main.AppInfo

@Composable
fun AppListItem(
    modifier: Modifier = Modifier,
    app: AppInfo,
    onClickApp: (String) -> Unit
) {
    // Rasterising the launcher icon is expensive; without remember it happened on every
    // recomposition, for every visible row.
    val icon = remember(app.icon) { app.icon.toBitmap().asImageBitmap() }

    ListItem(
        modifier = modifier.clickable { onClickApp(app.pkg) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Image(
                modifier = Modifier.size(40.dp),
                bitmap = icon,
                // Decorative: the app name is right next to it.
                contentDescription = null
            )
        },
        headlineContent = {
            Text(
                text = app.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = app.pkg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppTag(
                        text = stringResource(
                            if (app.isSystemApp()) R.string.label_system_app
                            else R.string.label_user_app
                        ),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (app.isModified())
                        AppTag(
                            text = stringResource(R.string.label_modified),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                }
            }
        }
    )
}

@Composable
private fun AppTag(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = containerColor,
        contentColor = contentColor
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            text = text,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}
