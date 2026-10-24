package kr.voicemate.malitda.ui.v2

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kr.voicemate.malitda.data.db.MeaningEntity

/** U11 · 아직 확인하지 않은 뜻(화면 28) — 미확인·사용 중지 보관함. */
@Composable
fun U11Archive(
    meanings: List<MeaningEntity>,
    onConfirm: (MeaningEntity) -> Unit,
    onOpen: (MeaningEntity) -> Unit,
    onToLibrary: () -> Unit,
    onBack: () -> Unit,
    onHelp: () -> Unit,
) {
    V2Screen {
        V2Header(onBack = onBack, onHelp = onHelp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("아직 확인하지 않은 뜻")
            V2SubTitle("함께 확인하기 전에는\n학생의 후보에 나타나지 않아요.")
            Spacer(Modifier.height(16.dp))
            if (meanings.isEmpty()) {
                V2Banner("보관함이 비어 있어요.\n모든 뜻을 함께 확인했어요.", leading = { CardGlyph() })
                Spacer(Modifier.height(16.dp))
            }
            meanings.forEach { m ->
                LibraryRow(m, chip = if (!m.confirmed) "미확인" else "사용 중지") { onOpen(m) }
                Spacer(Modifier.height(14.dp))
            }
            Spacer(Modifier.height(4.dp))
        }
        if (meanings.isNotEmpty()) {
            V2PrimaryButton("학생과 뜻 확인하기", icon = "✓") { onConfirm(meanings.first()) }
            Spacer(Modifier.height(12.dp))
        }
        V2SecondaryButton("내 표현으로") { onToLibrary() }
        Spacer(Modifier.height(16.dp))
    }
}
