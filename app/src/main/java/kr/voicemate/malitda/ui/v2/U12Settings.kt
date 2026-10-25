package kr.voicemate.malitda.ui.v2

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import kr.voicemate.malitda.R

/** U12 · 설정(화면 31). 하단 탭 '설정'. */
@Composable
fun U12Settings(
    studentName: String,
    onStudent: () -> Unit,
    onMicGuide: () -> Unit,
    onSoundGuide: () -> Unit,
    onRegister: () -> Unit,
    onFirstGuide: () -> Unit,
    onHome: () -> Unit,
    onLibrary: () -> Unit,
    onHelp: () -> Unit,
    largeText: Boolean = false,
    onLargeText: (Boolean) -> Unit = {},
    trialLogging: Boolean = false,
    onTrialLog: () -> Unit = {},
    onRemoveSamples: () -> Unit = {},
    artStyleLabel: String? = null,
    onArtStyle: () -> Unit = {},
    onPreferences: (() -> Unit)? = null,
    onPrivacy: (() -> Unit)? = null,
    onAppInfo: (() -> Unit)? = null,
    onDeleteAll: (() -> Unit)? = null,
    onResetRanking: (() -> Unit)? = null,
    onLock: (() -> Unit)? = null,
) {
    var confirmRemove by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var confirmDeleteAll by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var confirmReset by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val enlarged = LocalDensity.current.fontScale >= 1.4f
    V2Screen {
        V2Header(onHelp = onHelp, logoStart = true)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(4.dp))
            V2Card(radius = 28) {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .weight(1f)
                                .background(
                                    Brush.horizontalGradient(listOf(Color(0xFFF2E9FA), Color(0xFFEDF1FB))),
                                    RoundedCornerShape(20.dp),
                                )
                                .padding(horizontal = 20.dp, vertical = 26.dp),
                        ) { Text("${studentName.ifBlank { "친구" }}의 말잇다", style = jua(26, V2C.Title)) }
                        if (!enlarged) {
                            Spacer(Modifier.width(12.dp))
                            Image(
                            painterResource(R.drawable.v2_char_robot), contentDescription = null,
                            modifier = Modifier.size(84.dp).clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Fit,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp)
                            .background(Color(0xFFE2F4E8), RoundedCornerShape(18.dp))
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                    ) {
                        Text("그림·글·소리로 뜻 확인\n학생에게 편한 방법을 함께 고릅니다.", style = jua(15, V2C.GreenText, 1.5f))
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            SettingRow(icon = Icons.Rounded.Check, label = "사용할 학생 확인") { onStudent() }
            Spacer(Modifier.height(12.dp))
            SettingRow(iconPainter = R.drawable.ic_consent_mic, label = "마이크 권한 안내") { onMicGuide() }
            Spacer(Modifier.height(12.dp))
            SettingRow(icon = Icons.Rounded.Settings, label = "소리 설정 안내") { onSoundGuide() }
            Spacer(Modifier.height(12.dp))
            SettingRow(icon = Icons.Rounded.AddCircle, label = "지원자와 함께 등록") { onRegister() }
            Spacer(Modifier.height(12.dp))
            if (onPreferences != null) {
                SettingRow(icon = Icons.Rounded.Settings, label = "확인 방법과 접근성", onClick = onPreferences)
            } else {
                SettingRow(icon = Icons.Rounded.Info, label = if (largeText) "글자 크기: 크게" else "글자 크기: 보통") { onLargeText(!largeText) }
            }
            if (artStyleLabel != null) {
                Spacer(Modifier.height(12.dp))
                SettingRow(icon = Icons.Rounded.AddCircle, label = "그림체: $artStyleLabel") { onArtStyle() }
            }
            Spacer(Modifier.height(12.dp))
            SettingRow(icon = Icons.Rounded.Check, label = if (trialLogging) "실증 기록 (기록 중)" else "실증 기록") { onTrialLog() }
            Spacer(Modifier.height(10.dp))
            V2TextButton("처음 화면 안내") { onFirstGuide() }
            V2TextButton(if (confirmRemove) "한 번 더 누르면 예시 뜻이 지워져요(그림을 바꾼 뜻은 남아요)" else "예시 뜻 지우기 (실증 전)") {
                if (confirmRemove) { onRemoveSamples(); confirmRemove = false } else confirmRemove = true
            }
            if (onPrivacy != null) V2TextButton("개인정보와 사용 안내", onClick = onPrivacy)
            if (onAppInfo != null) V2TextButton("앱 정보와 문의", onClick = onAppInfo)
            if (onResetRanking != null) V2TextButton("현재 학생의 추천 순서 초기화", onClick = { confirmReset = true })
            if (onDeleteAll != null) V2TextButton("이 기기의 전체 자료 삭제", onClick = { confirmDeleteAll = true })
            if (onLock != null) V2SecondaryButton("지원자 모드 잠그고 홈으로", onClick = onLock)
            Spacer(Modifier.height(8.dp))
        }
        V2BottomNav(selected = "settings", onHome = onHome, onLibrary = onLibrary, onSettings = {})
    }
    if (confirmDeleteAll) androidx.compose.material3.AlertDialog(
        onDismissRequest = { confirmDeleteAll = false },
        title = { Text("전체 자료를 지울까요?", style = jua(23, V2C.Title)) },
        text = { Text("이 기기에 저장한 모든 학생·표현·뜻·사진·확인 기록·실증 기록·설정을 삭제합니다. 되돌릴 수 없습니다. 다른 앱으로 이미 공유한 자료나 서버 자료는 지워지지 않습니다.", style = jua(17, V2C.Ink)) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { confirmDeleteAll = false; onDeleteAll?.invoke() }) { Text("전체 삭제") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmDeleteAll = false }) { Text("돌아가기") } },
    )
    if (confirmReset) androidx.compose.material3.AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text("추천 순서를 초기화할까요?", style = jua(23, V2C.Title)) },
        text = { Text("현재 학생의 사용 횟수와 교정 이력을 초기화합니다. 직접 등록한 뜻·표현·그림은 남깁니다.", style = jua(17, V2C.Ink)) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { confirmReset = false; onResetRanking?.invoke() }) { Text("순서 초기화") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmReset = false }) { Text("돌아가기") } },
    )
}

