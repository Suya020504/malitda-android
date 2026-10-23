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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
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

/**
 * 앱에 내장된 의미 그림 목록. 그림 선택 화면(U05-M)이 이 순서대로 보여 준다.
 * 새 그림 추가: res/drawable-nodpi/v2_card_<키>.png를 넣고 여기에 한 줄 추가(docs/meaning-image-guide.md).
 * 키는 DB(meanings.imageKey)에 저장되므로 한 번 쓴 키는 바꾸거나 지우지 않는다.
 */
val BUILTIN_MEANING_IMAGES: List<Pair<String, Int>> = listOf(
    "walk" to R.drawable.v2_card_walk,
    "water" to R.drawable.v2_card_water,
    "rest" to R.drawable.v2_card_rest,
    "cold" to R.drawable.v2_card_cold,
)

private val BUILTIN_IMAGE_MAP = BUILTIN_MEANING_IMAGES.toMap()

/** 내장 의미 그림 키 → 리소스. 모르는 키·null은 그림 없음(U03-I)으로 다룬다. */
fun meaningImageRes(imageKey: String?): Int? = imageKey?.let { BUILTIN_IMAGE_MAP[it] }

@Composable
fun meaningPainter(imageKey: String?): Painter? = meaningImageRes(imageKey)?.let { painterResource(it) }

/** 사진 키("file:…")면 저장된 비트맵을 불러온다. */
@Composable
fun meaningFileBitmap(imageKey: String?): ImageBitmap? {
    val ctx = LocalContext.current
    return androidx.compose.runtime.remember(imageKey) {
        kr.voicemate.malitda.data.repo.MeaningImageStore.fileFor(ctx.filesDir, imageKey)
            ?.let { runCatching { android.graphics.BitmapFactory.decodeFile(it.path) }.getOrNull() }
            ?.asImageBitmap()
    }
}

/** 보여줄 수 있는 그림(내장·사진)이 있는가 — 없으면 U03-I 모습을 쓴다. */
@Composable
fun meaningImageAvailable(imageKey: String?): Boolean =
    meaningImageRes(imageKey) != null || meaningFileBitmap(imageKey) != null

/** 의미 그림 하나를 그린다: 내장 리소스 → 저장 사진 → 자리표시(카드 글리프) 순. */
@Composable
fun MeaningPicture(imageKey: String?, modifier: Modifier, contentScale: ContentScale = ContentScale.Crop, glyphSize: Int = 34) {
    val res = meaningImageRes(imageKey)
    if (res != null) {
        Image(painterResource(res), null, modifier, contentScale = contentScale)
        return
    }
    val bmp = meaningFileBitmap(imageKey)
    if (bmp != null) {
        Image(bmp, null, modifier, contentScale = contentScale)
        return
    }
    Box(modifier.background(V2C.CardInner), contentAlignment = Alignment.Center) { CardGlyph(size = glyphSize) }
}

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

/** 도구 조사: 받침 없거나 ㄹ 받침이면 '로', 그 외 받침은 '으로'. */
fun roJosa(name: String): String {
    if (name.isBlank()) return ""
    val last = name.last()
    if (last !in '가'..'힣') return name + "로"
    val jong = (last - '가') % 28
    return name + if (jong == 0 || jong == 8) "로" else "으로"
}

/** 주격 조사: 받침 있으면 '이', 없으면 '가'. */
fun iGa(name: String): String {
    if (name.isBlank()) return ""
    val last = name.last()
    if (last !in '가'..'힣') return name + "가"
    return name + if ((last - '가') % 28 > 0) "이" else "가"
}

