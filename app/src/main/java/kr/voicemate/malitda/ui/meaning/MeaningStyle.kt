package kr.voicemate.malitda.ui.meaning

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.voicemate.malitda.R

internal object MeaningColors {
    val Background = Color(0xFFFFFCFB)
    val Ink = Color(0xFF172542)
    val Muted = Color(0xFF5C6882)
    val Purple = Color(0xFF7040ED)
    val Lavender = Color(0xFFF3EEFF)
    val Line = Color(0xFFE3DAF5)
    val Mint = Color(0xFFE7F8F3)
    val MintInk = Color(0xFF126455)
    val Danger = Color(0xFFB3261E)
}

private val MeaningFont = FontFamily(
    Font(R.font.ibm_plex_sans_kr_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_kr_semibold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans_kr_semibold, FontWeight.Bold),
)

private val MeaningTypography = Typography(
    headlineLarge = TextStyle(fontFamily = MeaningFont, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = MeaningFont, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = MeaningFont, fontWeight = FontWeight.SemiBold, fontSize = 23.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = MeaningFont, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 31.sp),
    titleMedium = TextStyle(fontFamily = MeaningFont, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 29.sp),
    titleSmall = TextStyle(fontFamily = MeaningFont, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 27.sp),
    bodyLarge = TextStyle(fontFamily = MeaningFont, fontSize = 19.sp, lineHeight = 29.sp),
    bodyMedium = TextStyle(fontFamily = MeaningFont, fontSize = 18.sp, lineHeight = 27.sp),
    bodySmall = TextStyle(fontFamily = MeaningFont, fontSize = 16.sp, lineHeight = 25.sp),
    labelLarge = TextStyle(fontFamily = MeaningFont, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 29.sp),
    labelMedium = TextStyle(fontFamily = MeaningFont, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 25.sp),
    labelSmall = TextStyle(fontFamily = MeaningFont, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
)

/** The local meaning flow has its own accessible, scalable typography. */
@Composable
internal fun MeaningTheme(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val fontScale = maxOf(density.fontScale, LocalConfiguration.current.fontScale)
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
      MaterialTheme(
        colorScheme = lightColorScheme(
            primary = MeaningColors.Purple,
            onPrimary = Color.White,
            primaryContainer = MeaningColors.Lavender,
            onPrimaryContainer = MeaningColors.Ink,
            secondary = MeaningColors.MintInk,
            secondaryContainer = MeaningColors.Mint,
            background = MeaningColors.Background,
            onBackground = MeaningColors.Ink,
            surface = Color.White,
            onSurface = MeaningColors.Ink,
            onSurfaceVariant = MeaningColors.Muted,
            outline = MeaningColors.Muted,
            outlineVariant = MeaningColors.Line,
            error = MeaningColors.Danger,
        ),
        typography = MeaningTypography,
        content = content,
      )
    }
}

@Composable
private fun MeaningFrame(
    onBack: (() -> Unit)? = null,
    headerEnd: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    MeaningTheme {
        Column(
            Modifier.fillMaxSize().background(MeaningColors.Background)
                .safeDrawingPadding().imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier.widthIn(max = 640.dp).fillMaxWidth().heightIn(min = 64.dp)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "이전 화면으로", tint = MeaningColors.Ink)
                    }
                }
                Box(Modifier.weight(1f)) {
                    Image(
                        painterResource(R.drawable.logo_header),
                        contentDescription = "말잇다",
                        modifier = Modifier.height(36.dp).widthIn(max = 150.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                headerEnd?.invoke()
            }
            content()
        }
    }
}

/** Long libraries only compose the cards currently visible on the device. */
@Composable
internal fun MeaningLazyPage(
    title: String? = null,
    onBack: (() -> Unit)? = null,
    headerEnd: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    MeaningFrame(onBack, headerEnd) {
        LazyColumn(
            Modifier.weight(1f).widthIn(max = 640.dp).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (title != null) {
                item(key = "meaning-page-title") {
                    Text(title, style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() })
                }
            }
            content()
        }
    }
}

/** Actions stay in the scroll area so large system fonts and the IME never hide them. */
@Composable
internal fun MeaningPage(
    title: String? = null,
    onBack: (() -> Unit)? = null,
    headerEnd: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    MeaningFrame(onBack, headerEnd) {
        Column(
            Modifier.weight(1f).widthIn(max = 640.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() })
            }
            content()
        }
    }
}

