package kr.voicemate.malitda.ui.v2

import android.graphics.BitmapFactory
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kr.voicemate.malitda.domain.ArtStyles

/**
 * 설정 › 그림체(디자인 42화면 밖의 추가 화면). 학생별로 '그림 만들기'에 쓸 그림체를 고른다.
 * 팀원 R4 서버의 10종과 같다. 고르는 것은 선호일 뿐 뜻 확인·전달 승인이 아니다.
 * 기본 2열, 큰 글씨(1.2배 이상)에서는 1열로 아래로 내려 본다.
 */
@Composable
fun U15ArtStyle(
    studentName: String,
    selected: String,
    previews: Map<String, ByteArray>,
    serverNote: String?,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
    onHelp: () -> Unit,
) {
    val columns = if (LocalDensity.current.fontScale >= 1.15f) 1 else 2
    V2Screen {
        V2Header(onBack = onBack, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("어떤 그림이 편한가요?")
            V2SubTitle("${studentName.ifBlank { "친구" }}에게 보여 줄 그림체를 골라요.\n아래로 내려서 더 볼 수 있어요.")
            Spacer(Modifier.height(10.dp))
            Text(
                "지금 고른 그림체: ${ArtStyles.label(selected)}",
                style = jua(16, V2C.Title), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            if (serverNote != null) {
                Spacer(Modifier.height(10.dp))
                V2Banner(serverNote, icon = Icons.Rounded.Info)
            }
            Spacer(Modifier.height(14.dp))
            ArtStyles.all.chunked(columns).forEach { row ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { s -> StyleTile(s, s.id == selected, previews[s.id], Modifier.weight(1f)) { onSelect(s.id) } }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(12.dp))
            }
            V2Banner("고른 그림체는 '그림 만들기'에만 쓰여요.\n이미 저장한 그림은 바뀌지 않아요.", icon = Icons.Rounded.Info)
            Spacer(Modifier.height(16.dp))
        }
        V2PrimaryButton("다 골랐어요") { onBack() }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StyleTile(style: ArtStyles.Style, selected: Boolean, preview: ByteArray?, modifier: Modifier, onClick: () -> Unit) {
    val bmp = remember(preview) {
        preview?.let { b -> runCatching { BitmapFactory.decodeByteArray(b, 0, b.size, BitmapFactory.Options().apply { inSampleSize = 4 }) }.getOrNull() }
    }
    Column(
        modifier
            .background(if (selected) V2C.GreenBg else V2C.Surface, RoundedCornerShape(22.dp))
            .border(if (selected) 3.dp else 1.5.dp, if (selected) V2C.GreenText else Color(0xFFE4DEF2), RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (bmp != null) {
            Image(bmp.asImageBitmap(), null, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
        } else {
            Box(Modifier.fillMaxWidth().aspectRatio(1f).background(V2C.CardInner, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                CardGlyph(size = 30)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(if (selected) "✓ ${style.label}" else style.label, style = jua(17, if (selected) V2C.GreenText else V2C.Ink))
    }
}
