package kr.voicemate.malitda.ui.v2

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.voicemate.malitda.R

/**
 * v2 공통 스타일(디자인 42화면 기준).
 * 서체는 Jua Regular 하나뿐이다 — 가짜 볼드(가상 굵기)를 쓰지 않는다(전달 자료 지침).
 */
val JuaV2 = FontFamily(Font(R.font.jua, FontWeight.Normal))

object V2C {
    val BgTop = Color(0xFFF7F4FD)
    val BgBottom = Color(0xFFEFECF8)
    val Title = Color(0xFF7B4FF0)
    val Ink = Color(0xFF2E2E4A)
    val Muted = Color(0xFF8A84A3)
    val Surface = Color(0xFFFFFFFF)
    val CardInner = Color(0xFFF1EDFA)
    val BannerBg = Color(0xFFEFEBFA)
    val BannerIcon = Color(0xFF7B4FF0)
    val GreenBg = Color(0xFFE6F6EC)
    val GreenText = Color(0xFF2F8F5D)
    val GradStart = Color(0xFF4A6CF8)
    val GradEnd = Color(0xFFA64DF3)
    val Shadow = Color(0x337B4FF0)
}

val V2Gradient = Brush.horizontalGradient(listOf(V2C.GradStart, V2C.GradEnd))

fun jua(size: Int, color: Color, lineMul: Float = 1.35f) = TextStyle(
    fontFamily = JuaV2, fontWeight = FontWeight.Normal,
    fontSize = size.sp, lineHeight = (size * lineMul).sp, color = color,
)

/** 화면 배경(연보라 그라데이션) + 안전영역. */
@Composable
fun V2Screen(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(V2C.BgTop, V2C.BgBottom)))
            .statusBarsPadding()
            .navigationBarsPadding(),
        content = content,
    )
}

/** 상단 헤더: 뒤로가기(선택) · 로고 · 도움. */
@Composable
fun V2Header(onBack: (() -> Unit)? = null, onHelp: () -> Unit, logoStart: Boolean = false) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (onBack != null) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .size(48.dp)
                    .shadow(3.dp, CircleShape, clip = false, spotColor = V2C.Shadow)
                    .background(V2C.Surface, CircleShape)
                    .clip(CircleShape)
                    .clickable(onClickLabel = "뒤로 가기") { onBack() },
                contentAlignment = Alignment.Center,
            ) { Image(painterResource(R.drawable.ic_back), null, Modifier.size(22.dp)) }
        }
        Image(
            painterResource(R.drawable.v2_logo), contentDescription = "말잇다",
            modifier = Modifier
                .align(if (logoStart) Alignment.CenterStart else Alignment.Center)
                .height(52.dp),
            contentScale = ContentScale.Fit,
        )
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .height(44.dp)
                .shadow(3.dp, CircleShape, clip = false, spotColor = V2C.Shadow)
                .background(V2C.Surface, CircleShape)
                .clip(CircleShape)
                .clickable { onHelp() }
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) { Text("도움", style = jua(16, V2C.Title)) }
    }
}

@Composable
fun V2Title(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = jua(28, V2C.Title, 1.3f), textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

@Composable
fun V2SubTitle(text: String) {
    Text(
        text, style = jua(17, V2C.Ink), textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
    )
}

/** 가득 찬 그라데이션 주 버튼(높이 64dp, 알약형). */
@Composable
fun V2PrimaryButton(text: String, modifier: Modifier = Modifier, icon: String? = null, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(64.dp)
            .shadow(6.dp, RoundedCornerShape(32.dp), clip = false, spotColor = V2C.Shadow)
            .background(V2Gradient, RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(if (icon != null) "$icon  $text" else text, style = jua(22, Color.White))
    }
}

/** 흰 배경 보조 버튼. */
@Composable
fun V2SecondaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(60.dp)
            .shadow(4.dp, RoundedCornerShape(30.dp), clip = false, spotColor = V2C.Shadow)
            .background(V2C.Surface, RoundedCornerShape(30.dp))
            .clip(RoundedCornerShape(30.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(text, style = jua(20, V2C.Title)) }
}

/** 맨 아래 글자만 있는 버튼(취소 등). */
@Composable
fun V2TextButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(text, style = jua(18, V2C.Muted)) }
}

