package kr.voicemate.malitda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.voicemate.malitda.ui.figma.FigmaApp
import kr.voicemate.malitda.ui.theme.A11yPrefs
import kr.voicemate.malitda.ui.theme.MalitdaTheme
import kr.voicemate.malitda.ui.vm.SessionViewModel

class MainActivity : ComponentActivity() {
    private var sessionStarted = false
    private val vm: SessionViewModel by viewModels { SessionViewModel.factory((application as MalitdaApp).container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            kr.voicemate.malitda.ui.v2.DatabaseReadiness((application as MalitdaApp).container) {
            sessionStarted = true
            val settings by vm.settings.collectAsStateWithLifecycle()
            val preferences by vm.profilePreferences.collectAsStateWithLifecycle()
            MalitdaTheme(
                fontScale = settings.fontScale,
                a11y = A11yPrefs(visualEmphasis = settings.visualEmphasis, haptics = preferences.haptics, ttsRate = preferences.ttsRate),
            ) {
                // v2(42화면) 의미 카드 흐름 — U00~U04 핵심 학생 흐름(실 STT·TTS·의미 사전 연결)
                kr.voicemate.malitda.ui.v2.MalitdaV2App(vm)
            }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (sessionStarted) vm.pauseForBackground()
    }
}
