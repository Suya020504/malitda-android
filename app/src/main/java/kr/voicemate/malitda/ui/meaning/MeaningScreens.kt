package kr.voicemate.malitda.ui.meaning

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date
import kr.voicemate.malitda.R
import kr.voicemate.malitda.data.db.MeaningWithAliases
import kr.voicemate.malitda.data.db.ProfileEntity

private fun MeaningWithAliases.isConfirmed() = meaning.status == "CONFIRMED"

private fun MeaningWithAliases.aliasText() = aliases.map { it.rawAlias }.filter { it.isNotBlank() }.joinToString(" · ")

private fun statusLabel(status: String): String = when (status) {
    "CONFIRMED" -> "함께 확인함"
    "INACTIVE" -> "잠시 사용하지 않음"
    "DRAFT" -> "아직 미확인"
    else -> "상태 확인 필요"
}

@Composable
fun MeaningInfoScreen(
    title: String,
    message: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    MeaningPage(title, onBack = onPrimary) {
        MeaningNotice(message)
        MeaningAction(primaryLabel, onPrimary)
        if (secondaryLabel != null && onSecondary != null) MeaningAction(secondaryLabel, onSecondary, secondary = true)
    }
}

@Composable
fun MeaningOptionsDialog(title: String, options: List<Pair<String, () -> Unit>>, onDismiss: () -> Unit) {
    MeaningTheme {
        AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { (label, action) ->
                        TextButton(action, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(label, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }, confirmButton = { TextButton(onDismiss, Modifier.heightIn(min = 48.dp)) { Text("닫기") } })
    }
}

@Composable
fun MeaningInfoDialog(title: String, message: String, onDismiss: () -> Unit) {
    MeaningTheme {
        AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
            text = { Text(message, Modifier.verticalScroll(rememberScrollState()), style = MaterialTheme.typography.bodyMedium) },
            confirmButton = { TextButton(onDismiss, Modifier.heightIn(min = 48.dp)) { Text("닫기") } })
    }
}

@Composable
fun MeaningProfileDialog(profiles: List<ProfileEntity>, name: String, busy: Boolean,
    onNameChange: (String) -> Unit, onSelect: (Long) -> Unit, onAdd: () -> Unit, onDismiss: () -> Unit) {
    MeaningTheme {
        AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("사용할 학생 선택") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("학생을 바꾸면 이전 대화와 선택은 정리돼요.", style = MaterialTheme.typography.bodyMedium)
                    profiles.forEach { student ->
                        TextButton({ onSelect(student.id) }, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !busy) { Text(student.name) }
                    }
                    OutlinedTextField(name, onNameChange, enabled = !busy, label = { Text("새 학생 이름") }, singleLine = true)
                }
            }, confirmButton = { TextButton(onAdd, Modifier.heightIn(min = 48.dp), enabled = name.isNotBlank() && !busy) { Text("학생 추가") } },
            dismissButton = { TextButton(onDismiss, Modifier.heightIn(min = 48.dp), enabled = !busy) { Text("닫기") } })
    }
}

@Composable
private fun MeaningStatus(status: String) {
    val active = status == "CONFIRMED"
    Surface(shape = RoundedCornerShape(16.dp),
        color = if (active) MeaningColors.Mint else MeaningColors.Lavender) {
        Text(statusLabel(status), Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (active) MeaningColors.MintInk else MeaningColors.Muted)
    }
}

@Composable
private fun MeaningLargeCard(item: MeaningWithAliases) {
    Surface(shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, MeaningColors.Line),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MeaningPicture(item.meaning.imageRef, Modifier.fillMaxWidth().aspectRatio(1.5f))
            Text(item.meaning.displayText, style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
        }
    }
}

@Composable
private fun MeaningListCard(item: MeaningWithAliases, onClick: () -> Unit, showStatus: Boolean = false) {
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MeaningColors.Line), modifier = Modifier.fillMaxWidth().heightIn(min = 112.dp)) {
        BoxWithConstraints(Modifier.padding(12.dp)) {
            val stacked = maxWidth < 260.dp || LocalDensity.current.fontScale > 1.35f
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MeaningPicture(item.meaning.imageRef, Modifier.fillMaxWidth().aspectRatio(1.8f))
                    MeaningListText(item, showStatus)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    MeaningPicture(item.meaning.imageRef, Modifier.size(88.dp), compact = true)
                    Column(Modifier.weight(1f)) { MeaningListText(item, showStatus) }
                }
            }
        }
    }
}

