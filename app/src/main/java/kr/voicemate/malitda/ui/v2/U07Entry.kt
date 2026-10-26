package kr.voicemate.malitda.ui.v2

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kr.voicemate.malitda.R

/** U07 · 학생 확인(화면 19) — 지원자 흐름의 입구: 누구의 표현을 등록할지 확인. */
@Composable
fun U07Entry(
    studentName: String,
    onRegister: () -> Unit,
    onMyExpressions: () -> Unit,
    onStudentChip: () -> Unit,
    onBack: () -> Unit,
    onHelp: () -> Unit,
) {
    V2Screen {
        V2Header(onBack = onBack, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("누구의 표현인가요?")
            V2SubTitle("학생과 함께 뜻을 준비해요.")
            Spacer(Modifier.height(16.dp))
            V2Card {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(Color(0xFFF3F0FB), Color(0xFFF0F6F4))))
                        .padding(vertical = 34.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painterResource(R.drawable.v2_char_robot), contentDescription = null,
                        modifier = Modifier.size(150.dp).clip(RoundedCornerShape(22.dp)),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(Modifier.height(22.dp))
                    Box(
                        Modifier
                            .background(Color(0xFFE2F4E8), RoundedCornerShape(18.dp))
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onStudentChip() }
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                    ) { Text("${studentName.ifBlank { "친구" }}의 표현", style = jua(18, Color(0xFF2F8F5D))) }
                }
            }
            Spacer(Modifier.height(16.dp))
            V2Banner(
                "${iGa(studentName.ifBlank { "친구" })} 전하려는 뜻을 먼저 확인해요.\n지원자가 혼자 뜻을 확정하지 않아요.",
                green = true, icon = Icons.Rounded.Check,
            )
            Spacer(Modifier.height(16.dp))
        }
        V2PrimaryButton("${studentName.ifBlank { "친구" }}의 표현 등록", icon = "⊕") { onRegister() }
        Spacer(Modifier.height(12.dp))
        V2SecondaryButton("내 표현 보기") { onMyExpressions() }
        V2TextButton("홈으로", onClick = onBack)
        Spacer(Modifier.height(16.dp))
    }
}

/** U07-S · 사용할 학생을 확인해요(화면 36) — 전환·추가 포함. */
@Composable
fun U07NewSession(
    studentName: String,
    others: List<Pair<Long, String>>,
    onRestart: () -> Unit,
    onSwitch: (Long) -> Unit,
    onAdd: (String) -> Unit,
    onCancel: () -> Unit,
    onHelp: () -> Unit,
    onRename: (String) -> Unit = {},
) {
    val name = studentName.ifBlank { "친구" }
    var adding by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var newName by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    var renaming by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var renameText by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    V2Screen {
        V2Header(onBack = onCancel, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("사용할 학생을 확인해요")
            Spacer(Modifier.height(12.dp))
            V2Card {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(Color(0xFFF5EFFA), Color(0xFFF0F4FB), Color(0xFFF0FAF3))))
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painterResource(R.drawable.v2_char_robot), contentDescription = null,
                        modifier = Modifier.size(150.dp).background(V2C.CardInner, RoundedCornerShape(24.dp)).padding(10.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(Modifier.height(22.dp))
                    Row(
                        Modifier
                            .background(Color(0xFFE2F4E8), RoundedCornerShape(26.dp))
                            .padding(horizontal = 34.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Check, null, tint = Color(0xFF2F8F5D), modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(name, style = jua(24, Color(0xFF2F8F5D)))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            // 지금 학생 이름 바꾸기(사전은 그대로)
            if (renaming) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    V2TextField(value = renameText, onValueChange = { renameText = it }, placeholder = "학생 이름")
                }
                Spacer(Modifier.height(10.dp))
                V2SecondaryButton("이 이름으로 바꾸기") {
                    if (renameText.isNotBlank()) { onRename(renameText.trim()); renaming = false }
                }
            } else {
                V2TextButton("${name} 이름 바꾸기") { renameText = if (name == "친구") "" else name; renaming = true }
            }
            Spacer(Modifier.height(8.dp))
            V2Banner("학생을 바꾸면,\n말하던 내용과 선택한 뜻은 지워져요.", icon = Icons.Rounded.Refresh)
            // 다른 학생 전환
            if (others.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("다른 학생으로 바꾸기", style = jua(17, V2C.Ink), modifier = Modifier.padding(horizontal = 24.dp))
                others.forEach { (id, otherName) ->
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .background(V2C.Surface, RoundedCornerShape(22.dp))
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { onSwitch(id) }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(otherName, style = jua(19, V2C.Title), modifier = Modifier.weight(1f))
                        Text("전환 →", style = jua(16, V2C.Muted))
                    }
                }
            }
            // 새 학생 추가
            Spacer(Modifier.height(16.dp))
            if (adding) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    V2TextField(value = newName, onValueChange = { newName = it }, placeholder = "새 학생 이름")
                }
                Spacer(Modifier.height(10.dp))
                V2SecondaryButton("이 이름으로 추가하고 전환") {
                    if (newName.isNotBlank()) { onAdd(newName.trim()); adding = false; newName = "" }
                }
            } else {
                V2SecondaryButton("⊕  새 학생 추가") { adding = true }
            }
            Spacer(Modifier.height(16.dp))
        }
        V2PrimaryButton("${roJosa(name)} 새로 시작", icon = "⟳") { onRestart() }
        Spacer(Modifier.height(12.dp))
        V2SecondaryButton("취소") { onCancel() }
        Spacer(Modifier.height(16.dp))
    }
}
