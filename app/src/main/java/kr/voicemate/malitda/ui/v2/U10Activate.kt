package kr.voicemate.malitda.ui.v2

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * U10 · 학생과 뜻을 확인해요(화면 23: 확인 전 → 24: 확인 후 활성화).
 * '학생과 뜻을 함께 확인했어요'를 켜야 '이 뜻 사용하기'가 열린다 — 지원자 혼자 확정하지 않는다.
 */
@Composable
fun U10Activate(
    studentName: String,
    expression: String,
    meaningText: String,
    imageKey: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onListen: () -> Unit,
    onUse: () -> Unit,
    onLater: () -> Unit,
    onBack: () -> Unit,
    onHelp: () -> Unit,
) {
    V2Screen {
        V2Header(onBack = onBack, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("학생과 뜻을 확인해요")
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
                Text("${iGa(studentName.ifBlank { "친구" })} 쓰는 표현: $expression", style = jua(17, V2C.Title))
            }
            Spacer(Modifier.height(14.dp))
            V2Card {
                Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    MeaningPicture(imageKey, Modifier.fillMaxWidth().aspectRatio(1.42f).clip(RoundedCornerShape(24.dp)), glyphSize = 44)
                    Spacer(Modifier.height(20.dp))
                    Text(meaningText, style = jua(28, V2C.Ink), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(22.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            V2SecondaryButton("🔊  소리로 확인하기") { onListen() }
            Spacer(Modifier.height(14.dp))
            // 확인 토글(23: 꺼짐 → 24: 켜짐 녹색)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(4.dp, RoundedCornerShape(26.dp), clip = false, spotColor = V2C.Shadow)
                    .background(if (checked) Color(0xFFE6F6EC) else V2C.Surface, RoundedCornerShape(26.dp))
                    .clip(RoundedCornerShape(26.dp))
                    .clickable { onCheckedChange(!checked) }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (checked) {
                    Icon(Icons.Rounded.Check, null, tint = V2C.GreenText, modifier = Modifier.size(26.dp))
                } else {
                    Box(Modifier.size(26.dp).border(2.dp, Color(0xFFC9C2DF), CircleShape))
                }
                Spacer(Modifier.width(14.dp))
                Text("학생과 뜻을 함께 확인했어요", style = jua(18, if (checked) V2C.GreenText else V2C.Ink))
            }
            Spacer(Modifier.height(16.dp))
        }
        // 확인 전에는 잠김(회색), 확인 후 활성화
        if (checked) {
            V2PrimaryButton("이 뜻 사용하기") { onUse() }
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(64.dp)
                    .background(Color(0xFFDCD8E8), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center,
            ) { Text("이 뜻 사용하기", style = jua(22, Color(0xFF9A94B0))) }
        }
        V2TextButton("나중에 확인하기") { onLater() }
        Spacer(Modifier.height(8.dp))
    }
}
