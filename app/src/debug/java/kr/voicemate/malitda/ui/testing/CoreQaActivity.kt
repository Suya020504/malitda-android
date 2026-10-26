package kr.voicemate.malitda.ui.testing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import kr.voicemate.malitda.ui.vm.SessionViewModel

/** No intent-driven injection or public entry. Instrumentation supplies an isolated VM and real V2 UI. */
class CoreQaActivity : ComponentActivity() {
    var qaViewModel: SessionViewModel? = null
    override fun onCreate(state: Bundle?) { super.onCreate(state); enableEdgeToEdge() }
    override fun onStop() { super.onStop(); qaViewModel?.pauseForBackground() }
}
