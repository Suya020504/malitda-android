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
import kr.voicemate.malitda.BuildConfig

/** Supporter entry: prepare a meaning, review pending meanings, or manage the current student. */
@Composable
fun U07Entry(
    studentName: String,
    onRegister: () -> Unit,
    onMyExpressions: () -> Unit,
    onStudentChip: () -> Unit,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    onArchive: () -> Unit,
    onSettings: () -> Unit,
    onTrialLog: (() -> Unit)? = null,
    onData: (() -> Unit)? = null,
    onRemoveSamples: (() -> Unit)? = null,
    pendingCount: Int? = null,
) {
    var confirmSamples by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    V2Screen {
        V2Header(onHelp = onHelp, logoStart = true)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("함께 관리")
            V2SubTitle("보호자·교사와 표현을 함께 준비합니다.")
            Spacer(Modifier.height(16.dp))
            V2Card(radius = 26) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("현재 학생", style = jua(15, V2C.Muted))
                        Spacer(Modifier.height(6.dp))
                        Text(studentName.ifBlank { "친구" }, style = jua(24, V2C.Ink))
                        Spacer(Modifier.height(6.dp))
                        Text("이 학생의 표현을 함께 준비합니다.", style = jua(15, V2C.Muted))
                    }
                    Spacer(Modifier.width(12.dp))
                    Image(painterResource(R.drawable.v2_char_robot), contentDescription = null,
                        modifier = Modifier.size(84.dp), contentScale = ContentScale.Fit)
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("표현 준비", style = jua(20, V2C.Title), modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(10.dp))
            V2PrimaryButton("새 표현 등록", onClick = onRegister)
            Spacer(Modifier.height(12.dp))
            V2SecondaryButton("미확인·사용 중지 뜻" + (pendingCount?.let { " · ${it}개" } ?: ""), onClick = onArchive)
            Text("아직 확인하지 않았거나 사용을 쉬고 있는 뜻을 다시 살핍니다.", style = jua(15, V2C.Muted), modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            V2SecondaryButton("학생 변경·추가", onClick = onStudentChip)
            Spacer(Modifier.height(18.dp))
            V2Banner("학생이 그림·글·소리로 뜻을 확인한 뒤 사용합니다. 지원자의 추측만으로 확정하지 않습니다.", green = true, icon = Icons.Rounded.Check)
            if ((BuildConfig.PILOT_TOOLS_ENABLED && onTrialLog != null) || onData != null || onRemoveSamples != null) {
                Text(if (BuildConfig.PILOT_TOOLS_ENABLED) "기록과 자료" else "개인 자료", style = jua(20, V2C.Title), modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp))
                if (BuildConfig.PILOT_TOOLS_ENABLED && onTrialLog != null) V2TextButton("실증 준비·기록", onClick = onTrialLog)
                if (onData != null) V2TextButton("개인 자료 확인·삭제", onClick = onData)
                if (onRemoveSamples != null) V2TextButton("연습 예시 정리", onClick = { confirmSamples = true })
            }
            Spacer(Modifier.height(20.dp))
        }
        V2TextButton("관리 마치고 홈으로", onClick = onBack)
        V2BottomNav(selected = "register", onHome = onBack, onLibrary = onMyExpressions, onSettings = onSettings, onRegister = {})
    }
    if (confirmSamples) androidx.compose.material3.AlertDialog(
        onDismissRequest = { confirmSamples = false },
        title = { Text("연습 예시를 정리할까요?", style = jua(22, V2C.Title)) },
        text = { Text("지울 수 있는 연습 예시만 정리합니다. 직접 등록한 개인 뜻은 지우지 않습니다.", style = jua(17, V2C.Ink)) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { confirmSamples = false; onRemoveSamples?.invoke() }) { Text("예시 정리") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmSamples = false }) { Text("돌아가기") } },
    )
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