@Composable
private fun MeaningListText(item: MeaningWithAliases, showStatus: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(item.meaning.displayText, style = MaterialTheme.typography.titleMedium)
        if (item.meaning.contextLabel?.isNotBlank() == true) {
            Text(item.meaning.contextLabel.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MeaningColors.Muted)
        }
        if (showStatus) MeaningStatus(item.meaning.status)
    }
}

@Composable
private fun MeaningTileRow(group: List<MeaningWithAliases>, columns: Int, onChoose: (Long) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        group.forEach { item ->
            Surface(onClick = { onChoose(item.meaning.id) },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MeaningColors.Line)) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MeaningPicture(item.meaning.imageRef, Modifier.fillMaxWidth().aspectRatio(1.45f))
                    Text(item.meaning.displayText, style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                }
            }
        }
        if (group.size < columns) Spacer(Modifier.weight(1f))
    }
}

@Composable
fun MeaningHomeScreen(
    name: String,
    meanings: List<MeaningWithAliases>,
    onMic: () -> Unit,
    onChoose: (Long) -> Unit,
    onLibrary: () -> Unit,
    onAdd: () -> Unit,
    onLegacy: () -> Unit,
    onSettings: () -> Unit,
) {
    val usable = meanings.filter { it.isConfirmed() }
    MeaningTheme {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val contentWidth = minOf(maxWidth, 640.dp) - 40.dp
            val fontScale = LocalDensity.current.fontScale
            val columns = if (contentWidth < 320.dp || fontScale > 1.25f) 1 else 2
            MeaningLazyPage(headerEnd = {
                IconButton(onSettings, Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Settings, contentDescription = "설정 열기", tint = MeaningColors.Muted)
                }
            }) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val greeting = if (name.isBlank() || name == "친구") "무엇을 전하고 싶나요?" else "${name.trim()} 님,\n무엇을 전하고 싶나요?"
                        Text(greeting, style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.weight(1f).semantics { heading() })
                        if (contentWidth >= 300.dp && fontScale <= 1.3f) {
                            Image(painterResource(R.drawable.char_robot_mint), null, modifier = Modifier.size(88.dp))
                        }
                    }
                }
                item { MeaningAction("말하기", onMic, icon = Icons.Rounded.Mic) }
                item {
                    Text("카드로 골라도 돼요", style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.semantics { heading() })
                }
                if (usable.isEmpty()) {
                    item { MeaningNotice("아직 확인한 뜻이 없어요.\n지원자와 등록한 뒤 학생과 확인해요.", mint = true) }
                } else {
                    items(usable.chunked(columns), key = { "home-row-${it.first().meaning.id}-$columns" }) {
                        MeaningTileRow(it, columns, onChoose)
                    }
                }
                item { MeaningAction("지원자와 함께 등록", onAdd, secondary = true, icon = Icons.Rounded.Add) }
                item { MeaningAction("내 표현 보기", onLibrary, secondary = true) }
                item { MeaningAction("음성 입력 도구 열기", onLegacy, quiet = true) }
            }
        }
    }
}

@Composable
fun MeaningCandidatesScreen(
    raw: String,
    items: List<MeaningWithAliases>,
    onSelect: (Long) -> Unit,
    onRerecord: () -> Unit,
    onCards: () -> Unit,
    onRegister: () -> Unit,
    onCancel: () -> Unit,
) {
    val usable = items.filter { it.isConfirmed() }
    MeaningLazyPage("어떤 뜻인가요?", onBack = onCancel) {
        if (raw.isNotBlank()) item { MeaningNotice("입력한 표현: $raw") }
        item { Text("그림과 문장을 보고 골라 주세요.", style = MaterialTheme.typography.bodyLarge) }
        if (usable.isEmpty()) {
            item { MeaningNotice("이 표현으로 함께 확인한 뜻을 찾지 못했어요. 카드로 고르거나 지원자와 등록해 주세요.") }
        } else {
            items(usable, key = { it.meaning.id }) { item -> MeaningListCard(item, { onSelect(item.meaning.id) }) }
        }
        item { MeaningAction("다시 말하기", onRerecord, icon = Icons.Rounded.Mic) }
        item { MeaningAction("카드로 고르기", onCards, secondary = true) }
        item { MeaningAction("여기에 없어요 · 함께 등록", onRegister, secondary = true) }
        item { MeaningAction("취소", onCancel, quiet = true) }
    }
}