/** 안내 배너(연보라/연녹색). */
@Composable
fun V2Banner(text: String, icon: ImageVector? = null, green: Boolean = false, leading: (@Composable () -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(if (green) V2C.GreenBg else V2C.BannerBg, RoundedCornerShape(22.dp))
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            leading != null -> { leading(); Spacer(Modifier.width(14.dp)) }
            icon != null -> {
                Icon(icon, null, tint = if (green) V2C.GreenText else V2C.BannerIcon, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(14.dp))
            }
        }
        Text(text, style = jua(17, if (green) V2C.GreenText else V2C.Ink, 1.5f))
    }
}

/** 큰 흰색 카드 컨테이너. */
@Composable
fun V2Card(modifier: Modifier = Modifier, radius: Int = 32, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(8.dp, RoundedCornerShape(radius.dp), clip = false, spotColor = V2C.Shadow)
            .background(V2C.Surface, RoundedCornerShape(radius.dp))
            .clip(RoundedCornerShape(radius.dp)),
        content = content,
    )
}

/** 겹친 카드 모양 글리프(디자인의 '카드' 아이콘). */
@Composable
fun CardGlyph(size: Int = 22, tint: Color = V2C.BannerIcon) {
    val corner = RoundedCornerShape((size * 0.2f).dp)
    Box(Modifier.size(size.dp)) {
        Box(
            Modifier
                .size((size * 0.72f).dp)
                .align(Alignment.TopEnd)
                .border(2.dp, tint, corner)
        )
        Box(
            Modifier
                .size((size * 0.72f).dp)
                .align(Alignment.BottomStart)
                .background(V2C.BannerBg, corner)
                .border(2.dp, tint, corner)
        )
    }
}

/** 내장 의미 그림 키 → 리소스. 모르는 키·null은 그림 없음(U03-I)으로 다룬다. */
fun meaningImageRes(imageKey: String?): Int? = when (imageKey) {
    "walk" -> R.drawable.v2_card_walk
    "water" -> R.drawable.v2_card_water
    "rest" -> R.drawable.v2_card_rest
    "cold" -> R.drawable.v2_card_cold
    else -> null
}

@Composable
fun meaningPainter(imageKey: String?): Painter? = meaningImageRes(imageKey)?.let { painterResource(it) }

/** 이름 호격: 받침 있으면 '아', 없으면 '야'. */
fun vocative(name: String): String {
    if (name.isBlank()) return ""
    val last = name.last()
    if (last !in '가'..'힣') return name
    return name + if ((last - '가') % 28 > 0) "아" else "야"
}

/** 동반 조사: 받침 있으면 '과', 없으면 '와'. */
fun withJosa(name: String): String {
    if (name.isBlank()) return ""
    val last = name.last()
    if (last !in '가'..'힣') return name + "와"
    return name + if ((last - '가') % 28 > 0) "과" else "와"
}

/** 하단 탭(홈·내 표현·설정). U01 계열에서만 쓴다. */
@Composable
fun V2BottomNav(onLibrary: () -> Unit, onSettings: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(6.dp, RoundedCornerShape(28.dp), clip = false, spotColor = V2C.Shadow)
            .background(V2C.Surface, RoundedCornerShape(28.dp))
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavItem("⌂", "홈", selected = true) {}
        NavItem("♡", "내 표현", selected = false, onClick = onLibrary)
        NavItem("⚙", "설정", selected = false, onClick = onSettings)
    }
}

@Composable
private fun NavItem(glyph: String, label: String, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) V2C.Title else V2C.Muted
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(interactionSource = rememberNoRipple(), indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 4.dp)
            .semantics { contentDescription = label },
    ) {
        Box(
            Modifier
                .size(width = 56.dp, height = 34.dp)
                .background(if (selected) V2C.BannerBg else Color.Transparent, RoundedCornerShape(17.dp)),
            contentAlignment = Alignment.Center,
        ) { Text(glyph, style = jua(20, tint)) }
        Text(label, style = jua(13, tint))
    }
}

@Composable
private fun rememberNoRipple(): MutableInteractionSource =
    androidx.compose.runtime.remember { MutableInteractionSource() }
