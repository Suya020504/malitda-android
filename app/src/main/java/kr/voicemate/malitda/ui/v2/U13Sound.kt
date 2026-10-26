package kr.voicemate.malitda.ui.v2

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** U13 · 소리로 확인해요(화면 38~41) — 뜻을 소리로 재생하는 화면. 인디케이터를 누르면 다시 들려준다. */
@Composable
fun U13Sound(
    meaningText: String,
    imageKey: String?,
    speaking: Boolean,
    onReplay: () -> Unit,
    onClose: () -> Unit,
    onHelp: () -> Unit,
) {
    V2Screen {
        V2Header(onBack = onClose, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("소리로 확인해요")
            Spacer(Modifier.height(10.dp))
            V2Card {
                Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    MeaningPicture(imageKey, Modifier.fillMaxWidth().aspectRatio(1.42f).clip(RoundedCornerShape(24.dp)), glyphSize = 44)
                    Spacer(Modifier.height(22.dp))
                    Text(meaningText, style = jua(30, V2C.Ink), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(22.dp))
                    SoundIndicator(speaking = speaking, onClick = onReplay)
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            V2Banner("그림과 문장도 함께\n확인해 주세요.", leading = { CardGlyph() })
            Spacer(Modifier.height(16.dp))
        }
        V2PrimaryButton("닫기") { onClose() }
        Spacer(Modifier.height(16.dp))
    }
}

/** 소리 재생 알약: 막대-스피커-막대. 재생 중에는 맥동한다. 누르면 다시 재생. */
@Composable
private fun SoundIndicator(speaking: Boolean, onClick: () -> Unit) {
    val motion = rememberV2MotionEnabled()
    val barScale = if (speaking && motion) {
        val value by rememberInfiniteTransition(label = "sound").animateFloat(
            initialValue = 0.92f, targetValue = 1.08f,
            animationSpec = infiniteRepeatable(tween(650, easing = androidx.compose.animation.core.FastOutSlowInEasing), RepeatMode.Reverse),
            label = "sound",
        )
        value
    } else 1f
    Row(
        Modifier
            .background(
                Brush.horizontalGradient(listOf(Color(0xFFF1E9FB), Color(0xFFEDEFFB))),
                RoundedCornerShape(28.dp),
            )
            .clip(RoundedCornerShape(28.dp))
            .clickable { onClick() }
            .padding(horizontal = 34.dp, vertical = 16.dp)
            .semantics { contentDescription = if (speaking) "들려주는 중" else "다시 들려주기" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Bars(barScale)
        Spacer(Modifier.width(22.dp))
        Text("🔊", style = jua(24, V2C.Title))
        Spacer(Modifier.width(22.dp))
        Bars(barScale)
    }
}

@Composable
private fun Bars(scaleY: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        listOf(14, 24, 18).forEach { h ->
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .size(width = 5.dp, height = h.dp)
                    .scale(scaleX = 1f, scaleY = scaleY)
                    .background(Color(0xFF9B8CE8), RoundedCornerShape(2.5.dp))
            )
        }
    }
}
