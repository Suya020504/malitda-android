package kr.voicemate.malitda.ui.v2

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kr.voicemate.malitda.data.db.MeaningEntity

/** U04 · 이렇게 전달해요(화면 05·08·10·12). */
@Composable
fun U04Deliver(
    meaning: MeaningEntity,
    speaking: Boolean,
    soundless: Boolean,
    onSound: () -> Unit,
    onRepick: () -> Unit,
    onShare: () -> Unit,
    onBack: () -> Unit,
    onHelp: () -> Unit,
) {
    V2Screen {
        V2Header(onBack = onBack, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("이렇게 전달해요")
            Spacer(Modifier.height(10.dp))
            MeaningBigCard(meaning)
            Spacer(Modifier.height(14.dp))
            V2Banner("내가 확인한 뜻이에요.", green = true, icon = Icons.Rounded.Check)
            Spacer(Modifier.height(16.dp))
        }
        if (!soundless) {
            V2PrimaryButton(if (speaking) "들려주는 중…" else "소리로 들려주기", icon = "🔊", enabled = !speaking) { onSound() }
        } else {
            V2PrimaryButton("그림으로 보여주는 중") { }
        }
        Spacer(Modifier.height(12.dp))
        V2SecondaryButton("다시 고르기") { onRepick() }
        V2TextButton("다른 앱으로 보내기") { onShare() }
        Spacer(Modifier.height(8.dp))
    }
}

/** U04-F · 소리가 나오지 않아요(화면 34). */
@Composable
fun U04NoSound(
    meaning: MeaningEntity,
    onKeepVisual: () -> Unit,
    onSoundSettings: () -> Unit,
    onBack: () -> Unit,
    onHelp: () -> Unit,
) {
    V2Screen {
        V2Header(onBack = onBack, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("소리가 나오지 않아요")
            Spacer(Modifier.height(10.dp))
            MeaningBigCard(meaning)
            Spacer(Modifier.height(14.dp))
            V2Banner("그림과 문장으로\n전달할 수 있어요.", leading = { CardGlyph() })
            Spacer(Modifier.height(16.dp))
        }
        V2PrimaryButton("그림으로 계속 보여주기") { onKeepVisual() }
        Spacer(Modifier.height(12.dp))
        V2SecondaryButton("소리 설정 안내") { onSoundSettings() }
        Spacer(Modifier.height(16.dp))
    }
}

/** U04-S · 다른 앱으로 보내기(화면 35). 공유 시트가 열린 동안의 안내. */
@Composable
fun U04Share(
    meaning: MeaningEntity,
    onReturn: () -> Unit,
    onCancel: () -> Unit,
    onHelp: () -> Unit,
) {
    V2Screen {
        V2Header(onBack = onReturn, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("다른 앱으로 보내기")
            Spacer(Modifier.height(10.dp))
            V2Card {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(Color(0xFFF8F0F7), Color(0xFFF0F3FC), Color(0xFFF2FBF4))))
                        .padding(vertical = 44.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .size(96.dp)
                            .shadow(4.dp, RoundedCornerShape(28.dp), clip = false, spotColor = V2C.Shadow)
                            .background(V2C.Surface, RoundedCornerShape(28.dp)),
                        contentAlignment = Alignment.Center,
                    ) { CardGlyph(size = 40) }
                    Spacer(Modifier.height(40.dp))
                    Text(meaning.text, style = jua(30, V2C.Ink), textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(14.dp))
            V2Banner("공유할 앱은 휴대전화에서 선택해요.\n선택을 취소해도 뜻은 그대로예요.", icon = Icons.Rounded.Info)
            Spacer(Modifier.height(16.dp))
        }
        V2PrimaryButton("공유창에서 돌아오기") { onReturn() }
        Spacer(Modifier.height(12.dp))
        V2SecondaryButton("취소") { onCancel() }
        Spacer(Modifier.height(16.dp))
    }
}
