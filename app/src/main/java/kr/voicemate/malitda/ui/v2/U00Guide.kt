package kr.voicemate.malitda.ui.v2

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kr.voicemate.malitda.R

/** U00 · 처음 화면 안내(화면 37). */
@Composable
fun U00Guide(onStart: () -> Unit, onPrepare: () -> Unit, onBack: (() -> Unit)?, onHelp: () -> Unit) {
    V2Screen {
        V2Header(onBack = onBack, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(8.dp))
            V2Card {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFFDEDF4), Color(0xFFEDF4FD), Color(0xFFEDFDF4)))
                        )
                        .padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painterResource(R.drawable.v2_char_robot), contentDescription = null,
                        modifier = Modifier.size(150.dp).clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "내 표현을 함께\n이해하는 말잇다",
                        style = jua(28, V2C.Title, 1.35f), textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            GuideRow(icon = { Image(painterResource(R.drawable.ic_consent_mic), null, Modifier.size(24.dp)) }, text = "말하거나 카드로 골라요.")
            Spacer(Modifier.height(12.dp))
            GuideRow(icon = { CardGlyph() }, text = "그림과 문장으로 뜻을 확인해요.")
            Spacer(Modifier.height(12.dp))
            GuideRow(icon = { Icon(Icons.Rounded.Check, null, tint = V2C.BannerIcon, modifier = Modifier.size(24.dp)) }, text = "맞는 뜻을 상대에게 보여줘요.")
            Spacer(Modifier.height(16.dp))
            V2Banner(
                "처음에는 지원자와\n자주 쓰는 표현의 뜻을 준비해요.",
                green = true,
                leading = { Icon(Icons.Rounded.AddCircle, null, tint = V2C.GreenText, modifier = Modifier.size(26.dp)) },
            )
            Spacer(Modifier.height(16.dp))
        }
        V2PrimaryButton("말잇다 시작") { onStart() }
        Spacer(Modifier.height(12.dp))
        V2SecondaryButton("뜻 함께 준비하기") { onPrepare() }
        Spacer(Modifier.height(16.dp))
    }
}

/** 안내 항목 한 줄(흰 알약 + 아이콘 칩). */
@Composable
private fun GuideRow(icon: @Composable () -> Unit, text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(4.dp, RoundedCornerShape(26.dp), clip = false, spotColor = V2C.Shadow)
            .background(V2C.Surface, RoundedCornerShape(26.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).background(V2C.BannerBg, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) { icon() }
        Spacer(Modifier.width(14.dp))
        Text(text, style = jua(18, V2C.Ink))
    }
}
