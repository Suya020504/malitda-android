package kr.voicemate.malitda.ui.v2

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kr.voicemate.malitda.R

/** Three short steps; the example is not stored, sent, or scored as real use. */
@Composable
fun U00Guide(onStart: () -> Unit, onPrepare: () -> Unit, onBack: (() -> Unit)?, onHelp: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var response by rememberSaveable { mutableStateOf<String?>(null) }
    androidx.activity.compose.BackHandler(enabled = step > 0) { step-- }
    V2Screen(background = Color(0xFFFCF9F5)) {
        V2Header(onBack = if (step > 0) ({ step--; Unit }) else onBack, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text("처음 안내 · ${step + 1}/3", style = jua(14, V2C.Muted), modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            when (step) {
                0 -> {
                    V2Title("내 표현으로,\n내 뜻을 전합니다")
                    V2SubTitle("평소 쓰는 말을 전할 뜻과 이어 줍니다.")
                    Box(Modifier.fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
                        Image(painterResource(R.drawable.v2_char_robot), null, Modifier.size(128.dp), contentScale = ContentScale.Fit)
                    }
                    V2Card {
                        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("평소 쓰는 말", style = jua(15, V2C.Muted))
                            Text("“바람”", style = jua(30, V2C.Title))
                            Spacer(Modifier.height(12.dp))
                            MeaningPicture("walk", Modifier.fillMaxWidth().aspectRatio(2.2f).clip(RoundedCornerShape(18.dp)))
                            Spacer(Modifier.height(12.dp))
                            Text("산책하고 싶어요", style = jua(24, V2C.Ink), textAlign = TextAlign.Center)
                        }
                    }
                    V2SubTitle("함께 등록한 표현에서 뜻을 찾고, 내가 선택한 뜻을 전합니다.")
                }
                1 -> {
                    V2Title("내가 전할 뜻인지\n먼저 확인합니다")
                    V2SubTitle("맞는 뜻을 고르고, 모르면 잠시 멈춥니다.")
                    Spacer(Modifier.height(16.dp))
                    V2Card {
                        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            MeaningPicture("walk", Modifier.fillMaxWidth().aspectRatio(2.2f).clip(RoundedCornerShape(18.dp)))
                            Spacer(Modifier.height(12.dp))
                            Text("산책하고 싶어요", style = jua(25, V2C.Ink), textAlign = TextAlign.Center)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("맞아요", "아니에요", "모르겠어요").forEach { answer ->
                            val chosen = response == answer
                            Box(Modifier.weight(1f).heightIn(min = 60.dp)
                                .background(if (chosen) V2C.BannerBg else V2C.Surface, RoundedCornerShape(18.dp))
                                .clip(RoundedCornerShape(18.dp)).clickable(role = Role.RadioButton) { response = answer }
                                .semantics { selected = chosen }.padding(8.dp), contentAlignment = Alignment.Center) {
                                Text(answer, style = jua(17, if (chosen) V2C.Title else V2C.Ink), textAlign = TextAlign.Center)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    V2Banner(when (response) {
                        "맞아요" -> "이번에 확인한 뜻만 보여 주거나 들려줍니다."
                        "아니에요", "모르겠어요" -> "다시 고르거나 함께 확인합니다. 모르는 뜻은 보내지 않습니다."
                        else -> "그림·문장·소리로 확인하고 직접 선택합니다."
                    }, green = response == "맞아요")
                    V2SubTitle("연습용 예시입니다. 답을 저장하거나 실제로 전달하지 않습니다.")
                }
                else -> {
                    V2Title("어떻게 시작할까요?")
                    Spacer(Modifier.height(18.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Image(painterResource(R.drawable.v2_char_robot), null, Modifier.size(144.dp), contentScale = ContentScale.Fit)
                    }
                    Spacer(Modifier.height(18.dp))
                    V2PrimaryButton("내 뜻 전하기", onClick = onStart)
                    V2SubTitle("홈에서 말하거나 그림·문장으로 뜻을 고릅니다.")
                    Spacer(Modifier.height(12.dp))
                    V2SecondaryButton("표현 함께 준비하기", onClick = onPrepare)
                    V2SubTitle("보호자·교사·지인과 표현을 등록하고 당사자에게 뜻을 확인합니다. 등록과 관리는 PIN으로 보호합니다.")
                    V2Banner("처음에는 사용 안내와 보관 범위를 확인합니다. 마이크 없이도 그림이나 직접 입력으로 시작할 수 있습니다.")
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        if (step < 2) {
            V2PrimaryButton("다음") { step++ }
            V2TextButton("바로 시작 방법 고르기") { step = 2 }
        }
        Spacer(Modifier.height(12.dp))
    }
}