@Composable
fun MeaningConfirmScreen(
    item: MeaningWithAliases,
    inputSource: String,
    onYes: () -> Unit,
    onOther: () -> Unit,
    onListen: () -> Unit,
    onBack: () -> Unit,
) {
    MeaningPage("이 뜻이 맞나요?", onBack = onBack) {
        if (inputSource.isNotBlank()) MeaningNotice(inputSource)
        MeaningLargeCard(item)
        if (item.isConfirmed()) {
            Text("함께 등록한 뜻이에요. 지금 전하려는 뜻인지 확인해 주세요.",
                style = MaterialTheme.typography.bodyMedium, color = MeaningColors.Muted)
        } else {
            MeaningNotice("이 뜻은 지금 사용할 수 없어요. 내 표현에서 학생과 다시 확인해 주세요.", error = true)
        }
        MeaningAction("소리로 확인하기", onListen, secondary = true,
            enabled = item.isConfirmed(), icon = Icons.AutoMirrored.Rounded.VolumeUp)
        Text("그림과 문장으로 확인해도 돼요.", style = MaterialTheme.typography.bodyMedium, color = MeaningColors.Muted)
        MeaningAction("맞아요", onYes, enabled = item.isConfirmed(), icon = Icons.Rounded.Check)
        MeaningAction("다른 뜻이에요", onOther, secondary = true)
    }
}

@Composable
fun MeaningPresentScreen(
    item: MeaningWithAliases,
    approved: Boolean,
    ttsError: Boolean,
    onSpeak: () -> Unit,
    onShare: () -> Unit,
    onChoose: () -> Unit,
    onHome: () -> Unit,
) {
    val canDeliver = approved && item.isConfirmed()
    MeaningPage(if (canDeliver) "이렇게 전달해요" else "뜻을 다시 확인해 주세요", onBack = onChoose) {
        MeaningLargeCard(item)
        MeaningNotice(if (canDeliver) "내가 지금 확인한 뜻이에요." else "현재 전하려는 뜻을 확인하면 소리로 들려주거나 보낼 수 있어요.", mint = canDeliver)
        if (ttsError) {
            MeaningNotice("소리가 나오지 않았어요. 그림과 문장을 보여주거나 다시 들려주세요.", error = true)
        }
        MeaningAction(if (ttsError) "소리 다시 들려주기" else "소리로 들려주기", onSpeak,
            enabled = canDeliver, icon = Icons.AutoMirrored.Rounded.VolumeUp)
        MeaningAction("다시 고르기", onChoose, secondary = true)
        MeaningAction("다른 앱으로 보내기", onShare, secondary = true,
            enabled = canDeliver, icon = Icons.Rounded.Share)
        MeaningAction("홈으로", onHome, quiet = true)
    }
}

