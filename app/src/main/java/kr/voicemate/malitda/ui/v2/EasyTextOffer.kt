package kr.voicemate.malitda.ui.v2

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kr.voicemate.malitda.domain.EasyText

/** Compare first, then edit. Applying a proposal never approves or changes a stored personal meaning. */
@Composable
fun EasyTextOffer(original: String, onUse: (String) -> Unit, onListen: (String) -> Unit,
    enabled: Boolean = true, onStop: () -> Unit = {}) {
    var expanded by remember(original) { mutableStateOf(false) }
    val suggestion = remember(original) { EasyText.suggest(original) }
    V2TextButton(if (expanded) "쉬운 말 제안 닫기" else "쉬운 말 제안 보기", enabled = enabled && original.isNotBlank()) { if (expanded) onStop(); expanded = !expanded }
    if (expanded) {
        if (suggestion == null) {
            V2Banner("바꿀 수 있는 표현을 찾지 못했습니다. 원문을 유지하거나 직접 고칠 수 있습니다.")
        } else {
            V2Card(radius = 24) {
                Column(Modifier.padding(16.dp)) {
                    Text("원래 문장", style = jua(14, V2C.Muted))
                    Text(suggestion.original, style = jua(18, V2C.Ink))
                    Spacer(Modifier.height(14.dp))
                    Text("쉬운 말 제안", style = jua(14, V2C.Title))
                    Text(suggestion.text, style = jua(22, V2C.Ink))
                    Spacer(Modifier.height(10.dp))
                    Text("같은 뜻인지 확인합니다. 잘 모르겠으면 원문을 그대로 둡니다.", style = jua(15, V2C.Muted))
                    V2TextButton("제안 문장 듣기", enabled = enabled) { onListen(suggestion.text) }
                    V2TextButton("소리 멈추기", enabled = enabled, onClick = onStop)
                    V2SecondaryButton("이 문장으로 고치기", enabled = enabled) { onStop(); onUse(suggestion.text); expanded = false }
                    V2TextButton("원문 유지", enabled = enabled) { onStop(); expanded = false }
                }
            }
        }
    }
}
