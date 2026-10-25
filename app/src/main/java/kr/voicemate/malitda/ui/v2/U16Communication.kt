package kr.voicemate.malitda.ui.v2

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kr.voicemate.malitda.data.db.MeaningEntity
import kr.voicemate.malitda.domain.CommunicationSafety

/** No default candidate is selected; the unmodified transcript remains available. */
@Composable
fun U16SpeechCandidates(raw: String, meanings: List<MeaningEntity>, alternatives: List<String>,
    candidateCount: Int, context: String, onContext: (String) -> Unit, onRerank: () -> Unit,
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
            meanings.take(candidateCount).forEach { m ->
                LibraryRow(m, chip = m.situation?.takeIf { it.isNotBlank() } ?: "등록한 개인 뜻") { onMeaning(m) }
                V2TextButton("이 후보 듣기", onClick = { onListen(m.text) })
            }
            alternatives.distinct().filter { it != raw && meanings.none { m -> m.text == it } }
                .take((candidateCount - meanings.size).coerceAtLeast(0)).forEach { text ->
                    V2SecondaryButton(text, onClick = { onLiteral(text) })
                    V2TextButton("이 문장 듣기", onClick = { onListen(text) })
                }
            if (meanings.isEmpty()) V2Banner("연결된 개인 뜻이 없습니다. 원문을 확인하거나 직접 고칠 수 있습니다.")
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
    onBack: () -> Unit, onHelp: () -> Unit) {
    V2Screen {
        V2Header(onBack = onBack, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("전할 문장을 확인해 주세요")
            V2SubTitle("직접 쓴 문장입니다. 원하는 말인지 확인합니다.")
            raw?.takeIf { it.isNotBlank() }?.let { V2Banner("들은 말 · 원문\n$it") }
            Spacer(Modifier.height(14.dp))
            V2TextField(text, onText, "전하고 싶은 문장", error = text.trim().length > 200)
            V2SubTitle("${text.trim().length}/200자 · 원문을 임의로 줄이지 않습니다.")
            if (CommunicationSafety.sensitiveChange(raw, text)) {
                Spacer(Modifier.height(12.dp))
                V2Banner("숫자나 부정 표현이 달라졌습니다. 시간·수량·싫다는 뜻을 한 번 더 확인합니다.")
            }
            V2TextButton("소리로 확인", onClick = onListen)
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
    onEdit: () -> Unit, onDone: () -> Unit, onHelp: () -> Unit, onFeedback: (() -> Unit)? = null) {
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
            V2TextButton("다시 고치기 · 승인 취소", onClick = onEdit)
            if (onFeedback != null) V2TextButton("실증 응답 기록 · 지원자", onClick = onFeedback)
            V2TextButton("마치고 홈으로", onClick = onDone)
            V2SubTitle("앱 선택은 실제 전송 완료를 뜻하지 않습니다. 취소해도 확인한 문장은 유지됩니다.")
            Spacer(Modifier.height(16.dp))
        }
    }
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
