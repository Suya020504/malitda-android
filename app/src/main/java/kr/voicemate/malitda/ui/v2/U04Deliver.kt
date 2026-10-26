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
import androidx.compose.foundation.layout.heightIn
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
    // 오인식 학습 제안: 말로 시도했는데 카드로 고친 경우에만 들어온다.
    learnHeard: String? = null,
    onLearn: () -> Unit = {},
    // 실증 기록 입력: 기록이 켜져 있을 때만 들어온다(-1 기록 안 함, 1 예, 0 아니요).
    trialUnderstood: Int? = null,
    trialHelped: Boolean = false,
    onTrialUnderstood: (Boolean?) -> Unit = {},
    onTrialHelped: (Boolean) -> Unit = {},
    onDone: () -> Unit = {},
    onFeedback: (() -> Unit)? = null,
    onStopSound: () -> Unit = {},
    onCopy: () -> Unit = {},
) {
    V2Screen {
        V2Header(onBack = onBack, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("이렇게 전달해요")
            Spacer(Modifier.height(10.dp))
            // 지원자 칸(오인식 연결·실증 기록)이 뜨면 그림을 낮춰 아래 버튼에 가리지 않게 한다.
            MeaningBigCard(meaning, imageAspect = if (learnHeard != null || trialUnderstood != null) 2.2f else 1.42f)
            Spacer(Modifier.height(14.dp))
            V2Banner("내가 확인한 뜻이에요.", green = true, icon = Icons.Rounded.Check)
            if (learnHeard != null) {
                Spacer(Modifier.height(14.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .background(V2C.BannerBg, RoundedCornerShape(22.dp))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    Text("지원자 확인", style = jua(14, V2C.Muted))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "방금 “$learnHeard”(이)라고 들렸어요.\n다음부터 이 말도 이 뜻으로 알아들을까요?",
                        style = jua(17, V2C.Ink, 1.5f),
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp).padding(vertical = 10.dp)
                            .background(V2C.Surface, RoundedCornerShape(26.dp))
                            .clip(RoundedCornerShape(26.dp))
                            .clickable { onLearn() },
                        contentAlignment = Alignment.Center,
                    ) { Text("이 뜻에 “$learnHeard” 추가", style = jua(17, V2C.Title)) }
                }
            }
            if (trialUnderstood != null) {
                Spacer(Modifier.height(14.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .background(V2C.Surface, RoundedCornerShape(22.dp))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                ) {
                    Text("실증 기록 · 지원자", style = jua(14, V2C.Muted))
                    Spacer(Modifier.height(8.dp))
                    Text("상대가 뜻을 이해했나요?", style = jua(17, V2C.Ink))
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TrialChip("이해했어요", trialUnderstood == 1, Modifier.fillMaxWidth()) {
                            onTrialUnderstood(if (trialUnderstood == 1) null else true)
                        }
                        TrialChip("못 했어요", trialUnderstood == 0, Modifier.fillMaxWidth()) {
                            onTrialUnderstood(if (trialUnderstood == 0) null else false)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    TrialChip(if (trialHelped) "✓ 지원자가 도와줬어요" else "지원자가 도와줬어요", trialHelped, Modifier.fillMaxWidth()) {
                        onTrialHelped(!trialHelped)
                    }
                }
            }
            if (speaking) V2SecondaryButton("소리 멈추기", onClick = onStopSound)
            V2TextButton("문장 복사", onClick = onCopy)
            if (onFeedback != null) V2TextButton("실증 응답 기록 · 지원자", onClick = onFeedback)
            Spacer(Modifier.height(16.dp))
        }
        if (!soundless) {
            V2PrimaryButton(if (speaking) "들려주는 중…" else "소리로 들려주기", icon = "🔊", enabled = !speaking) { onSound() }
        } else {
            Text("그림과 문장으로 보여주고 있습니다.", style = jua(18, V2C.Ink), modifier = Modifier.padding(horizontal = 24.dp))
        }
        Spacer(Modifier.height(12.dp))
        V2SecondaryButton("다시 고르기") { onRepick() }
        V2TextButton("다른 앱으로 보내기") { onShare() }
        V2TextButton("마치고 홈으로", onClick = onDone)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TrialChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .heightIn(min = 48.dp).padding(vertical = 10.dp)
            .background(if (selected) V2C.GreenBg else V2C.BannerBg, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(label, style = jua(16, if (selected) V2C.GreenText else V2C.Title)) }
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
    onHome: () -> Unit,
    onHelp: () -> Unit,
) {
    V2Screen {
        V2Header(onBack = onReturn, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("공유 뒤 이어가기")
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
            V2Banner("공유창을 열어도 실제 전송 완료는 확인할 수 없습니다. 다시 전달하려면 뜻을 확인합니다.", icon = Icons.Rounded.Info)
            Spacer(Modifier.height(16.dp))
        }
        V2PrimaryButton("홈으로") { onHome() }
        Spacer(Modifier.height(12.dp))
        V2SecondaryButton("뜻 다시 확인하기") { onReturn() }
        Spacer(Modifier.height(16.dp))
    }
}
