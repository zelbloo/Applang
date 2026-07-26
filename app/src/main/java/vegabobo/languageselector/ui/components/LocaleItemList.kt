package vegabobo.languageselector.ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun LocaleItemList(
    itemText: String,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
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
        headlineContent = { Text(itemText) }
    )
}
