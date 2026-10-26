package kr.voicemate.malitda.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kr.voicemate.malitda.R

val Pretendard = FontFamily(
    Font(R.font.pretendard_variable, FontWeight.Normal),
    Font(R.font.pretendard_variable, FontWeight.Medium),
    Font(R.font.pretendard_variable, FontWeight.SemiBold),
    Font(R.font.pretendard_variable, FontWeight.Bold),
    Font(R.font.pretendard_variable, FontWeight.ExtraBold),
)

// Keep Pretendard above for older explicit callers. All app-default Material
// roles use the original regular-weight Jua, including field labels and dialogs.
private val JuaRegular = FontFamily(Font(R.font.jua, FontWeight.Normal))

val MalitdaTypography = Typography(
    displayLarge = TextStyle(lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase), localeList = LocaleList("ko-KR"), fontFamily = JuaRegular, fontWeight = FontWeight.Normal, fontSize = 34.sp, lineHeight = 44.sp),
    displayMedium = TextStyle(lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase), localeList = LocaleList("ko-KR"), fontFamily = JuaRegular, fontWeight = FontWeight.Normal, fontSize = 30.sp, lineHeight = 40.sp),
    displaySmall = TextStyle(lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase), localeList = LocaleList("ko-KR"), fontFamily = JuaRegular, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 36.sp),
    headlineLarge = TextStyle(lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase), localeList = LocaleList("ko-KR"), fontFamily = JuaRegular, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase), localeList = LocaleList("ko-KR"), fontFamily = JuaRegular, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase), localeList = LocaleList("ko-KR"), fontFamily = JuaRegular, fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase), localeList = LocaleList("ko-KR"), fontFamily = JuaRegular, fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase), localeList = LocaleList("ko-KR"), fontFamily = JuaRegular, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase), localeList = LocaleList("ko-KR"), fontFamily = JuaRegular, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase), localeList = LocaleList("ko-KR"), fontFamily = JuaRegular, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase), localeList = LocaleList("ko-KR"), fontFamily = JuaRegular, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 23.sp),
    bodySmall = TextStyle(lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase), localeList = LocaleList("ko-KR"), fontFamily = JuaRegular, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase), localeList = LocaleList("ko-KR"), fontFamily = JuaRegular, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    labelMedium = TextStyle(lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase), localeList = LocaleList("ko-KR"), fontFamily = JuaRegular, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase), localeList = LocaleList("ko-KR"), fontFamily = JuaRegular, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
)
