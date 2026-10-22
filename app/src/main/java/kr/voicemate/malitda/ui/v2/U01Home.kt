package kr.voicemate.malitda.ui.v2

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kr.voicemate.malitda.R
import kr.voicemate.malitda.data.db.MeaningEntity

/** U01 · 학생 홈(화면 01) / U01-E · 등록 없는 홈(화면 13). */
@Composable
fun U01Home(
    name: String,
    meanings: List<MeaningEntity>,
    onSpeak: () -> Unit,
    onCard: (MeaningEntity) -> Unit,
    onRegister: () -> Unit,
    onGuide: () -> Unit,
    onHelp: () -> Unit,
    onLibrary: () -> Unit,
    onSettings: () -> Unit,
) {
    V2Screen {
        V2Header(onHelp = onHelp, logoStart = true)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (meanings.isEmpty()) EmptyHome(onRegister, onGuide) else FilledHome(name, meanings, onSpeak, onCard, onRegister)
        }
        V2BottomNav(onLibrary = onLibrary, onSettings = onSettings)
    }
}

@Composable
private fun FilledHome(
    name: String,
    meanings: List<MeaningEntity>,
    onSpeak: () -> Unit,
    onCard: (MeaningEntity) -> Unit,
    onRegister: () -> Unit,
) {
    Spacer(Modifier.height(4.dp))
    // 인사 카드
    V2Card(radius = 26) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Color(0xFFF6EFFB), Color(0xFFEFF2FC))))
                .padding(horizontal = 22.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${vocative(name)}, 무엇을\n전하고 싶어?",
                style = jua(26, V2C.Title, 1.35f),
                modifier = Modifier.weight(1f),
            )
            Image(
                painterResource(R.drawable.v2_char_robot), contentDescription = null,
                modifier = Modifier.size(92.dp).clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Fit,
            )
        }
    }
    Spacer(Modifier.height(18.dp))
    // 말하기 버튼(홈 전용으로 조금 더 크게)
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(72.dp)
            .shadow(8.dp, RoundedCornerShape(36.dp), clip = false, spotColor = V2C.Shadow)
            .background(V2Gradient, RoundedCornerShape(36.dp))
            .clip(RoundedCornerShape(36.dp))
            .clickable { onSpeak() }
            .semantics { contentDescription = "말하기" },
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_mic_white), null, Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Text("말하기", style = jua(26, Color.White))
        }
    }
    Spacer(Modifier.height(24.dp))
    Text(
        "카드로 골라도 돼요",
        style = jua(21, V2C.Ink),
        modifier = Modifier.padding(horizontal = 24.dp),
    )
    Spacer(Modifier.height(12.dp))
    // 의미 카드 2열 격자
    meanings.chunked(2).forEach { rowItems ->
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            rowItems.forEach { m -> MeaningCard(m, Modifier.weight(1f)) { onCard(m) } }
            if (rowItems.size == 1) Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
    }
    Spacer(Modifier.height(6.dp))
    V2SecondaryButton("⊕  지원자와 함께 등록") { onRegister() }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun MeaningCard(m: MeaningEntity, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .shadow(6.dp, RoundedCornerShape(24.dp), clip = false, spotColor = V2C.Shadow)
            .background(V2C.Surface, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(10.dp)
            .semantics { contentDescription = m.text },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val painter = meaningPainter(m.imageKey)
        if (painter != null) {
            Image(
                painter, contentDescription = null,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.22f).clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                Modifier.fillMaxWidth().aspectRatio(1.22f).background(V2C.CardInner, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) { CardGlyph(size = 34) }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            m.text, style = jua(18, V2C.Ink), textAlign = TextAlign.Center, maxLines = 2,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
}

/** U01-E · 아직 확인한 뜻이 없는 홈. */
@Composable
private fun EmptyHome(onRegister: () -> Unit, onGuide: () -> Unit) {
    Spacer(Modifier.height(4.dp))
    V2Title("전하고 싶은 뜻을\n함께 준비해요")
    Spacer(Modifier.height(8.dp))
    V2Card {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFFFBF2F8), Color(0xFFF0F3FC))))
                .padding(24.dp),
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .shadow(3.dp, CircleShape, clip = false, spotColor = V2C.Shadow)
                    .background(V2C.Surface, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.AddCircle, null, tint = V2C.BannerIcon, modifier = Modifier.size(26.dp)) }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(28.dp))
                Image(
                    painterResource(R.drawable.v2_char_robot), contentDescription = null,
                    modifier = Modifier
                        .size(180.dp)
                        .background(V2C.CardInner, RoundedCornerShape(28.dp))
                        .padding(14.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.height(28.dp))
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    V2Banner("아직 함께 확인한 뜻이 없어요.\n자주 쓰는 표현부터 등록해 주세요.", leading = { CardGlyph() })
    Spacer(Modifier.height(18.dp))
    V2PrimaryButton("뜻 함께 등록하기", icon = "⊕") { onRegister() }
    Spacer(Modifier.height(12.dp))
    V2SecondaryButton("처음 화면 안내") { onGuide() }
    Spacer(Modifier.height(16.dp))
}
