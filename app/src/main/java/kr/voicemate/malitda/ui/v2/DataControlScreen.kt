package kr.voicemate.malitda.ui.v2

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Distinct deletion scopes, with no implicit data export and no deletion on opening the screen. */
@Composable
fun DataControlScreen(
    counts: Map<String, Int>,
    onClearMeanings: () -> Unit,
    onResetRanking: () -> Unit,
    onClearTrials: () -> Unit,
    onWipeAll: () -> Unit,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    studentName: String = "",
    busy: Boolean = false,
    error: String? = null,
    onPrivacy: (() -> Unit)? = null,
) {
    var confirmation by remember { mutableStateOf<DataControlAction?>(null) }
    val student = studentName.ifBlank { "현재 학생" }
    val hasLegacyTrialRecords = (counts["trials"] ?: 0) > 0
    LaunchedEffect(hasLegacyTrialRecords) {
        if (!hasLegacyTrialRecords && confirmation == DataControlAction.TRIALS) confirmation = null
    }
    V2Screen {
        V2Header(onBack = { if (!busy) onBack() }, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("이 기기의 자료 관리")
            if (onPrivacy != null) V2TextButton("개인정보 처리·동의 안내", onClick = onPrivacy)
            V2SubTitle("${student}의 자료와 지울 범위를 확인합니다.")
            if (hasLegacyTrialRecords) V2SubTitle("이전 시험 기록을 지우면 이 기기의 공유용 임시 파일도 정리합니다. 다른 학생의 원본 기록은 남기며, 외부로 보낸 사본은 삭제할 수 없습니다.")
            DataSection("현재 학생의 기록") {
                Text("한 뜻에 평소 표현이 여러 개 연결될 수 있습니다.", style = jua(16, V2C.Muted))
                Spacer(Modifier.height(12.dp))
                CountRow("등록한 뜻 · 연습 예시 포함", counts["meanings"])
                CountRow("뜻에 연결된 평소 표현", counts["aliases"])
                CountRow("뜻 확인 반응 이력", counts["confirmations"])
                CountRow("이전 형식의 등록 문장", counts["expressions"])
                CountRow("개인 교정 이력", counts["corrections"])
                if (hasLegacyTrialRecords) CountRow("이전 시험 기록", counts["trials"])
                CountRow("등록한 뜻이 쓰는 사진·생성 그림", counts["photos"])
                Text("같은 사진을 여러 뜻이나 학생이 함께 쓰면 파일 수와 뜻 수는 다릅니다.", style = jua(15, V2C.Muted), modifier = Modifier.padding(top = 8.dp))
            }
            DataSection("어디에 보관하나") {
                Text("개인 뜻·표현·확인 이력과 보관 기록은 이 기기의 암호화된 데이터베이스에 저장합니다. 설정과 동의는 앱 전용 설정 저장공간에 둡니다.", style = jua(17, V2C.Ink, 1.5f))
                Spacer(Modifier.height(12.dp))
                Text("직접 고른 사진과 AI 그림은 앱 전용 사진 저장공간에 보관합니다. 다른 앱의 원본 사진은 바꾸지 않습니다. 사진 파일은 데이터베이스 암호화와 별개의 저장 방식이며, 앱을 지우면 함께 사라집니다.", style = jua(17, V2C.Ink, 1.5f))
                Spacer(Modifier.height(12.dp))
                Text("일반 말하기의 원음 녹음파일: 0개 · 저장하지 않음", style = jua(17, V2C.Ink))
                Text("음성인식 문장은 현재 확인 과정의 메모리에서만 사용합니다. 개발·시험을 위해 별도로 넣은 음원과 평가 파일이 있다면 기기 전체 자료 삭제에 포함합니다.", style = jua(15, V2C.Muted), modifier = Modifier.padding(top = 8.dp))
            }
            if (busy || error != null) Text(
                error ?: "자료를 정리하는 중입니다. 잠시 기다립니다.",
                style = jua(17, if (error != null) Color(0xFFA52D4C) else V2C.Ink),
                modifier = Modifier.padding(24.dp).semantics { liveRegion = LiveRegionMode.Polite },
            )
            DataSection("현재 학생의 뜻 모두 삭제") {
                Text("등록한 뜻·연습 예시·연결 표현·확인 반응·이전 등록 문장·교정 이력을 삭제합니다. 학생·설정과 다른 보관 기록은 남깁니다. 다른 학생이 사용하는 사진은 보존합니다.", style = jua(17, V2C.Ink, 1.5f))
            }
            DataActionButton("현재 학생의 뜻 삭제", !busy) { confirmation = DataControlAction.MEANINGS }
            DataSection("추천·교정만 초기화") {
                Text("현재 학생의 사용 횟수와 교정 이력을 초기화합니다. 등록한 뜻·평소 표현·그림·확인 반응과 다른 보관 기록은 남깁니다.", style = jua(17, V2C.Ink, 1.5f))
            }
            DataActionButton("현재 학생의 추천·교정 초기화", !busy) { confirmation = DataControlAction.RANKING }
            if (hasLegacyTrialRecords) {
                DataSection("이전 시험 기록 관리") {
                    Text("이전 버전이나 개발·시험 과정에서 남은 현재 학생의 결과만 정리합니다. 직접 등록한 개인 뜻과 학생·설정, 다른 학생의 원본 기록은 남깁니다. 현재 화면을 열었다고 새 시험 기록을 수집하지 않습니다.", style = jua(17, V2C.Ink, 1.5f))
                }
                DataActionButton("이전 시험 기록 삭제", !busy) { confirmation = DataControlAction.TRIALS }
            }
            DataSection("이 기기의 전체 자료 삭제") {
                Text("모든 학생·뜻·문장·사진·확인 이력·보관 기록·설정·PIN을 삭제하고 시작 안내로 돌아갑니다. 다른 앱에 공유한 문장·파일과 외부 서버 자료는 삭제되지 않습니다. 인식에 쓰는 앱 내장 모델은 남깁니다.", style = jua(17, V2C.Ink, 1.5f))
            }
            DataActionButton("기기 전체 자료 삭제", !busy, danger = true) { confirmation = DataControlAction.ALL }
            Spacer(Modifier.height(16.dp))
            DataActionButton("이전 화면으로", !busy, onClick = onBack)
            Spacer(Modifier.height(24.dp))
        }
    }
    confirmation?.takeIf { it != DataControlAction.TRIALS || hasLegacyTrialRecords }?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text(action.title, style = jua(23, V2C.Title)) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(action.description(student), style = jua(17, V2C.Ink, 1.5f))
                Spacer(Modifier.height(12.dp))
                Text("이 작업은 되돌릴 수 없습니다.", style = jua(17, Color(0xFFA52D4C)))
            } },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                confirmation = null
                when (action) {
                    DataControlAction.MEANINGS -> onClearMeanings()
                    DataControlAction.RANKING -> onResetRanking()
                    DataControlAction.TRIALS -> onClearTrials()
                    DataControlAction.ALL -> onWipeAll()
                }
            }) { Text(action.button, color = Color(0xFFA52D4C)) } },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text("돌아가기") } },
        )
    }
}

