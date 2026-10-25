package kr.voicemate.malitda.ui.v2

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Ending an observed failed task is distinct from withdrawing, fatigue or a safety stop. */
@Composable
fun PilotTaskCompletion(delivered: Boolean, judgedCorrect: Int, understood: Int, helped: Boolean,
    onJudged: (Boolean?) -> Unit, onUnderstood: (Boolean?) -> Unit, onHelped: (Boolean) -> Unit,
    onFinish: () -> Unit, onStop: () -> Unit, onBack: () -> Unit, onHelp: () -> Unit) {
    V2Screen {
        V2Header(onBack = onBack, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("이 과업의 기록을 마칩니다")
            V2Banner("전달하지 못한 과업도 시험을 마쳤다면 완료로 기록합니다. 참여자 요청·피로·안전 문제로 멈춘 경우에는 중단을 선택합니다.")
            V2SubTitle(if (delivered) "앱의 승인 후 전달 화면에 도달했습니다. 실제 외부 전송을 뜻하지 않습니다."
                else "앱의 승인 후 전달 화면에 도달하지 못했습니다.")
            Text("계획한 뜻을 앱으로 전했습니까?", style = jua(20, V2C.Ink), modifier = Modifier.padding(24.dp))
            V2SecondaryButton(if (judgedCorrect == 1) "✓ 목표 뜻을 전함" else "목표 뜻을 전함") { onJudged(if (judgedCorrect == 1) null else true) }
            V2SecondaryButton(if (judgedCorrect == 0) "✓ 목표 뜻을 전하지 못함" else "목표 뜻을 전하지 못함") { onJudged(if (judgedCorrect == 0) null else false) }
            V2SubTitle("관찰하지 않은 항목은 선택하지 않습니다.")
            V2SecondaryButton(if (understood == 1) "✓ 상대가 이해함" else "상대가 이해함") { onUnderstood(if (understood == 1) null else true) }
            V2SecondaryButton(if (understood == 0) "✓ 상대가 이해하지 못함" else "상대가 이해하지 못함") { onUnderstood(if (understood == 0) null else false) }
            V2TextButton(if (helped) "✓ 지원자가 도와줌" else "지원자가 도와줌") { onHelped(!helped) }
            Spacer(Modifier.height(16.dp))
            V2PrimaryButton("과업 마침 기록", onClick = onFinish)
            V2TextButton("실증 중단으로 기록", onClick = onStop)
            V2TextButton("아직 진행 중 · 돌아가기", onClick = onBack)
        }
    }
}