/** v2 입력 칸(U05). error면 붉은 테두리·분홍 배경(U05-E). */
@Composable
fun V2TextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    error: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .fillMaxWidth()
            .background(if (error) Color(0xFFFDF1F3) else V2C.Surface, shape)
            .border(1.5.dp, if (error) Color(0xFFE2708A) else Color(0xFFE4DEF2), shape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(placeholder, style = jua(18, if (error) Color(0xFFD95D7B) else Color(0xFFB4AECB)))
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = jua(18, V2C.Ink),
            singleLine = true,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(V2C.Title),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 하단 탭(홈·내 표현·설정). selected: "home" | "library" | "settings". */
@Composable
fun V2BottomNav(selected: String, onHome: () -> Unit, onLibrary: () -> Unit, onSettings: () -> Unit) {
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
        NavItem("⌂", "홈", selected = selected == "home", onClick = onHome)
        NavItem("♡", "내 표현", selected = selected == "library", onClick = onLibrary)
        NavItem("⚙", "설정", selected = selected == "settings", onClick = onSettings)
    }
}

/** 화면별 도움말 창. '도움' 버튼으로 연다. */
@Composable
fun V2HelpDialog(text: String, onClose: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(V2C.Surface, RoundedCornerShape(28.dp))
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("도움", style = jua(24, V2C.Title))
            Spacer(Modifier.height(14.dp))
            Text(text, style = jua(18, V2C.Ink, 1.6f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(V2Gradient, RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .clickable { onClose() },
                contentAlignment = Alignment.Center,
            ) { Text("알겠어요", style = jua(20, Color.White)) }
        }
    }
}

/** 화면 ID별 도움말 문구. */
fun helpTextFor(route: String): String = when (route) {
    "U00" -> "말잇다는 학생이 평소 쓰는 말을\n상대가 알아들을 수 있게 이어 줘요.\n처음에는 지원자와 함께 뜻을 등록해요."
    "U01" -> "'말하기'를 누르고 천천히 말해요.\n말하기 어려우면 아래 카드에서\n전하고 싶은 뜻을 골라도 돼요."
    "U02", "U02D" -> "말이 끝나면 '말하기 끝내기'를 눌러요.\n잘 안 되면 '카드로 고르기'를 눌러요."
    "U03" -> "그림과 문장을 보고, 내가 전하고 싶은 뜻이면\n'맞아요', 아니면 '다른 뜻이에요'를 눌러요."
    "U03E", "U03M" -> "다시 말하거나, 카드에서 뜻을 골라요.\n자주 쓰는 말이면 지원자와 함께 등록해요."
    "U04", "U04F", "U04S", "U13" -> "이 화면을 상대에게 보여 주거나\n'소리로 들려주기'를 눌러 전해요."
    "U06" -> "그림을 보고 전하고 싶은 뜻을 골라요.\n없으면 '여기에 없어요'를 눌러요."
    "U05", "U05M" -> "학생이 평소 쓰는 말과 전할 뜻을 적어요.\n그림을 고르면 글을 읽기 어려운 학생도\n뜻을 확인할 수 있어요."
    "U07", "U07S" -> "누구의 표현인지 먼저 확인해요.\n뜻은 지원자 혼자 정하지 않고\n학생과 함께 확인해요."
    "U10" -> "그림·문장·소리로 학생에게 뜻을 보여 주고,\n학생이 맞다고 하면 체크한 뒤\n'이 뜻 사용하기'를 눌러요."
    "U08", "U09", "U09D", "U11" -> "등록한 뜻을 고치거나 잠시 쉬게 할 수 있어요.\n바꾼 뜻은 학생과 다시 확인해야 쓰여요."
    "U14" -> "실증 기록을 켜면 대화마다 결과만 남아요.\n학생이 한 말은 남기지 않아요."
    "U15" -> "학생이 보기 편한 그림체를 골라요.\n'그림 만들기'로 새 그림을 만들 때 쓰여요.\n고르는 것만으로 뜻이 확정되지는 않아요."
    else -> "궁금한 점은 지원자와 함께 확인해요."
}

/** 붉은 위험 버튼(U09-D 삭제하기). */
@Composable
fun V2DangerButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(64.dp)
            .shadow(6.dp, RoundedCornerShape(32.dp), clip = false, spotColor = Color(0x33E2566E))
            .background(
                Brush.horizontalGradient(listOf(Color(0xFFE85C7A), Color(0xFFE0485F))),
                RoundedCornerShape(32.dp),
            )
            .clip(RoundedCornerShape(32.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(text, style = jua(22, Color.White)) }
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