@Composable
fun MeaningLibraryScreen(
    items: List<MeaningWithAliases>,
    onDetail: (Long) -> Unit,
    onAdd: () -> Unit,
    onHome: () -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf("CONFIRMED") }
    val filters = listOf("CONFIRMED" to "함께 확인함", "DRAFT" to "미확인", "INACTIVE" to "사용 안 함", "ALL" to "전체")
    val shown = if (filter == "ALL") items else items.filter { it.meaning.status == filter }
    MeaningLazyPage("내 표현", onBack = onHome) {
        item {
            Text("함께 확인한 뜻을 관리해요. 사용할 때는 지금 전하려는 뜻을 다시 확인해요.",
                style = MaterialTheme.typography.bodyMedium, color = MeaningColors.Muted)
        }
        item {
          Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            filters.forEach { (key, label) ->
                Surface(shape = RoundedCornerShape(16.dp),
                    color = if (filter == key) MeaningColors.Lavender else Color.White,
                    border = BorderStroke(1.dp, if (filter == key) MeaningColors.Purple else MeaningColors.Line)) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .selectable(filter == key, role = Role.RadioButton, onClick = { filter = key })
                        .padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = filter == key, onClick = null)
                        val count = if (key == "ALL") items.size else items.count { it.meaning.status == key }
                        Text("$label · $count", style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f).padding(start = 8.dp))
                    }
                }
            }
          }
        }
        if (filter == "DRAFT") item { MeaningNotice("미확인인 뜻은 학생의 카드와 후보에 나타나지 않아요.") }
        if (filter == "INACTIVE") item { MeaningNotice("사용을 멈춘 뜻이에요. 다시 사용하려면 학생과 확인해 주세요.") }
        if (shown.isEmpty()) {
            item { MeaningNotice(if (items.isEmpty()) "등록한 표현이 없어요. 지원자와 함께 첫 표현을 준비해 주세요." else "이 상태의 표현이 없어요. 다른 상태를 선택하면 등록한 표현을 볼 수 있어요.") }
        } else {
            this.items(shown, key = { it.meaning.id }) { item -> MeaningListCard(item, { onDetail(item.meaning.id) }, showStatus = true) }
        }
        item { MeaningAction("뜻 함께 등록하기", onAdd, icon = Icons.Rounded.Add) }
        item { MeaningAction("홈으로", onHome, quiet = true) }
    }
}

@Composable
fun MeaningEditScreen(
    aliases: String,
    displayText: String,
    contextLabel: String,
    imageRef: String?,
    error: String?,
    busy: Boolean,
    onAliasesChange: (String) -> Unit,
    onTextChange: (String) -> Unit,
    onContextChange: (String) -> Unit,
    onPickBuiltin: (String) -> Unit,
    onPickPhoto: () -> Unit,
    onReview: () -> Unit,
    onDraft: () -> Unit,
    onCancel: () -> Unit,
) {
    val canContinue = aliases.isNotBlank() && displayText.isNotBlank() && !busy
    MeaningPage("표현과 뜻을 등록해요", onBack = if (busy) null else onCancel) {
        MeaningNotice("학생이 전하려는 뜻을 함께 준비해요. 수정한 뜻도 학생과 다시 확인해 주세요.", mint = true)
        OutlinedTextField(value = aliases, onValueChange = onAliasesChange,
            modifier = Modifier.fillMaxWidth(), enabled = !busy,
            label = { Text("평소 쓰는 표현") }, minLines = 2,
            supportingText = { Text("여러 표현은 한 줄에 하나씩 적어 주세요.") })
        OutlinedTextField(value = displayText, onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(), enabled = !busy,
            label = { Text("전할 뜻") }, minLines = 2,
            supportingText = { Text("학생이 전하려는 뜻을 문장으로 적어 주세요.") })
        Text("함께 볼 그림 · 선택", style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.semantics { heading() })
        MeaningPicture(imageRef, Modifier.fillMaxWidth().aspectRatio(1.7f))
        Text("뜻과 맞는 그림을 골라 주세요. 그림 없이도 문장으로 확인할 수 있어요.",
            style = MaterialTheme.typography.bodyMedium, color = MeaningColors.Muted)
        val pictures = listOf("builtin:walk" to "산책 그림", "builtin:water" to "물 그림", "builtin:rest" to "휴식 그림", "builtin:cold" to "추위 그림")
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            pictures.forEach { (ref, label) ->
                MeaningPictureChoice(label, ref == imageRef, !busy) { onPickBuiltin(ref) }
            }
            MeaningPictureChoice("그림 없이 사용", imageRef.isNullOrBlank(), !busy) { onPickBuiltin("") }
        }
        MeaningAction("내 기기의 사진 고르기", onPickPhoto, enabled = !busy, secondary = true)
        OutlinedTextField(value = contextLabel, onValueChange = onContextChange,
            modifier = Modifier.fillMaxWidth(), enabled = !busy,
            label = { Text("사용 상황 · 선택") }, minLines = 2)
        if (!error.isNullOrBlank()) MeaningNotice(error, error = true)
        if (busy) MeaningBusy()
        MeaningAction("학생과 뜻 확인하기", onReview, enabled = canContinue)
        MeaningAction("미확인으로 저장", onDraft, enabled = canContinue, secondary = true)
        Text("미확인으로 저장한 뜻은 함께 확인할 때까지 카드와 후보에 나오지 않아요.",
            style = MaterialTheme.typography.bodyMedium, color = MeaningColors.Muted)
        MeaningAction("취소", onCancel, enabled = !busy, quiet = true)
    }
}

