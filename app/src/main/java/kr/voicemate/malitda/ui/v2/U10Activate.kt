package kr.voicemate.malitda.ui.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kr.voicemate.malitda.domain.StudentMeaningConfirmation
import kr.voicemate.malitda.domain.StudentMeaningResponse
import kr.voicemate.malitda.domain.StudentConfirmationMethod

/** 학생 반응과 확인 방법을 구분하고 판단이 어려우면 미확인으로 둔다. */
@Composable
fun U10Activate(
    studentName: String, expression: String, meaningText: String, imageKey: String?,
    confirmation: StudentMeaningConfirmation, saving: Boolean,
    onResponse: (StudentMeaningResponse) -> Unit, onMethod: (StudentConfirmationMethod) -> Unit,
    onListen: () -> Unit, onUse: () -> Unit, onUseAndHome: () -> Unit, onLater: () -> Unit,
    onBack: () -> Unit, onHelp: () -> Unit,
) {
    V2Screen {
        V2Header(onBack = { if (!saving) onBack() }, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("학생과 뜻 확인")
            V2SubTitle("$studentName · 평소 표현: $expression")
            Spacer(Modifier.height(12.dp))
            V2Card {
                Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    MeaningPicture(imageKey, Modifier.fillMaxWidth().aspectRatio(2.1f).clip(RoundedCornerShape(22.dp)))
                    Spacer(Modifier.height(12.dp))
                    Text(meaningText, style = jua(26, V2C.Ink), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            V2SecondaryButton("소리로 확인하기", onClick = onListen)
            Spacer(Modifier.height(18.dp))
            Text("학생은 어떤 반응을 보였나요?", style = jua(19, V2C.Ink), modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(8.dp))
            ConfirmationChoices(
                listOf(StudentMeaningResponse.MATCHED to "맞는 뜻", StudentMeaningResponse.DIFFERENT to "다른 뜻",
                    StudentMeaningResponse.UNSURE to "모르겠음", StudentMeaningResponse.NO_RESPONSE to "응답 확인 안 됨"),
                confirmation.response, !saving, onResponse,
            )
            if (confirmation.response == StudentMeaningResponse.MATCHED) {
                Spacer(Modifier.height(16.dp))
                Text("학생이 뜻을 고른 방법", style = jua(19, V2C.Ink), modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(8.dp))
                ConfirmationChoices(
                    listOf(StudentConfirmationMethod.SPEECH to "말로 답함", StudentConfirmationMethod.POINTING to "가리켜 고름",
                        StudentConfirmationMethod.GAZE to "시선으로 고름", StudentConfirmationMethod.OTHER to "정해 둔 신호"),
                    confirmation.method, !saving, onMethod,
                )
            }
            Spacer(Modifier.height(14.dp))
            V2Banner(if (confirmation.response != null && confirmation.response != StudentMeaningResponse.MATCHED)
                "지금은 확정하지 않습니다.\n미확인으로 두고 나중에 다시 확인합니다."
                else "학생의 선택을 먼저 확인합니다.\n지원자의 추측만으로 확정하지 않습니다.")
            if (confirmation.canActivate && !saving) {
                Spacer(Modifier.height(12.dp))
                V2SecondaryButton("확인하고 홈으로", onClick = onUseAndHome)
            }
            Spacer(Modifier.height(16.dp))
        }
        V2PrimaryButton(if (saving) "저장 중…" else "확인한 뜻 사용하기", enabled = confirmation.canActivate && !saving, onClick = onUse)
        V2TextButton("미확인으로 두기", enabled = !saving, onClick = onLater)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun <T> ConfirmationChoices(options: List<Pair<T, String>>, selected: T?, enabled: Boolean, onSelect: (T) -> Unit) {
    options.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { (value, label) ->
                val chosen = selected == value
                Box(Modifier.weight(1f).heightIn(min = 54.dp)
                    .background(if (chosen) Color(0xFFE3DCF9) else Color.White, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(enabled = enabled, role = Role.RadioButton) { onSelect(value) }
                    .semantics { this.selected = chosen }.padding(horizontal = 8.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center) {
                    Text((if (chosen) "● " else "○ ") + label, style = jua(17, if (chosen) V2C.Title else V2C.Ink), textAlign = TextAlign.Center)
                }
            }
        }
    }
}
