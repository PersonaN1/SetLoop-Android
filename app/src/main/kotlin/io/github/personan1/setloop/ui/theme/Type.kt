package io.github.personan1.setloop.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.personan1.setloop.R

/** Nunito (SIL OFL): rounded stand-in for SF Pro Rounded. Its digits are tabular at every weight. */
val RoundedFontFamily = FontFamily(
    Font(R.font.nunito, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.nunito, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.nunito, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
)

private val Default = Typography()

/**
 * DESIGN V7 mapped onto Material roles. All sizes are sp, so the system font scale applies.
 * - Large Title 34: headlineLarge, and headlineMedium because `LargeTopAppBar` draws its expanded title with it.
 * - Headline 17 semibold: titleLarge (collapsed top bar title), titleMedium.
 * - Body 17: bodyLarge. Subheadline 15: bodyMedium. Footnote 13: bodySmall. Caption 2 11: labelSmall, labelMedium.
 */
val SetLoopTypography = Typography(
    displayLarge = Default.displayLarge,
    displayMedium = Default.displayMedium,
    displaySmall = Default.displaySmall,
    headlineLarge = TextStyle(fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Normal),
)

/** Rounded tabular styles for timer digits and big workout titles (DESIGN V3). */
object TimerTextStyles {
    private const val TABULAR = "tnum"

    /** Countdown on the workout screen, ~96 sp in portrait. */
    val workoutDigits = TextStyle(
        fontFamily = RoundedFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 96.sp,
        lineHeight = 104.sp,
        fontFeatureSettings = TABULAR,
    )

    /** Step name on the workout screen. */
    val workoutTitle = TextStyle(
        fontFamily = RoundedFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 41.sp,
    )

    /** Total time in a summary card (preview, workout summary). */
    val total = TextStyle(
        fontFamily = RoundedFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        fontFeatureSettings = TABULAR,
    )

    /** Metric values, durations in lists. */
    val metric = TextStyle(
        fontFamily = RoundedFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        fontFeatureSettings = TABULAR,
    )
}