@Composable
private fun SettingRow(label: String, icon: ImageVector? = null, iconPainter: Int? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(5.dp, RoundedCornerShape(26.dp), clip = false, spotColor = V2C.Shadow)
            .background(V2C.Surface, RoundedCornerShape(26.dp))
            .clip(RoundedCornerShape(26.dp))
            .clickable(role = Role.Button) { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).background(V2C.BannerBg, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            when {
                icon != null -> Icon(icon, null, tint = V2C.BannerIcon, modifier = Modifier.size(24.dp))
                iconPainter != null -> Image(painterResource(iconPainter), null, Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Text(label, style = jua(19, V2C.Title), modifier = Modifier.weight(1f))
        if (LocalDensity.current.fontScale < 1.4f) Text("→", style = jua(20, V2C.Title), modifier = Modifier.clearAndSetSemantics { })
    }
}

/** U12-P · 마이크 권한 켜기(화면 32). */
@Composable
fun U12MicGuide(
    onPickCards: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    onHelp: () -> Unit,
) {
    V2Screen {
        V2Header(onBack = onBack, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("마이크 권한 켜기")
            Spacer(Modifier.height(8.dp))
            V2Banner("휴대전화 설정에서 허용해 주세요.", leading = { Image(painterResource(R.drawable.ic_consent_mic), null, Modifier.size(24.dp)) })
            Spacer(Modifier.height(14.dp))
            StepRow(1, "휴대전화 설정에서\n앱을 열어요.")
            Spacer(Modifier.height(12.dp))
            StepRow(2, "말잇다를 선택해요.")
            Spacer(Modifier.height(12.dp))
            StepRow(3, "권한에서 마이크를\n허용해요.")
            Spacer(Modifier.height(14.dp))
            V2Banner("휴대전화에 따라 메뉴 이름이\n조금 다를 수 있어요.", icon = Icons.Rounded.Info)
            Spacer(Modifier.height(16.dp))
        }
        V2PrimaryButton("카드로 말하기") { onPickCards() }
        Spacer(Modifier.height(12.dp))
        V2SecondaryButton("설정으로") { onOpenSettings() }
        Spacer(Modifier.height(16.dp))
    }
}

/** U12-T · 소리가 나오지 않나요?(화면 33). */
@Composable
fun U12SoundGuide(
    onPickCards: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    onHelp: () -> Unit,
) {
    V2Screen {
        V2Header(onBack = onBack, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("소리가 나오지 않나요?")
            Spacer(Modifier.height(8.dp))
            V2Banner("음량과 한국어 음성을\n확인해 주세요.", icon = Icons.Rounded.Info)
            Spacer(Modifier.height(14.dp))
            GuideCard(Icons.Rounded.Info, "휴대전화 소리 크기를\n확인해요.")
            Spacer(Modifier.height(12.dp))
            GuideCard(Icons.Rounded.Settings, "음성 설정에서\n한국어 음성을 준비해요.")
            Spacer(Modifier.height(12.dp))
            GuideCard(Icons.Rounded.Check, "도움이 필요하면 지원자와\n함께 확인해요.")
            Spacer(Modifier.height(14.dp))
            V2Banner("음성 준비에 인터넷이\n필요할 수 있어요.", icon = Icons.Rounded.Info)
            Spacer(Modifier.height(16.dp))
        }
        V2PrimaryButton("카드로 뜻 다시 고르기") { onPickCards() }
        Spacer(Modifier.height(12.dp))
        V2SecondaryButton("설정으로") { onOpenSettings() }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StepRow(n: Int, text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(4.dp, RoundedCornerShape(26.dp), clip = false, spotColor = V2C.Shadow)
            .background(V2C.Surface, RoundedCornerShape(26.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(46.dp).background(V2C.BannerBg, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) { Text("$n", style = jua(22, V2C.Title)) }
        Spacer(Modifier.width(16.dp))
        Text(text, style = jua(18, V2C.Ink, 1.5f))
    }
}

@Composable
private fun GuideCard(icon: ImageVector, text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(4.dp, RoundedCornerShape(26.dp), clip = false, spotColor = V2C.Shadow)
            .background(V2C.Surface, RoundedCornerShape(26.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(46.dp).background(V2C.BannerBg, RoundedCornerShape(23.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = V2C.BannerIcon, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(16.dp))
        Text(text, style = jua(18, V2C.Ink, 1.5f))
    }
}
