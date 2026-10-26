package kr.voicemate.malitda.ui.v2

import android.animation.ValueAnimator
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** OS animation preference is checked again when returning from Settings. */
@Composable
fun rememberV2MotionEnabled(): Boolean {
    var enabled by remember { mutableStateOf(ValueAnimator.areAnimatorsEnabled()) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) enabled = ValueAnimator.areAnimatorsEnabled()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return enabled
}

data class V2Press(val interaction: MutableInteractionSource, val modifier: Modifier)

internal fun Modifier.graphicsLayerCompatAlpha(value: Float): Modifier =
    graphicsLayer { alpha = value }

/** Transform the drawn surface only: text reflow and the touch target stay stable. */
@Composable
fun rememberV2Press(enabled: Boolean = true): V2Press {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val motion = rememberV2MotionEnabled()
    val scale by animateFloatAsState(
        if (enabled && pressed && motion) 0.985f else 1f,
        tween(if (motion) 100 else 0, easing = FastOutSlowInEasing), label = "buttonPress",
    )
    return V2Press(interaction, Modifier.graphicsLayer { scaleX = scale; scaleY = scale })
}
