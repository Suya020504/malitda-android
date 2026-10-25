package kr.voicemate.malitda.ui.v2

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kr.voicemate.malitda.data.db.MeaningEntity
import kr.voicemate.malitda.domain.PilotConfig
import kr.voicemate.malitda.domain.PilotFeedback
import kr.voicemate.malitda.domain.PilotPlan

/** 지원자가 과업 전에 확인하는 화면. 개인정보를 입력할 칸은 제공하지 않는다. */
@Composable
fun PilotSetupScreen(
    config: PilotConfig,
    meanings: List<MeaningEntity>,
    onSave: (PilotConfig) -> Unit,
    onBack: () -> Unit,
    onHelp: () -> Unit,
) {
    var code by rememberSaveable(config.participantCode) { mutableStateOf(config.participantCode) }
    var task by rememberSaveable(config.taskCode) { mutableStateOf(config.taskCode) }
    var target by rememberSaveable(config.targetMeaningId) { mutableStateOf(config.targetMeaningId) }
    var condition by rememberSaveable(config.condition) { mutableStateOf(config.condition) }
    var practice by rememberSaveable(config.practice) { mutableStateOf(config.practice) }
    var suitability by rememberSaveable(config.suitabilityReviewed) { mutableStateOf(config.suitabilityReviewed) }
    var targets by remember(config.taskTargets) { mutableStateOf(config.taskTargets) }
    var consent by rememberSaveable(config.consented, config.consentVersion) {
        mutableStateOf(config.consented && config.consentVersion == PilotConfig.CONSENT_VERSION)
    }
    var environment by rememberSaveable(config.deviceContext) { mutableStateOf(config.deviceContext.substringBefore('|')) }
    var attempted by remember { mutableStateOf(false) }
    val validCode = PilotConfig.validParticipantCode(code)
    val validTask = PilotConfig.validTaskCode(task)
    val validTarget = practice || meanings.any { it.id == target && !it.isSample && it.confirmed }
    val valid = validCode && validTask && validTarget && consent && (practice || suitability)
    V2Screen {
        V2Header(onBack = onBack, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("실증 준비")
            V2SubTitle("실증 참여는 앱 사용과 별개입니다.")
            Spacer(Modifier.height(14.dp))
            V2Banner("참여하지 않거나 중간에 그만두어도 앱을 사용할 수 있습니다. 설명을 이해했는지 확인하고, 필요한 보호자 동의 절차를 먼저 진행합니다. 이 화면은 기관의 동의 절차를 대신하지 않습니다.")
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(value = code, onValueChange = {
                    val next = it.uppercase().take(6)
                    if (next != code) consent = false
                    code = next
                },
                    label = { Text("참여자 코드 (예: P001)") }, singleLine = true,
                    supportingText = { Text("실명·전화번호 대신 영문 1자와 숫자 3~5자를 사용합니다.") },
                    isError = attempted && !validCode, textStyle = jua(19, V2C.Ink), modifier = Modifier.fillMaxWidth())
                V2TextButton("새 익명 코드 제안") {
                    code = "P" + java.security.SecureRandom().nextInt(100000).toString().padStart(5, '0')
                    consent = false
                }
                Text("과업 세트 ${config.taskSetVersion}", style = jua(20, V2C.Ink))
                Text("각 과업에 참여자와 함께 정한 뜻을 하나씩 연결합니다. 목표가 바뀌면 세트 버전도 갱신합니다.", style = jua(16, V2C.Muted))
                listOf("T01" to "일상 요청 1", "T02" to "일상 요청 2", "T03" to "장소 말하기", "T04" to "감정 전하기", "T05" to "개인 고유 표현").forEach { (code, label) ->
                    ChoiceRow("$code · $label${if (targets.containsKey(code)) " · 목표 설정됨" else ""}", task == code) {
                        task = code; target = targets[code]
                    }
                }
                OutlinedTextField(value = task, onValueChange = {
                    val next = it.uppercase().take(4)
                    if (next != task) { task = next; target = targets[next] }
                },
                    label = { Text("과업 코드 (예: T01)") }, singleLine = true,
                    supportingText = { Text("T01~T999. 같은 비교에서는 같은 과업과 목표를 사용합니다.") },
                    isError = attempted && !validTask, textStyle = jua(19, V2C.Ink), modifier = Modifier.fillMaxWidth())
                ToggleRow("연습 시도 (익명 CSV에서 제외)", practice) { practice = it }
                ToggleRow("이번 참여자가 그림·글·소리 중 가능한 방법으로 뜻을 확인하거나 거절할 수 있고, 그만두겠다는 의사를 표현할 수 있는지 사전에 검토했습니다.", suitability) { suitability = it }
                Text("가능 여부가 불분명하면 치료사·지원자 검토와 연습부터 진행합니다. 앱이 장애유형이나 참여 적합성을 자동 판정하지 않습니다.", style = jua(16, V2C.Muted))
                Text("과업의 목표 뜻", style = jua(20, V2C.Ink))
                Text("지원자와 참여자가 사전에 정한 목표를 선택합니다. 결과를 본 뒤 정답을 바꾸지 않습니다.", style = jua(16, V2C.Muted))
                if (meanings.isEmpty()) Text("먼저 개인 표현을 등록하고 본인 확인을 마쳐야 실증할 수 있습니다.", style = jua(17, V2C.Ink))
                meanings.filter { practice || (!it.isSample && it.confirmed) }.forEach { meaning ->
                    ChoiceRow(meaning.text, target == meaning.id) {
                        target = meaning.id
                        if (PilotConfig.validTaskCode(task)) targets = targets + (task to meaning.id)
                    }
                }
                if (attempted && !validTarget) Text("실증의 목표 뜻을 선택해야 합니다.", style = jua(17, V2C.Title))
                if (attempted && !practice && !suitability) Text("본실증 전 확인·거절·중단 방법을 검토해야 합니다.", style = jua(17, V2C.Title))
                Text("비교 조건", style = jua(20, V2C.Ink))
                Text("추천 순서: ${PilotPlan.order(code).joinToString(" → ")}. 권장 순서는 참여자 번호에 따라 균형 있게 나눕니다. 조건은 지원자가 직접 확인합니다.", style = jua(16, V2C.Muted))
                ChoiceRow("A · 기본 음성 전사", condition == "A") { condition = "A" }
                ChoiceRow("B · 개인 표현의 후보 비교", condition == "B") { condition = "B" }
                ChoiceRow("C · 그림·문장·소리로 뜻 확인", condition == "C") { condition = "C" }
                Text("모든 조건에서 본인이 확인하고 승인한 뒤에만 전달합니다. 뜻을 추정해 자동으로 보내지 않습니다.", style = jua(16, V2C.Ink))
                Text("환경", style = jua(20, V2C.Ink))
                listOf("QUIET" to "조용한 곳", "NORMAL" to "일상 대화가 있는 곳", "NOISY" to "주변 소음이 있는 곳").forEach { (id, label) ->
                    ChoiceRow(label, environment == id) { environment = id }
                }
                Text("기기·Android·인식 엔진 정보는 시도 시점에 로컬 기록에 추가하며 익명 CSV에 포함하지 않습니다.", style = jua(15, V2C.Muted))
                ToggleRow("참여자에게 설명하고 자발적 참여 의사를 확인했습니다. 필요한 보호자 동의 절차도 확인했습니다.", consent) { consent = it }
                if (attempted && !consent) Text("참여 의사를 확인하지 않은 상태에서는 기록할 수 없습니다.", style = jua(17, V2C.Title))
                Text("기록 항목: 참여자·과업 코드, 조건, 시간, 선택·도움·중단 결과, 선택형 면담 응답. 수집하지 않는 항목: 실명·진단명·녹음·전사문·전달 문장. 철회하면 기기의 동의 설정과 기록을 삭제합니다. 내보낸 사본은 수신자에게 별도로 삭제를 요청해야 합니다.", style = jua(16, V2C.Ink))
                Text("중단 기준: 참여자의 요청, 피로, 안전 문제, 3회 연속 실패. 중단 후에는 실증 수집을 멈추고 일반 앱만 사용합니다.", style = jua(16, V2C.Ink))
                Text("기록 보관: 기관이 정한 보관 기간을 참여 전에 별도로 안내하고, 종료 후 설정에서 삭제합니다. 이 앱은 인터넷으로 실증 기록을 자동 전송하지 않습니다.", style = jua(16, V2C.Muted))
            }
            V2PrimaryButton("설정 저장") {
                attempted = true
                if (valid) onSave(config.copy(
                    participantCode = code, taskCode = task, targetMeaningId = target,
                    condition = condition, practice = practice, consented = true,
                    consentVersion = PilotConfig.CONSENT_VERSION,
                    consentedAt = if (config.consented && config.participantCode == code && config.consentVersion == PilotConfig.CONSENT_VERSION)
                        config.consentedAt.takeIf { it > 0 } ?: System.currentTimeMillis() else System.currentTimeMillis(),
                    deviceContext = environment, paused = false,
                    suitabilityReviewed = suitability, taskTargets = targets,
                    taskSetVersion = PilotPlan.nextTaskVersion(config, targets),
                ))
            }
            V2TextButton("저장하지 않고 돌아가기", onClick = onBack)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics(mergeDescendants = true) {}
        .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, onCheckedChange = null)
        Text(label, style = jua(17, V2C.Ink), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics(mergeDescendants = true) {}
        .selectable(selected, role = Role.RadioButton, onClick = onSelect), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = null)
        Text(label, style = jua(17, V2C.Ink), modifier = Modifier.weight(1f))
    }
}

