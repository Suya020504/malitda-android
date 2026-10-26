package kr.voicemate.malitda.ui.v2

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kr.voicemate.malitda.data.db.MeaningEntity
import kr.voicemate.malitda.domain.CommunicationSafety
import kr.voicemate.malitda.domain.VoiceCandidateOption
import kr.voicemate.malitda.domain.CorrectionLearningSnapshot

/** No default candidate is selected; the unmodified transcript remains available. */
@Composable
fun U16SpeechCandidates(raw: String, choices: List<VoiceCandidateOption>,
    context: String, onContext: (String) -> Unit, onRerank: () -> Unit,
    onMeaning: (MeaningEntity) -> Unit, onLiteral: (String) -> Unit, onListen: (String) -> Unit,
    onCards: () -> Unit, onRespeak: () -> Unit, onBack: () -> Unit, onHelp: () -> Unit) {
    V2Screen {
        V2Header(onBack = onBack, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("전할 뜻을 골라 주세요")
            V2SubTitle("아직 선택하거나 전달하지 않았습니다.")
            V2Card { Column(Modifier.padding(20.dp)) {
                Text("들은 말 · 원문", style = jua(15, V2C.Muted))
                Spacer(Modifier.height(8.dp)); Text(raw, style = jua(24, V2C.Ink))
            } }
            V2SecondaryButton("원문 그대로 확인", onClick = { onLiteral(raw) })
            V2TextButton("원문 듣기", onClick = { onListen(raw) })
            choices.forEach { choice ->
                if (choice.remembered) V2SubTitle("전에 확인하고 저장한 수정")
                val meaning = choice.meaning
                if (meaning != null) LibraryRow(meaning, chip = meaning.situation?.takeIf { it.isNotBlank() } ?: "등록한 개인 뜻") { onMeaning(meaning) }
                else V2SecondaryButton(choice.text, onClick = { onLiteral(choice.text) })
                V2TextButton("이 후보 듣기", onClick = { onListen(choice.text) })
            }
            if (choices.isEmpty()) V2Banner("추가 후보가 없습니다. 원문을 확인하거나 직접 고칠 수 있습니다.")
            V2TextButton("직접 고치기", onClick = { onLiteral(raw) })
            V2SecondaryButton("다른 카드에서 고르기", onClick = onCards)
            Spacer(Modifier.height(16.dp))
            V2SubTitle("상황이 다르면 같은 말도 다른 뜻일 수 있습니다.")
            V2TextField(context, onContext, "지금 상황 · 예: 점심, 쉬는 시간")
            V2TextButton("이 상황으로 후보 다시 찾기", onClick = onRerank)
            V2TextButton("다시 말하기", onClick = onRespeak)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun U17LiteralEdit(text: String, raw: String?, onText: (String) -> Unit,
    onListen: () -> Unit, onConfirm: () -> Unit, onCards: () -> Unit,
    onBack: () -> Unit, onHelp: () -> Unit,
    onListenText: (String) -> Unit = {}, onUnsure: (() -> Unit)? = null, onStopReading: () -> Unit = {}) {
    V2Screen {
        V2Header(onBack = onBack, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("전할 문장을 확인해 주세요")
            V2SubTitle("이번에 전할 문장입니다. 원하는 말인지 확인합니다.")
            raw?.takeIf { it.isNotBlank() }?.let { V2Banner("들은 말 · 원문\n$it") }
            Spacer(Modifier.height(14.dp))
            V2TextField(text, onText, "전하고 싶은 문장", error = text.trim().length > 200)
            V2SubTitle("${text.trim().length}/200자 · 원문을 임의로 줄이지 않습니다.")
            if (CommunicationSafety.sensitiveChange(raw, text)) {
                Spacer(Modifier.height(12.dp))
                V2Banner("숫자나 부정 표현이 달라졌습니다. 시간·수량·싫다는 뜻을 한 번 더 확인합니다.")
            }
            V2TextButton("소리로 확인", onClick = onListen)
            EasyTextOffer(text, onUse = onText, onListen = onListenText, onStop = onStopReading)
            if (onUnsure != null) V2TextButton("잘 모르겠어요", onClick = onUnsure)
            V2TextButton("잘 모르겠으면 카드로 고르기", onClick = onCards)
            Spacer(Modifier.height(16.dp))
            V2PrimaryButton("이 문장이 맞아요", enabled = text.isNotBlank() && text.trim().length <= 200, onClick = onConfirm)
            V2TextButton("그만하기", onClick = onBack)
        }
    }
}

@Composable
fun U18LiteralDeliver(text: String, onListen: () -> Unit, onStopSound: () -> Unit,
    onShare: () -> Unit, onCopy: () -> Unit, onSave: () -> Unit,
    onEdit: () -> Unit, onDone: () -> Unit, onHelp: () -> Unit, onFeedback: (() -> Unit)? = null,
    learning: CorrectionLearningSnapshot? = null, remembered: Boolean = false, remembering: Boolean = false,
    onRemember: (CorrectionLearningSnapshot) -> Unit = {}, onHistory: () -> Unit = {}) {
    var rememberDialog by remember(learning) { mutableStateOf(false) }
    V2Screen {
        V2Header(onBack = onEdit, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("이 문장으로 전합니다")
            V2Card { Text(text, style = jua(32, V2C.Ink), modifier = Modifier.padding(24.dp)) }
            V2PrimaryButton("소리로 들려주기", onClick = onListen)
            V2TextButton("소리 멈추기", onClick = onStopSound)
            V2SecondaryButton("공유할 앱 선택", onClick = onShare)
            V2TextButton("문장 복사", onClick = onCopy)
            V2TextButton("내 표현에 함께 등록", onClick = onSave)
            if (learning != null) {
                V2TextButton(if (remembering) "수정 저장 중…" else if (remembered) "이번 수정 저장됨" else "다음에도 이 수정 사용",
                    enabled = !remembered && !remembering) { rememberDialog = true }
                if (remembered) V2TextButton("저장한 수정 보기", onClick = onHistory)
            }
            V2TextButton("다시 고치기 · 승인 취소", onClick = onEdit)
            if (onFeedback != null) V2TextButton("실증 응답 기록 · 지원자", onClick = onFeedback)
            V2TextButton("마치고 홈으로", onClick = onDone)
            V2SubTitle("앱 선택은 실제 전송 완료를 뜻하지 않습니다. 취소해도 확인한 문장은 유지됩니다.")
            Spacer(Modifier.height(16.dp))
        }
    }
    if (rememberDialog && learning != null) AlertDialog(
        onDismissRequest = { rememberDialog = false },
        title = { Text("이 수정을 다음에도 쓸까요?", style = jua(22,V2C.Ink)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("들은 원문\n${learning.raw}\n\n확인한 문장\n${learning.approved}",style=jua(18,V2C.Ink))
            Spacer(Modifier.height(12.dp))
            Text("두 문장을 이 기기에 저장합니다. 같은 원문이 들어오면 후보로 보여 주며, 전달할 때는 다시 확인합니다. 같은 원문의 이전 기록은 이번 수정으로 바뀝니다. '내 표현 > 저장한 수정'에서 지울 수 있습니다.",style=jua(16,V2C.Muted))
        } },
        confirmButton = { TextButton(onClick = { rememberDialog=false;onRemember(learning) },enabled=!remembering) { Text("수정 저장",style=jua(18,V2C.Title)) } },
        dismissButton = { TextButton(onClick={rememberDialog=false}) { Text("이번만 사용",style=jua(18,V2C.Muted)) } },
    )
}

@Composable
fun V2MissingMeaning(onCards: () -> Unit, onHome: () -> Unit, onHelp: () -> Unit) {
    V2Screen {
        V2Header(onBack = onHome, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("선택한 뜻을 찾을 수 없습니다")
            V2Banner("삭제되었거나 다른 학생의 뜻입니다. 다시 선택해 주세요.")
            V2PrimaryButton("카드 다시 고르기", onClick = onCards)
            V2TextButton("홈으로", onClick = onHome)
        }
    }
}

@Composable
fun V2TaskEnded(onBack: () -> Unit, onHelp: () -> Unit) {
    V2Screen {
        V2Header(onBack = onBack, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("이 시도는 종료되었습니다")
            V2Banner("화면을 떠난 시도의 응답을 나중에 덧붙이지 않습니다. 새 시도에서 다시 기록합니다.")
            V2PrimaryButton("실증 설정으로", onClick = onBack)
        }
    }
}