@Composable
private fun MeaningPictureChoice(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = if (selected) MeaningColors.Lavender else Color.White,
        border = BorderStroke(1.dp, if (selected) MeaningColors.Purple else MeaningColors.Line)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected, onClick = null, enabled = enabled)
            Text(label, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).padding(start = 8.dp))
        }
    }
}

@Composable
private fun MeaningBusy() {
    Row(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        Text("저장하고 있어요. 잠시 기다려 주세요.", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun MeaningReviewScreen(
    item: MeaningWithAliases,
    checked: Boolean,
    confirmationMethod: String,
    confirmedBy: String,
    onCheckedChange: (Boolean) -> Unit,
    onMethodChange: (String) -> Unit,
    onSupporterChange: (String) -> Unit,
    busy: Boolean,
    error: String?,
    onConfirm: () -> Unit,
    onDraft: () -> Unit,
    onListen: () -> Unit,
    onBack: () -> Unit,
) {
    val methods = listOf("그림·문장으로 함께 확인", "그림을 고름", "말로 답함", "합의한 몸짓으로 답함")
    val customMethod = confirmationMethod.takeUnless { it in methods }.orEmpty()
    MeaningPage("학생과 뜻을 확인해요", onBack = if (busy) null else onBack) {
        MeaningNotice("평소 쓰는 표현: ${item.aliasText().ifBlank { "입력한 표현 없음" }}")
        MeaningLargeCard(item)
        MeaningAction("소리로 확인하기", onListen, enabled = !busy, secondary = true,
            icon = Icons.AutoMirrored.Rounded.VolumeUp)
        Text("그림·문장과 학생이 평소 쓰는 확인 방법을 이용해요. 학생의 표정만 보고 뜻을 확정하지 않아요.",
            style = MaterialTheme.typography.bodyMedium, color = MeaningColors.Muted)
        Text("학생은 어떤 방법으로 답했나요?", style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.semantics { heading() })
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            methods.forEach { method ->
                MeaningPictureChoice(method, confirmationMethod == method, !busy) { onMethodChange(method) }
            }
        }
        OutlinedTextField(value = customMethod, onValueChange = onMethodChange,
            modifier = Modifier.fillMaxWidth(), enabled = !busy,
            label = { Text("다른 확인 방법 직접 입력") }, minLines = 2,
            supportingText = { Text("다른 방법을 적으면 위 선택을 대신해 기록돼요.") })
        OutlinedTextField(value = confirmedBy, onValueChange = onSupporterChange,
            modifier = Modifier.fillMaxWidth(), enabled = !busy,
            label = { Text("함께 확인한 지원자 · 선택") }, minLines = 1,
            supportingText = { Text("이 기록은 이 기기에서 표현과 함께 보관돼요.") })
        Surface(shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, MeaningColors.Line)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp)
                .toggleable(value = checked, enabled = !busy, role = Role.Checkbox, onValueChange = onCheckedChange)
                .padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Checkbox(checked = checked, onCheckedChange = null, enabled = !busy)
                Text("학생이 전하려는 뜻을 함께 확인했어요", style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f))
            }
        }
        if (!error.isNullOrBlank()) MeaningNotice(error, error = true)
        if (busy) MeaningBusy()
        MeaningAction("이 뜻 사용하기", onConfirm,
            enabled = checked && confirmationMethod.isNotBlank() && !busy && item.meaning.status == "DRAFT")
        if (!checked || confirmationMethod.isBlank()) {
            Text("학생의 확인 방법을 기록하고 함께 확인했는지 선택해 주세요.",
                style = MaterialTheme.typography.bodyMedium, color = MeaningColors.Muted)
        }
        MeaningAction("나중에 확인하기 · 미확인 저장", onDraft, secondary = true, enabled = !busy)
        MeaningAction("입력으로 돌아가기", onBack, quiet = true, enabled = !busy)
    }
}

