package kr.voicemate.malitda.ui.v2

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kr.voicemate.malitda.R
import kr.voicemate.malitda.data.db.MeaningEntity
import kr.voicemate.malitda.ui.vm.SessionViewModel.V2Origin

/** 출처 배지 문구. */
fun originBadge(origin: V2Origin) = if (origin == V2Origin.CARD) "카드로 고른 뜻" else "말로 전한 뜻"

/** 연보라 배지 줄(카드로 고른 뜻 / 말로 전한 뜻). */
@Composable
fun OriginBadge(origin: V2Origin) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(V2C.BannerBg, RoundedCornerShape(22.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (origin == V2Origin.CARD) CardGlyph(size = 20)
        else Image(painterResource(R.drawable.ic_consent_mic), null, Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(originBadge(origin), style = jua(17, V2C.Title))
    }
}

/** 큰 뜻 카드: 그림(있으면) + 문장. 그림이 없으면 U03-I 모습. */
@Composable
fun MeaningBigCard(m: MeaningEntity, noImageCaption: String? = null) {
    V2Card {
        Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val painter = meaningPainter(m.imageKey)
            if (painter != null) {
                Image(
                    painter, contentDescription = null,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.42f).clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.height(22.dp))
                Text(m.text, style = jua(30, V2C.Ink), textAlign = TextAlign.Center)
                Spacer(Modifier.height(26.dp))
            } else {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(V2C.CardInner, RoundedCornerShape(24.dp))
                        .padding(vertical = 44.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CardGlyph(size = 44)
                    Spacer(Modifier.height(30.dp))
                    Text(m.text, style = jua(30, V2C.Ink), textAlign = TextAlign.Center)
                }
                if (noImageCaption != null) {
                    Spacer(Modifier.height(18.dp))
                    Text(noImageCaption, style = jua(16, V2C.Muted, 1.5f), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

/** U03 · 이 뜻이 맞나요?(화면 04·07·09·11, 그림 없으면 17). */
@Composable
fun U03Confirm(
    meaning: MeaningEntity,
    origin: V2Origin,
    studentName: String,
    onListen: () -> Unit,
    onYes: () -> Unit,
    onOther: () -> Unit,
    onBack: () -> Unit,
    onHelp: () -> Unit,
) {
    V2Screen {
        V2Header(onBack = onBack, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("이 뜻이 맞나요?")
            Spacer(Modifier.height(6.dp))
            OriginBadge(origin)
            Spacer(Modifier.height(14.dp))
            MeaningBigCard(meaning, noImageCaption = "그림을 불러올 수 없어요.\n문장이나 소리로 확인해 주세요.")
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .padding(horizontal = 20.dp)
                    .background(Color(0xFFE9E4F4), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) { Text("${withJosa(studentName)} 함께 등록한 뜻", style = jua(14, Color(0xFF6B6485))) }
            Spacer(Modifier.height(14.dp))
            V2SecondaryButton("🔊  소리로 확인하기") { onListen() }
            Spacer(Modifier.height(16.dp))
        }
        V2PrimaryButton("맞아요", icon = "✓") { onYes() }
        Spacer(Modifier.height(12.dp))
        V2SecondaryButton("다른 뜻이에요") { onOther() }
        Spacer(Modifier.height(16.dp))
    }
}

/** U03-E · 잘 듣지 못했어요(화면 15: 인식 결과 없음·오류 공용). */
@Composable
fun U03NoHear(
    onRespeak: () -> Unit,
    onPickCards: () -> Unit,
    onCancel: () -> Unit,
    onHelp: () -> Unit,
) {
    V2Screen {
        V2Header(onBack = onCancel, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("잘 듣지 못했어요")
            Spacer(Modifier.height(12.dp))
            CharacterCard(
                chip = { Image(painterResource(R.drawable.ic_consent_mic), null, Modifier.size(24.dp)) },
                caption = null,
            )
            Spacer(Modifier.height(16.dp))
            V2Banner("다시 말하거나,\n카드로 뜻을 골라 주세요.", leading = { CardGlyph() })
            Spacer(Modifier.height(16.dp))
        }
        V2PrimaryButton("다시 말하기", icon = "🎙") { onRespeak() }
        Spacer(Modifier.height(12.dp))
        V2SecondaryButton("카드로 고르기") { onPickCards() }
        V2TextButton("취소") { onCancel() }
        Spacer(Modifier.height(8.dp))
    }
}

/** U03-M · 이 말의 뜻을 아직 모르겠어요(화면 16). */
@Composable
fun U03NoMeaning(
    origin: V2Origin,
    onPickCards: () -> Unit,
    onRegister: () -> Unit,
    onRespeak: () -> Unit,
    onBack: () -> Unit,
    onHelp: () -> Unit,
) {
    V2Screen {
        V2Header(onBack = onBack, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("이 말의 뜻을\n아직 모르겠어요")
            Spacer(Modifier.height(6.dp))
            OriginBadge(origin)
            Spacer(Modifier.height(14.dp))
            V2Card {
                Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painterResource(R.drawable.v2_char_purple), null,
                        Modifier.size(170.dp).clip(RoundedCornerShape(22.dp)),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(Modifier.height(18.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .background(Color(0xFFF2EFF9), RoundedCornerShape(22.dp))
                            .padding(22.dp),
                    ) {
                        CardGlyph(size = 26)
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "함께 등록한 뜻만 보여드려요.\n지원자와 뜻을 등록하거나,\n다른 카드로 골라 주세요.",
                            style = jua(18, V2C.Ink, 1.6f),
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        V2PrimaryButton("카드로 고르기") { onPickCards() }
        Spacer(Modifier.height(12.dp))
        V2SecondaryButton("함께 뜻 등록하기") { onRegister() }
        V2TextButton("다시 말하기") { onRespeak() }
        Spacer(Modifier.height(8.dp))
    }
}

/** U03-V · 뜻이 바뀌었어요(화면 18). */
@Composable
fun U03Changed(
    onRecheck: () -> Unit,
    onHome: () -> Unit,
    onHelp: () -> Unit,
) {
    V2Screen {
        V2Header(onBack = onHome, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("뜻이 바뀌었어요")
            Spacer(Modifier.height(12.dp))
            CharacterCard(
                chip = { Icon(Icons.Rounded.Refresh, null, tint = V2C.BannerIcon, modifier = Modifier.size(26.dp)) },
                caption = null,
            )
            Spacer(Modifier.height(16.dp))
            V2Banner("등록한 뜻이 수정되었어요.\n이전 선택은 사용할 수 없어요.", icon = Icons.Rounded.Refresh)
            Spacer(Modifier.height(18.dp))
            Text(
                "바뀐 그림과 문장을\n다시 확인해 주세요.",
                style = jua(20, V2C.Ink, 1.5f), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
        }
        V2PrimaryButton("다시 뜻 확인하기", icon = "⟳") { onRecheck() }
        Spacer(Modifier.height(12.dp))
        V2SecondaryButton("홈으로 가기") { onHome() }
        Spacer(Modifier.height(16.dp))
    }
}
