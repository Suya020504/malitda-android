package kr.voicemate.malitda.ui.v2

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.voicemate.malitda.di.AppContainer
import kr.voicemate.malitda.ui.theme.MalitdaTheme

/** Keep consent and accessibility defaults off screen until storage has loaded. */
@Composable
fun SettingsLoadingScreen() {
    V2Screen {
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        ) {
            androidx.compose.foundation.Image(
                androidx.compose.ui.res.painterResource(kr.voicemate.malitda.R.drawable.v2_logo),
                contentDescription = "말잇다",
                modifier = Modifier.width(180.dp).height(52.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            )
            Spacer(Modifier.height(24.dp))
            androidx.compose.material3.CircularProgressIndicator(Modifier.size(32.dp))
            Spacer(Modifier.height(20.dp))
            Text("저장한 설정을 불러옵니다", style = jua(18, V2C.Ink))
        }
    }
}

/** Open/migrate first so a failed legacy schema cannot crash several eager ViewModel flows. */
@Composable
fun DatabaseReadiness(container: AppContainer, content: @Composable () -> Unit) {
    var ready by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableStateOf(0) }
    val context = LocalContext.current
    LaunchedEffect(retry) {
        failed = false
        try {
            withContext(Dispatchers.IO) { container.db.openHelper.writableDatabase }
            ready = true
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            failed = true
        }
    }
    if (ready) content() else MalitdaTheme {
        V2Screen {
            V2Header(onHelp = { retry++ })
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                V2Title(if (failed) "보관 자료를 열지 못했습니다" else "말잇다를 준비합니다")
                if (failed) {
                    V2Banner("이전 자료를 임의로 삭제하거나 덮어쓰지 않았습니다. 다시 시도해도 열리지 않으면 앱 버전을 확인하고 개발자에게 복구를 요청합니다.")
                    Spacer(Modifier.height(16.dp))
                    V2PrimaryButton("다시 열기", onClick = { retry++ })
                    V2SecondaryButton("개발 프로젝트·복구 문의", onClick = {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/choihyungyu0/malitda-android/issues"))) }
                    })
                    V2TextButton("Android 앱 정보", onClick = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null))) }
                    })
                    V2SubTitle("앱 정보에서 저장공간을 초기화하거나 앱을 삭제하면 보관한 자료가 사라집니다. 복구가 필요하면 먼저 초기화하지 않습니다. 공개 문의에 이름·문장·사진·키를 올리지 않습니다.")
                    V2TextButton("앱 닫기", onClick = { (context as? Activity)?.finish() })
                } else Text("기기 안의 자료를 확인하고 있습니다.", style = jua(18, V2C.Ink), modifier = Modifier.padding(24.dp))
            }
        }
    }
}
