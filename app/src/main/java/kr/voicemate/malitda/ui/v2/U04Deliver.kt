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
    // 오인식 학습 제안: 말로 시도했는데 카드로 고친 경우에만 들어온다.
    learnHeard: String? = null,
    onLearn: () -> Unit = {},
    // 실증 기록 입력: 기록이 켜져 있을 때만 들어온다(-1 기록 안 함, 1 예, 0 아니요).
    trialUnderstood: Int? = null,
    trialHelped: Boolean = false,
    onTrialUnderstood: (Boolean?) -> Unit = {},
    onTrialHelped: (Boolean) -> Unit = {},
    // 전달을 마치고 바로 홈으로(2026-10-26 실기기 피드백).
    onHome: (() -> Unit)? = null,
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
                            .height(52.dp)
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
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TrialChip("이해했어요", trialUnderstood == 1, Modifier.weight(1f)) {
                            onTrialUnderstood(if (trialUnderstood == 1) null else true)
                        }
                        TrialChip("못 했어요", trialUnderstood == 0, Modifier.weight(1f)) {
                            onTrialUnderstood(if (trialUnderstood == 0) null else false)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    TrialChip(if (trialHelped) "✓ 지원자가 도와줬어요" else "지원자가 도와줬어요", trialHelped, Modifier.fillMaxWidth()) {
                        onTrialHelped(!trialHelped)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        if (!soundless) {
            V2PrimaryButton(if (speaking) "들려주는 중…" else "소리로 들려주기", icon = "🔊", enabled = !speaking) { onSound() }
        } else {
            V2PrimaryButton("그림으로 보여주는 중") { }
        }
        Spacer(Modifier.height(12.dp))
        V2SecondaryButton("다시 고르기") { onRepick() }
        if (onHome != null) {
            // 한 줄에 나란히 둬 화면 높이를 늘리지 않는다(실증 기록 칸이 버튼에 가리지 않게).
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                V2TextButton("다른 앱으로 보내기", Modifier.weight(1f)) { onShare() }
                V2TextButton("⌂ 처음으로", Modifier.weight(1f)) { onHome() }
            }
        } else {
            V2TextButton("다른 앱으로 보내기") { onShare() }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TrialChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(48.dp)
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
    onCancel: () -> Unit,
    onHelp: () -> Unit,
    onHome: (() -> Unit)? = null,
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
        onHome?.let { home -> V2TextButton("⌂ 처음으로") { home() } }
        Spacer(Modifier.height(16.dp))
    }
}
