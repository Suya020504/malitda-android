package kr.voicemate.malitda.ui.v2

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val meaningCategories = linkedMapOf("often" to "자주 쓰는 뜻", "people" to "사람", "places" to "장소",
    "time" to "시간", "daily" to "일상", "request" to "요청", "other" to "기타")

@Composable
fun MeaningCategoryField(selected: String, onSelect: (String) -> Unit, allowAll: Boolean = false) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        TextButton(onClick = { expanded = true }) {
            Text("분류: ${meaningCategories[selected] ?: "전체"} ▾", style = jua(18, V2C.Title))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            val options = if (allowAll) mapOf("" to "전체") + meaningCategories else meaningCategories
            options.forEach { (key, label) ->
                DropdownMenuItem(text = { Text(label, style = jua(18, V2C.Ink)) },
                    onClick = { expanded = false; onSelect(key) })
            }
        }
    }
}
