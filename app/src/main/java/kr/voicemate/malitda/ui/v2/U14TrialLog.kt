package kr.voicemate.malitda.ui.v2

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kr.voicemate.malitda.domain.PilotConfig
import kr.voicemate.malitda.domain.TrialSummary

/** 현재 선택한 학생의 로컬 실증 설정·기록. CSV 실행 전 실제 반출 범위를 다시 확인한다. */
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
    config: PilotConfig = PilotConfig(),
    exportCount: Int = 0,
    onConfigure: () -> Unit = {},
    onWithdraw: () -> Unit = {},
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onPauseReason: (kr.voicemate.malitda.domain.PilotStopReason) -> Unit = { onPause() },
) {
    var choosePauseReason by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmWithdraw by remember { mutableStateOf(false) }
    var confirmExport by remember { mutableStateOf(false) }
    V2Screen {
        V2Header(onBack = onBack, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("실증 기록")
            V2SubTitle("${studentName.ifBlank { "현재 학생" }}의 기록만 관리합니다.")
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (enabled) "시도 기록 켜짐" else "시도 기록 꺼짐", style = jua(19, V2C.Ink), modifier = Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
            V2Banner("참여자 코드·조건·과업·걸린 시간·선택 결과와 도움 여부를 기기에 기록합니다. 실명, 녹음, 전사문, 전달 문장은 기록하지 않습니다.")
            Spacer(Modifier.height(14.dp))
            V2SecondaryButton("참여자·동의·과업 설정", onClick = onConfigure)
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Text("참여자 코드: ${config.participantCode.ifBlank { "미설정" }}", style = jua(18, V2C.Ink))
                Text("현재 조건: ${config.condition} · 과업 ${config.taskCode} · ${if (config.practice) "연습" else "실증"}", style = jua(16, V2C.Muted))
                Text(if (config.canCollect) "동의 확인됨 · 기록 가능" else "동의·과업 설정을 확인해야 기록할 수 있습니다.", style = jua(16, V2C.Ink))
                if (config.paused) Text("실증이 중단된 상태입니다. 일반 앱 사용은 계속할 수 있습니다.", style = jua(17, V2C.Title))
            }
            Spacer(Modifier.height(14.dp))
            V2Card {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Text("현재 학생의 로컬 기록", style = jua(20, V2C.Ink))
                    StatRow("전체 시도", "${summary.total}회")
                    StatRow("중단 / 연습", "${summary.aborted} / ${summary.practice}회")
                    Text("아래 지표는 동의한 실증 완료 시도만 집계합니다.", style = jua(15, V2C.Muted))
                    StatRow("완료 시도", "${summary.completed}회")
                    StatRow("뜻을 확인해 전달", ratio(summary.delivered, summary.completed))
                    StatRow("상대가 이해함", ratio(summary.understoodYes, summary.understoodAnswered))
                    StatRow("말로 첫 후보 선택", ratio(summary.voiceFirstHit, summary.voiceTrials))
                    StatRow("말하다 카드로 전환", "${summary.cardFallback}회")
                    StatRow("지원자 도움", "${summary.supporterHelped}회")
                }
            }
            Spacer(Modifier.height(16.dp))
            V2PrimaryButton("익명 CSV 확인", enabled = config.hasConsent && exportCount > 0) { confirmExport = true }
            if (exportCount == 0) Text("내보낼 동의·완료 기록이 없습니다. 연습·중단 기록은 제외됩니다.", style = jua(15, V2C.Muted), modifier = Modifier.padding(24.dp))
            V2TextButton(if (config.paused) "설정 확인 후 실증 다시 시작" else "실증 잠시 중단") {
                if (config.paused) onResume() else choosePauseReason = true
            }
            V2TextButton("참여 철회 및 이 학생 기록 삭제") { confirmWithdraw = true }
            V2TextButton("이 학생 실증 기록만 삭제") { confirmClear = true }
            Spacer(Modifier.height(16.dp))
        }
    }
    if (confirmExport) AlertDialog(
        onDismissRequest = { confirmExport = false }, title = { Text("공유할 범위 확인") },
        text = { Text("현재 학생의 참여자 코드 ${config.participantCode}, 동의한 실증 완료 ${exportCount}건을 CSV 파일로 공유합니다. 참여자·과업 코드, 비교조건, 수치만 포함합니다. 이름·문장·절대시각·면담 내용과 다른 학생 기록은 포함하지 않습니다.\n\n공유한 사본은 수신자에게 별도로 삭제를 요청해야 합니다.") },
        confirmButton = { TextButton(onClick = { confirmExport = false; onExport() }) { Text("CSV 파일 공유") } },
        dismissButton = { TextButton(onClick = { confirmExport = false }) { Text("취소") } },
    )
    if (choosePauseReason) AlertDialog(onDismissRequest = { choosePauseReason = false },
        title = { Text("실증 중단 사유") },
        text = { Column {
            listOf("참여자 요청" to kr.voicemate.malitda.domain.PilotStopReason.PARTICIPANT_REQUEST,
                "피로" to kr.voicemate.malitda.domain.PilotStopReason.FATIGUE,
                "안전 문제" to kr.voicemate.malitda.domain.PilotStopReason.SAFETY).forEach { (label, reason) ->
                TextButton(onClick = { choosePauseReason = false; onPauseReason(reason) }) { Text(label) }
            }
        } }, confirmButton = {}, dismissButton = { TextButton(onClick = { choosePauseReason = false }) { Text("돌아가기") } })

    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false }, title = { Text("실증 기록 삭제") },
        text = { Text("현재 학생의 실증 기록을 모두 삭제합니다. 등록한 표현과 다른 학생의 기록은 유지됩니다.") },
        confirmButton = { TextButton(onClick = { confirmClear = false; onClear() }) { Text("기록 삭제") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("취소") } },
    )
    if (confirmWithdraw) AlertDialog(
        onDismissRequest = { confirmWithdraw = false }, title = { Text("참여 철회") },
        text = { Text("실증 기록을 끄고 현재 학생의 동의 설정과 실증 기록을 삭제합니다. 일반 앱은 계속 사용할 수 있습니다. 이미 공유한 사본은 자동으로 삭제되지 않습니다.") },
        confirmButton = { TextButton(onClick = { confirmWithdraw = false; onWithdraw() }) { Text("철회하고 삭제") } },
        dismissButton = { TextButton(onClick = { confirmWithdraw = false }) { Text("취소") } },
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, style = jua(16, V2C.Muted))
        Text(value, style = jua(18, V2C.Ink))
    }
}
private fun ratio(n: Int, d: Int): String = if (d == 0) "응답 없음" else "$n/$d (${n * 100 / d}%)"
