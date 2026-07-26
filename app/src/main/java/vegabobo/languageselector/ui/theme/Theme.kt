package vegabobo.languageselector.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * The app targets Android 13 and up, so Material You dynamic color is always available and is
 * used unconditionally: no hand-picked palette can match the wallpaper-derived one the rest of
 * the system uses.
 *
 * Typography, shapes and motion intentionally stay on the defaults rather than being partially
 * overridden — as of Material 3 1.4 those defaults are the expressive ones.
 */
@Composable
fun LanguageSelectorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme =
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
