package kr.voicemate.malitda.ui.v2

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kr.voicemate.malitda.R
import kr.voicemate.malitda.ui.vm.ListenState

/** U02 · 녹음 중(화면 02) / U02-P · 처리 중(화면 03). listen 상태로 두 모습을 오간다. */
@Composable
fun U02Listen(
    listen: ListenState,
    onStop: () -> Unit,
    onPickCards: () -> Unit,
    onCancel: () -> Unit,
    onHelp: () -> Unit,
) {
    val processing = listen is ListenState.Processing
    V2Screen {
        V2Header(onBack = onCancel, onHelp = onHelp)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!processing) {
                V2Title("천천히 말해 주세요")
                V2SubTitle("편안한 목소리로 말해 주세요.")
                Spacer(Modifier.height(16.dp))
                ListeningCard(listen as? ListenState.Listening)
            } else {
                V2Title("말을 확인하고 있어요")
                Spacer(Modifier.height(16.dp))
                CharacterCard(
                    chip = { Image(painterResource(R.drawable.ic_consent_mic), null, Modifier.size(24.dp)) },
                    caption = "잠시 기다려 주세요.",
                )
            }
            Spacer(Modifier.height(16.dp))
        }
        if (!processing) {
            V2PrimaryButton("말하기 끝내기", icon = "■") { onStop() }
            Spacer(Modifier.height(12.dp))
            V2SecondaryButton("카드로 고르기") { onPickCards() }
        } else {
            V2SecondaryButton("취소하고 카드로 고르기") { onPickCards() }
        }
        V2TextButton("취소") { onCancel() }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ListeningCard(listening: ListenState.Listening?) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.94f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    V2Card {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFFF4F0FB), Color(0xFFF0F4FB))))
                .padding(vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(240.dp).scale(pulse).background(Color(0x22A64DF3), CircleShape))
                Box(Modifier.size(196.dp).scale(pulse).background(Color(0x338B6CF5), CircleShape))
                Box(Modifier.size(150.dp).background(V2C.Surface, CircleShape))
                Box(
                    Modifier.size(132.dp).background(V2Gradient, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Image(painterResource(R.drawable.ic_mic_white), null, Modifier.size(52.dp)) }
                SoundBars(Modifier.align(Alignment.CenterStart))
                SoundBars(Modifier.align(Alignment.CenterEnd))
            }
            Spacer(Modifier.height(4.dp))
            Image(
                painterResource(R.drawable.v2_char_purple), null,
                Modifier
                    .align(Alignment.End)
                    .padding(end = 24.dp)
                    .size(76.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Fit,
            )
            val partial = listening?.partial.orEmpty().ifBlank { listening?.segments.orEmpty() }
            Text("듣고 있어요", style = jua(26, V2C.Title))
            if (partial.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    partial, style = jua(16, V2C.Muted), textAlign = TextAlign.Center, maxLines = 2,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Dots()
            Spacer(Modifier.height(10.dp))
            Text("말을 마치면 말하기 끝내기를 누릅니다.", style = jua(14, V2C.Muted))
        }
    }
}

@Composable
private fun SoundBars(modifier: Modifier) {
    Row(modifier.padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(16, 28, 20).forEach { h ->
            Box(
                Modifier
                    .padding(horizontal = 2.dp)
                    .size(width = 4.dp, height = h.dp)
                    .background(Color(0xFFB3A3EF), RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun Dots() {
    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(9.dp).background(V2C.Title, CircleShape))
        Box(Modifier.size(9.dp).background(Color(0xFFC9BFEA), CircleShape))
        Box(Modifier.size(9.dp).background(Color(0xFFC9BFEA), CircleShape))
    }
}

/** 캐릭터 + 설명 카드(처리 중·오류 계열 공용). */
@Composable
fun CharacterCard(chip: (@Composable () -> Unit)?, caption: String?) {
    V2Card {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFFF7F1FA), Color(0xFFF0F3FC)))),
        ) {
            if (chip != null) {
                Box(
                    Modifier
                        .padding(20.dp)
                        .size(52.dp)
                        .shadow(3.dp, CircleShape, clip = false, spotColor = V2C.Shadow)
                        .background(V2C.Surface, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { chip() }
            }
            Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(if (chip != null) 28.dp else 0.dp))
                Image(
                    painterResource(R.drawable.v2_char_purple), null,
                    Modifier.size(190.dp).clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Fit,
                )
                if (caption != null) {
                    Spacer(Modifier.height(20.dp))
                    Text(caption, style = jua(22, V2C.Title), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                    Dots()
                }
            }
        }
    }
}

/** U02-D · 마이크 권한 없음(화면 14). */
@Composable
fun U02MicDenied(
    onPickCards: () -> Unit,
    onHowToEnable: () -> Unit,
    onCancel: () -> Unit,
    onHelp: () -> Unit,
) {
    V2Screen {
        V2Header(onBack = onCancel, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("마이크를 사용할\n수 없어요")
            Spacer(Modifier.height(12.dp))
            CharacterCard(
                chip = { Icon(Icons.Rounded.Lock, null, tint = V2C.BannerIcon, modifier = Modifier.size(26.dp)) },
                caption = null,
            )
            Spacer(Modifier.height(16.dp))
            V2Banner("카드로 뜻을 골라도 돼요.", leading = { CardGlyph() })
            Spacer(Modifier.height(16.dp))
        }
        V2PrimaryButton("카드로 고르기") { onPickCards() }
        Spacer(Modifier.height(12.dp))
        V2SecondaryButton("권한 켜는 방법") { onHowToEnable() }
        V2TextButton("취소") { onCancel() }
        Spacer(Modifier.height(8.dp))
    }
}
