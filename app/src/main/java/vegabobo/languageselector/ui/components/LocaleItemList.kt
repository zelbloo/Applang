package vegabobo.languageselector.ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import vegabobo.languageselector.R

/**
 * A single locale (or language group) row.
 *
 * @param isSelected marks the locale currently applied to the app.
 * @param isPinned when non-null, the row shows a pin toggle. Pinning used to be reachable only
 *   by long pressing, which nothing in the UI hinted at; the long press still works.
 */
@Composable
fun LocaleItemList(
    itemText: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isPinned: Boolean? = null,
    onLongClick: () -> Unit = {},
    onTogglePin: () -> Unit = {},
    onClick: () -> Unit
) {
    // ListItem sizes itself around its content instead of the fixed 72dp height this used to
    // have, so long locale names and large font scales no longer get clipped.
    ListItem(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        ),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = {
            Text(
                text = itemText,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
        },
        leadingContent = if (isSelected) {
            {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = stringResource(R.string.current_language),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            null
        },
        trailingContent = if (isPinned != null) {
            {
                IconButton(onClick = onTogglePin) {
                    Icon(
                        imageVector = if (isPinned) Icons.Filled.PushPin
                        else Icons.Outlined.PushPin,
                        contentDescription = stringResource(
                            if (isPinned) R.string.unpin else R.string.pin
                        ),
                        tint = if (isPinned) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            null
        }
    )
}
