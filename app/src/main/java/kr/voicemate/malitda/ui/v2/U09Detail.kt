package kr.voicemate.malitda.ui.v2

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kr.voicemate.malitda.data.db.MeaningEntity

/** U09 · 등록한 표현 상세(화면 26) / U09-I · 잠시 사용하지 않아요(화면 30). active 여부로 모습이 갈린다. */
@Composable
fun U09Detail(
    meaning: MeaningEntity,
    expression: String,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit,
    onReconfirm: () -> Unit,
    onDelete: () -> Unit,
    onToLibrary: () -> Unit,
    onBack: () -> Unit,
    onHelp: () -> Unit,
) {
    V2Screen {
        V2Header(onBack = onBack, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (meaning.active) {
                V2Title("등록한 표현")
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .background(V2C.BannerBg, RoundedCornerShape(22.dp))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CardGlyph(size = 20)
                    Spacer(Modifier.width(12.dp))
                    Text("$expression  →  ${meaning.text}", style = jua(17, V2C.Title))
                }
            } else {
                V2Title("잠시 사용하지 않아요")
            }
            Spacer(Modifier.height(12.dp))
            MeaningBigCard(meaning)
            Spacer(Modifier.height(12.dp))
            // 상태 칩
            Box(Modifier.padding(horizontal = 20.dp)) {
                if (meaning.active && meaning.confirmed) {
                    Box(Modifier.background(Color(0xFFE2F4E8), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 7.dp)) {
                        Text("함께 확인함", style = jua(14, V2C.GreenText))
                    }
                } else if (!meaning.active) {
                    Box(Modifier.background(Color(0xFFE9E4F4), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 7.dp)) {
                        Text("사용 중지", style = jua(14, Color(0xFF6B6485)))
                    }
                } else {
                    Box(Modifier.background(V2C.BannerBg, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 7.dp)) {
                        Text("미확인", style = jua(14, V2C.Title))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (meaning.active) {
                if (meaning.situation != null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .background(V2C.BannerBg, RoundedCornerShape(18.dp))
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                    ) { Text("사용 상황: ${meaning.situation}", style = jua(16, V2C.Ink)) }
                    Spacer(Modifier.height(12.dp))
                }
            } else {
                V2Banner("학생의 후보에 나오지 않아요.\n다시 쓰려면 뜻을 함께 확인해 주세요.", icon = Icons.Rounded.Info)
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(8.dp))
        }
        if (meaning.active) {
            V2PrimaryButton("표현과 뜻 수정") { onEdit() }
            Spacer(Modifier.height(12.dp))
            V2SecondaryButton("잠깐 사용하지 않기") { onDeactivate() }
            V2TextButton("이 뜻 삭제") { onDelete() }
        } else {
            V2PrimaryButton("다시 함께 확인하기", icon = "✓") { onReconfirm() }
            Spacer(Modifier.height(12.dp))
            V2SecondaryButton("내 표현으로") { onToLibrary() }
            V2TextButton("이 뜻 삭제") { onDelete() }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** U09-D · 이 뜻을 삭제할까요?(화면 29). */
@Composable
fun U09Delete(
    meaning: MeaningEntity,
    onConfirmDelete: () -> Unit,
    onKeep: () -> Unit,
    onHelp: () -> Unit,
) {
    V2Screen {
        V2Header(onBack = onKeep, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("이 뜻을 삭제할까요?")
            Spacer(Modifier.height(12.dp))
            V2Card {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(Color(0xFFFBEFF2), Color(0xFFF4EFF8))))
                        .padding(vertical = 44.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(92.dp).background(V2C.Surface, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Delete, null, tint = Color(0xFFE0485F), modifier = Modifier.size(40.dp)) }
                    Spacer(Modifier.height(40.dp))
                    Text(meaning.text, style = jua(28, V2C.Ink), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 20.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            V2Banner("이 뜻과 연결한 표현이 함께 지워져요.\n되돌릴 수 없어요.", icon = Icons.Rounded.Info)
            Spacer(Modifier.height(16.dp))
        }
        V2DangerButton("삭제하기") { onConfirmDelete() }
        Spacer(Modifier.height(12.dp))
        V2SecondaryButton("계속 보관하기") { onKeep() }
        Spacer(Modifier.height(16.dp))
    }
}