private enum class DataControlAction(val title: String, val button: String) {
    MEANINGS("현재 학생의 뜻을 모두 지울까요?", "뜻 모두 삭제"),
    RANKING("추천·교정만 초기화할까요?", "추천·교정 초기화"),
    TRIALS("이전 시험 기록을 지울까요?", "시험 기록 삭제"),
    ALL("이 기기의 전체 자료를 지울까요?", "전체 자료 삭제");

    fun description(student: String): String = when (this) {
        MEANINGS -> "${student}의 모든 뜻과 연습 예시, 연결 표현, 확인 반응, 이전 등록 문장과 교정 이력을 삭제합니다. 학생·설정과 다른 보관 기록은 남습니다."
        RANKING -> "${student}의 사용 횟수와 교정 이력을 초기화합니다. 직접 등록한 뜻·표현·그림과 다른 보관 기록은 지우지 않습니다."
        TRIALS -> "${student}의 보관된 시험 결과와 이 기기의 공유용 임시 파일만 정리합니다. 개인 뜻 사전·학생·설정과 다른 학생의 원본 기록은 남깁니다. 이미 외부로 보낸 사본은 삭제되지 않습니다."
        ALL -> "현재 학생을 포함한 모든 학생의 기기 내 자료와 설정, PIN을 삭제합니다. 개발 시험용 음원·평가 파일과 공유용 앱 캐시도 삭제합니다. 이미 다른 앱에 보낸 자료나 외부 서버 기록은 지워지지 않습니다."
    }
}

@Composable
private fun DataSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp)) {
        Text(title, style = jua(21, V2C.Title), modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun CountRow(label: String, count: Int?) {
    Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Text(label, style = jua(17, V2C.Ink))
        Text(count?.coerceAtLeast(0)?.let { "${it}개" } ?: "확인 중", style = jua(19, V2C.Title))
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = V2C.CardInner)
    }
}

@Composable
private fun DataActionButton(label: String, enabled: Boolean, danger: Boolean = false, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = V2C.Surface, contentColor = if (danger) Color(0xFFA52D4C) else V2C.Title),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).heightIn(min = 56.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
    ) { Text(label, style = jua(19, if (danger) Color(0xFFA52D4C) else V2C.Title)) }
}
