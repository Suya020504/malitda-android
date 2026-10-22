package kr.voicemate.malitda.ui.v2

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** U05 · 표현과 뜻 등록(화면 20) / U05-E · 입력 오류(화면 21). */
@Composable
fun U05Register(
    expression: String,
    meaning: String,
    imageKey: String?,
    situation: String,
    showError: Boolean,
    conflictMessage: String?,
    onExpression: (String) -> Unit,
    onMeaning: (String) -> Unit,
    onSituation: (String) -> Unit,
    onPickImage: () -> Unit,
    onConfirmWithStudent: () -> Unit,
    onSaveUnconfirmed: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onHelp: () -> Unit,
) {
    val expressionError = showError && expression.isBlank()
    val meaningError = showError && meaning.isBlank()
    V2Screen {
        V2Header(onBack = onCancel, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("표현과 뜻을 등록해요")
            Spacer(Modifier.height(8.dp))
            if (conflictMessage != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .background(Color(0xFFFDF1F3), RoundedCornerShape(20.dp))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                ) { Text(conflictMessage, style = jua(16, Color(0xFFD95D7B), 1.5f)) }
                Spacer(Modifier.height(12.dp))
            }
            V2Card(radius = 28) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp)) {
                    Text("평소 쓰는 표현", style = jua(17, V2C.Ink))
                    Spacer(Modifier.height(8.dp))
                    V2TextField(
                        value = expression, onValueChange = onExpression,
                        placeholder = if (expressionError) "표현을 입력해 주세요" else "예: 바람, 산책 가자",
                        error = expressionError,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("쉼표(,)로 여러 표현을 함께 등록할 수 있어요.", style = jua(13, V2C.Muted))
                    Spacer(Modifier.height(16.dp))
                    Text("전할 뜻", style = jua(17, V2C.Ink))
                    Spacer(Modifier.height(8.dp))
                    V2TextField(
                        value = meaning, onValueChange = onMeaning,
                        placeholder = if (meaningError) "뜻을 입력해 주세요" else "예: 산책하고 싶어요",
                        error = meaningError,
                    )
                    Spacer(Modifier.height(18.dp))
                    // 의미 그림 선택 줄
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(V2C.BannerBg, RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onPickImage() }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val painter = meaningPainter(imageKey)
                        if (painter != null) {
                            Image(
                                painter, contentDescription = null,
                                modifier = Modifier.size(width = 104.dp, height = 70.dp).clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Box(
                                Modifier.size(width = 104.dp, height = 70.dp).background(V2C.Surface, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center,
                            ) { CardGlyph(size = 26) }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("의미 그림", style = jua(14, V2C.Muted))
                            Spacer(Modifier.height(4.dp))
                            Text(if (imageKey == null) "그림 선택하기" else "그림 바꾸기", style = jua(19, V2C.Title))
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("사용 상황 · 선택", style = jua(17, V2C.Ink))
                    Spacer(Modifier.height(8.dp))
                    V2TextField(value = situation, onValueChange = onSituation, placeholder = "예: 쉬는 시간, 점심 후")
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "이 뜻이 맞는지 학생과 함께 확인해요.",
                        style = jua(15, V2C.Muted), textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        if (showError && (expression.isBlank() || meaning.isBlank())) {
            V2PrimaryButton("표현 다시 입력하기") { onRetry() }
            Spacer(Modifier.height(12.dp))
            V2SecondaryButton("취소") { onCancel() }
        } else {
            V2PrimaryButton("학생과 뜻 확인하기", icon = "✓") { onConfirmWithStudent() }
            Spacer(Modifier.height(12.dp))
            V2SecondaryButton("미확인으로 저장") { onSaveUnconfirmed() }
        }
        Spacer(Modifier.height(16.dp))
    }
}

private val PICKER_KEYS = listOf("walk", "water", "rest", "cold", null)

/** U05-M · 뜻을 보여줄 그림(화면 22) — 내장 그림 중 선택 후 확정. */
@Composable
fun U05PickImage(
    meaningText: String,
    selectedKey: String?,
    onSelect: (String?) -> Unit,
    onUse: () -> Unit,
    onCancel: () -> Unit,
    onHelp: () -> Unit,
) {
    V2Screen {
        V2Header(onBack = onCancel, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("뜻을 보여줄 그림")
            V2SubTitle(meaningText.ifBlank { "전할 뜻을 함께 보여줘요" })
            Spacer(Modifier.height(14.dp))
            // 큰 미리보기 카드
            V2Card {
                Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val painter = meaningPainter(selectedKey)
                    if (painter != null) {
                        Image(
                            painter, contentDescription = null,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1.5f).clip(RoundedCornerShape(22.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(1.5f).background(V2C.CardInner, RoundedCornerShape(22.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CardGlyph(size = 40)
                                Spacer(Modifier.height(12.dp))
                                Text("그림 없이 문장으로 보여줘요", style = jua(16, V2C.Muted))
                            }
                        }
                    }
                    if (meaningText.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Text(meaningText, style = jua(24, V2C.Ink), textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            // 내장 그림 선택 줄
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PICKER_KEYS.forEach { key ->
                    val selected = key == selectedKey
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(V2C.Surface, RoundedCornerShape(16.dp))
                            .border(
                                if (selected) 3.dp else 1.5.dp,
                                if (selected) V2C.Title else Color(0xFFE4DEF2),
                                RoundedCornerShape(16.dp),
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelect(key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        val p = meaningPainter(key)
                        if (p != null) Image(p, null, Modifier.fillMaxWidth().aspectRatio(1f), contentScale = ContentScale.Crop)
                        else CardGlyph(size = 24)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            V2Banner("이 그림으로 함께 확인해요.", green = true, icon = Icons.Rounded.Check)
            Spacer(Modifier.height(16.dp))
        }
        V2PrimaryButton("이 그림 사용", icon = "✓") { onUse() }
        V2TextButton("취소") { onCancel() }
        Spacer(Modifier.height(8.dp))
    }
}