/** 참여자의 선택형 면담 응답. 의견을 대신 추정하지 않고 무응답도 보존한다. */
@Composable
fun PilotFeedbackScreen(
    current: PilotFeedback = PilotFeedback(),
    understood: Int = -1,
    helped: Boolean = false,
    onUnderstood: (Boolean?) -> Unit = {},
    onHelped: (Boolean) -> Unit = {},
    onAccidentalSend: () -> Unit = {},
    onSave: (PilotFeedback) -> Unit,
    onBack: () -> Unit,
    onHelp: () -> Unit,
) {
    var values by remember(current) { mutableStateOf(listOf(current.control, current.fatigue, current.willingness, current.candidateEase, current.ttsEase)) }
    val prompts = listOf("내가 뜻을 고르고 그만둘 수 있었습니까?", "사용하는 동안 얼마나 피곤했습니까?", "다음에도 사용하고 싶습니까?", "뜻 후보를 비교하기 쉬웠습니까?", "소리를 듣고 뜻을 확인하기 쉬웠습니까?")
    V2Screen {
        V2Header(onBack = onBack, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("실증 후 짧은 질문")
            V2SubTitle("상대의 이해는 관찰 후 기록합니다. 응답 없음은 실패로 계산하지 않습니다.")
            V2SecondaryButton(if (understood == 1) "✓ 상대가 이해함" else "상대가 이해함") { onUnderstood(if (understood == 1) null else true) }
            V2SecondaryButton(if (understood == 0) "✓ 상대가 이해하지 못함" else "상대가 이해하지 못함") { onUnderstood(if (understood == 0) null else false) }
            V2TextButton("오전송 관찰 · 실증 즉시 중단", onClick = onAccidentalSend)
            V2TextButton(if (helped) "✓ 지원자가 도와줌" else "지원자가 도와줌") { onHelped(!helped) }

            V2Banner("참여자에게 질문하고 답변을 선택합니다. 지원자가 답을 대신 추정하지 않습니다. 응답하지 않은 항목은 0으로 남깁니다. 피로 항목은 점수가 높을수록 피로가 큰 뜻입니다.")
            prompts.forEachIndexed { index, prompt ->
                Column(Modifier.padding(horizontal = 24.dp, vertical = 10.dp)) {
                    Text(prompt, style = jua(19, V2C.Ink))
                    listOf(0 to "응답 없음", 1 to "1 · 전혀 그렇지 않음 / 피로 없음", 2 to "2 · 조금", 3 to "3 · 보통", 4 to "4 · 많이", 5 to "5 · 매우 많이").forEach { (n, label) ->
                        ChoiceRow(label, values[index] == n) { values = values.toMutableList().also { it[index] = n } }
                    }
                }
            }
            V2PrimaryButton("응답 기록") { onSave(PilotFeedback(values[0], values[1], values[2], values[3], values[4])) }
            V2TextButton("기록하지 않고 돌아가기", onClick = onBack)
            Spacer(Modifier.height(16.dp))
        }
    }
}
