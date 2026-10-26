package kr.voicemate.malitda.ui.v2

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kr.voicemate.malitda.data.db.CorrectionEntity

@Composable
fun U19SavedCorrections(profileId: Long, items: List<CorrectionEntity>, busy: Boolean,
    onUse: (CorrectionEntity) -> Unit, onDelete: (CorrectionEntity) -> Unit,
    onBack: () -> Unit, onHome: () -> Unit, onHelp: () -> Unit) {
    val own = items.filter { it.profileId == profileId }
    var pending by remember(profileId) { mutableStateOf<CorrectionEntity?>(null) }
    V2Screen {
        V2Header(onBack = onBack, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("저장한 수정")
            V2SubTitle("직접 저장한 원문과 확인 문장입니다. 다음에도 후보로만 제시하며, 고르기 전에는 전달하지 않습니다.")
            Spacer(Modifier.height(16.dp))
            if (own.isEmpty()) V2Banner("저장한 수정이 없습니다. 말한 문장을 고쳐 확인한 뒤, 다음에도 쓸 수정을 따로 저장할 수 있습니다.")
            for (item in own) {
                V2Card(radius = 24) {
                    Column(Modifier.padding(18.dp)) {
                        Text("전에 들은 말", style = jua(14, V2C.Muted))
                        Text(item.sourceRaw, style = jua(18, V2C.Ink))
                        Spacer(Modifier.height(12.dp))
                        Text("내가 확인한 문장", style = jua(14, V2C.Title))
                        Text(item.approvedText, style = jua(22, V2C.Ink))
                    }
                    V2SecondaryButton("이 문장 확인하기", modifier = Modifier.semantics { contentDescription = "${item.approvedText} 다시 확인" }, enabled = !busy) { onUse(item) }
                    V2TextButton("이 수정 기록 삭제", modifier = Modifier.semantics { contentDescription = "${item.approvedText} 수정 기록 삭제" }, enabled = !busy) { pending = item }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
        V2TextButton("내 표현으로", enabled = !busy, onClick = onBack)
        V2TextButton("홈으로", enabled = !busy, onClick = onHome)
    }
    pending?.takeIf { row -> own.any { it.id == row.id && it == row } }?.let { row ->
        AlertDialog(onDismissRequest = { pending = null },
            title = { Text("이 수정 기록을 지울까요?", style = jua(22, V2C.Ink)) },
            text = { Text("${row.approvedText}\n\n다음 후보에 이 기록을 사용하지 않습니다. 따로 등록한 개인 뜻과 다른 사용자의 기록은 남습니다.", style = jua(17, V2C.Ink)) },
            confirmButton = { TextButton(onClick = { pending = null; onDelete(row) }, enabled = !busy) { Text("기록 삭제", style = jua(18,V2C.Title)) } },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("유지", style = jua(18,V2C.Muted)) } })
    }
}