/*
 * The frame deliberately has no fixed bottom actions: reading and confirming
 * remain reachable even with the keyboard or large font settings enabled.
 */

@Composable
internal fun MeaningAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    secondary: Boolean = false,
    quiet: Boolean = false,
    danger: Boolean = false,
    icon: ImageVector? = null,
) {
    val color = if (danger) MeaningColors.Danger else MeaningColors.Purple
    val shape = RoundedCornerShape(20.dp)
    val content: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f))
        }
    }
    val m = modifier.fillMaxWidth().heightIn(min = if (quiet) 48.dp else 58.dp)
    when {
        quiet -> TextButton(onClick, m, enabled = enabled,
            colors = ButtonDefaults.textButtonColors(contentColor = color),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) { content() }
        secondary -> OutlinedButton(onClick, m, enabled = enabled, shape = shape,
            border = BorderStroke(1.dp, if (enabled) MeaningColors.Line else Color.LightGray),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) { content() }
        else -> Button(onClick, m, enabled = enabled, shape = shape,
            colors = ButtonDefaults.buttonColors(containerColor = color),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) { content() }
    }
}

@Composable
internal fun MeaningNotice(text: String, mint: Boolean = false, error: Boolean = false) {
    Surface(
        modifier = Modifier.fillMaxWidth().semantics { if (error) liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(18.dp),
        color = when { error -> Color(0xFFFFEDEA); mint -> MeaningColors.Mint; else -> MeaningColors.Lavender },
    ) {
        Text(text, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium,
            color = when { error -> MeaningColors.Danger; mint -> MeaningColors.MintInk; else -> MeaningColors.Ink })
    }
}

private sealed interface LocalPicture {
    data object Loading : LocalPicture
    data object Missing : LocalPicture
    data class Loaded(val bitmap: ImageBitmap) : LocalPicture
}

/** Only immutable media inside this app's means_media directory can be loaded. */
private fun readLocalPicture(filesDir: File, imageRef: String): ImageBitmap? {
    val relative = imageRef.removePrefix("file:").replace('\\', '/')
    if (!relative.startsWith("means_media/")) return null
    return try {
        val mediaDir = File(filesDir, "means_media").canonicalFile
        val file = File(filesDir, relative).canonicalFile
        if (!file.path.startsWith(mediaDir.path + File.separator) || !file.isFile) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > 1200 || bounds.outHeight / sample > 1200) sample *= 2
        BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

@Composable
internal fun MeaningPicture(imageRef: String?, modifier: Modifier = Modifier, compact: Boolean = false) {
    val builtin = when (imageRef) {
        "builtin:walk" -> R.drawable.meaning_walk
        "builtin:water" -> R.drawable.meaning_water
        "builtin:rest" -> R.drawable.meaning_rest
        "builtin:cold" -> R.drawable.meaning_cold
        else -> null
    }
    val imageModifier = modifier.clip(RoundedCornerShape(16.dp)).background(MeaningColors.Lavender)
    if (builtin != null) {
        Image(painterResource(builtin), contentDescription = null,
            modifier = imageModifier, contentScale = ContentScale.Crop)
        return
    }
    val context = LocalContext.current
    key(imageRef) {
      val picture by produceState<LocalPicture>(
          if (imageRef?.startsWith("file:") == true) LocalPicture.Loading else LocalPicture.Missing,
          context.filesDir,
      ) {
          value = if (imageRef?.startsWith("file:") == true) {
              val bitmap = withContext(Dispatchers.IO) { readLocalPicture(context.filesDir, imageRef) }
              bitmap?.let { LocalPicture.Loaded(it) } ?: LocalPicture.Missing
          } else LocalPicture.Missing
      }
      Box(imageModifier, contentAlignment = Alignment.Center) {
        when (val state = picture) {
            is LocalPicture.Loaded -> Image(state.bitmap, contentDescription = null,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else -> if (compact) {
                Icon(Icons.Rounded.Image,
                    contentDescription = if (state == LocalPicture.Loading) "그림을 불러오는 중" else "그림 없음. 문장으로 확인해요",
                    tint = MeaningColors.Muted, modifier = Modifier.size(32.dp))
            } else {
                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Image, contentDescription = null, tint = MeaningColors.Muted)
                    Text(if (state == LocalPicture.Loading) "그림을 불러와요" else "문장으로 확인해요",
                        style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                        color = MeaningColors.Muted)
                }
            }
        }
      }
    }
}
