package kr.voicemate.malitda.ui.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kr.voicemate.malitda.domain.TrialSummary

/**
 * 설정 › 실증 기록(디자인 42화면 밖의 추가 화면).
 * 중간심사 의견 "효과 가설 명확화"에 답하기 위한 대화 시도 기록. 기본 꺼짐.
 */
@Composable
fun U14TrialLog(
    studentName: String,
    enabled: Boolean,
    summary: TrialSummary,
    onToggle: (Boolean) -> Unit,
    onExport: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    onHelp: () -> Unit,
) {
    var confirmClear by remember { mutableStateOf(false) }
    V2Screen {
        V2Header(onBack = onBack, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("실증 기록")
            V2SubTitle("대화가 실제로 전달됐는지 기록해요.")
            Spacer(Modifier.height(14.dp))
            // 켜기 스위치
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(if (enabled) V2C.GreenBg else V2C.Surface, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onToggle(!enabled) }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (enabled) "기록하는 중" else "기록 꺼짐", style = jua(19, if (enabled) V2C.GreenText else V2C.Ink), modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(width = 52.dp, height = 30.dp)
                        .background(if (enabled) V2C.GreenText else Color(0xFFCFC9E0), RoundedCornerShape(15.dp)),
                    contentAlignment = if (enabled) Alignment.CenterEnd else Alignment.CenterStart,
                ) { Box(Modifier.padding(3.dp).size(24.dp).background(Color.White, CircleShape)) }
            }
            Spacer(Modifier.height(12.dp))
            V2Banner(
                "켜면 대화 한 번마다 시작 방식, 전달 여부,\n상대 이해, 지원자 도움만 남겨요.\n학생이 한 말과 들은 문장은 남기지 않아요.",
                icon = Icons.Rounded.Info,
            )
            Spacer(Modifier.height(16.dp))
            Text("${studentName.ifBlank { "친구" }} 기록 요약", style = jua(18, V2C.Ink), modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(V2C.Surface, RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                StatRow("대화 시도", "${summary.total}회")
                StatRow("뜻을 확정해 전달", ratio(summary.delivered, summary.total))
                StatRow("상대가 이해함", ratio(summary.understoodYes, summary.understoodAnswered) + "  (응답 ${summary.understoodAnswered}회 중)")
                StatRow("말로 바로 맞힘", ratio(summary.voiceFirstHit, summary.voiceTrials) + "  (말하기 ${summary.voiceTrials}회 중)")
                StatRow("말하다 카드로 바꿈", "${summary.cardFallback}회")
                StatRow("지원자 도움", "${summary.supporterHelped}회")
            }
            Spacer(Modifier.height(16.dp))
        }
        V2PrimaryButton("CSV로 내보내기") { onExport() }
        V2TextButton(if (confirmClear) "한 번 더 누르면 이 학생 기록이 지워져요" else "이 학생 기록 지우기") {
            if (confirmClear) { onClear(); confirmClear = false } else confirmClear = true
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = jua(16, V2C.Muted), modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(value, style = jua(17, V2C.Ink))
    }
}

private fun ratio(n: Int, d: Int): String = if (d == 0) "-" else "$n/$d (${n * 100 / d}%)"
