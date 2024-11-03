package org.fcitx.fcitx5.android.updater.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DarkColors = darkColorScheme(
    primary = Purple200,
    //primaryVariant = Purple700,
    secondary = Teal200
)

private val LightColors = lightColorScheme(
    primary = Purple500,
    // primaryVariant = Purple700,
    secondary = Teal200
)

val DenseTypography = Typography(
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp
    )
)

private val SharpShapes = Shapes(
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(2.dp),
    large = RoundedCornerShape(0.dp),
)

@Composable
fun Fcitx5ForAndroidUpdaterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = when {
        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) -> {
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = DenseTypography,
        shapes = SharpShapes,
        content = content
    )
}