@Composable
fun MeaningDetailScreen(
    item: MeaningWithAliases,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit,
    onReactivate: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    var dialog by rememberSaveable(item.meaning.id, item.meaning.version) { mutableStateOf<String?>(null) }
    MeaningPage("등록한 표현", onBack = onBack) {
        MeaningNotice("${item.aliasText().ifBlank { "표현 없음" }} → ${item.meaning.displayText}")
        MeaningLargeCard(item)
        MeaningStatus(item.meaning.status)
        item.meaning.contextLabel?.takeIf { it.isNotBlank() }?.let {
            Text("사용 상황: $it", style = MaterialTheme.typography.bodyMedium, color = MeaningColors.Muted)
        }
        item.meaning.confirmationMethod?.takeIf { it.isNotBlank() }?.let {
            Text("함께 확인한 방법: $it", style = MaterialTheme.typography.bodyMedium, color = MeaningColors.Muted)
        }
        item.meaning.confirmedBy?.takeIf { it.isNotBlank() }?.let {
            Text("함께 확인한 지원자: $it", style = MaterialTheme.typography.bodyMedium, color = MeaningColors.Muted)
        }
        item.meaning.confirmedAt?.let {
            Text("마지막 확인: ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it))}",
                style = MaterialTheme.typography.bodySmall, color = MeaningColors.Muted)
        }
        when (item.meaning.status) {
            "DRAFT" -> MeaningNotice("아직 학생과 확인하지 않은 뜻이에요. 확인할 때까지 카드와 후보에 나타나지 않아요.")
            "INACTIVE" -> MeaningNotice("잠시 사용을 멈춘 뜻이에요. 다시 사용하려면 학생과 확인해 주세요.")
            "CONFIRMED" -> Text("표현·뜻·그림을 수정하면 학생과 다시 확인해요.",
                style = MaterialTheme.typography.bodyMedium, color = MeaningColors.Muted)
        }
        MeaningAction(if (item.meaning.status == "DRAFT") "표현 확인·수정하기" else "표현과 뜻 수정", onEdit)
        if (item.isConfirmed()) {
            MeaningAction("잠시 사용하지 않기", { dialog = "deactivate" }, secondary = true)
        } else if (item.meaning.status == "INACTIVE") {
            MeaningAction("학생과 확인하고 다시 사용", { dialog = "reactivate" }, secondary = true)
        }
        MeaningAction("이 뜻 삭제", { dialog = "delete" }, quiet = true, danger = true)
        MeaningAction("내 표현으로", onBack, quiet = true)
        if (dialog != null) {
            val deleting = dialog == "delete"
            val reactivating = dialog == "reactivate"
            val title = when { deleting -> "이 뜻을 삭제할까요?"; reactivating -> "학생과 다시 확인할까요?"; else -> "잠시 사용하지 않을까요?" }
            val message = when {
                deleting -> "이 뜻과 연결된 표현, 함께 확인한 기록이 이 기기에서 삭제돼요. 삭제한 뜻은 되돌릴 수 없어요."
                reactivating -> "이 뜻을 미확인 상태로 바꾸고 학생과 확인하는 화면으로 이동해요. 확인한 뒤 카드와 후보에 다시 나타나요."
                else -> "카드와 후보에서 이 뜻을 숨겨요. 내 표현에는 보관되며 학생과 다시 확인하고 사용할 수 있어요."
            }
            AlertDialog(onDismissRequest = { dialog = null }, title = { Text(title) },
                text = { Text(message, modifier = Modifier.verticalScroll(rememberScrollState())) },
                confirmButton = {
                    TextButton(onClick = {
                        val action = dialog
                        dialog = null
                        when (action) { "delete" -> onDelete(); "reactivate" -> onReactivate(); "deactivate" -> onDeactivate() }
                    }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(when { deleting -> "삭제하기"; reactivating -> "학생과 다시 확인"; else -> "사용 멈추기" },
                            color = if (deleting) MeaningColors.Danger else MeaningColors.Purple)
                    }
                }, dismissButton = {
                    TextButton(onClick = { dialog = null }, modifier = Modifier.heightIn(min = 48.dp)) { Text("취소") }
                })
        }
        Spacer(Modifier.height(8.dp))
    }
}
