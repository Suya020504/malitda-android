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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kr.voicemate.malitda.R
import kr.voicemate.malitda.data.db.MeaningEntity

/** U08 · 함께 확인한 뜻 목록(화면 25·42). 하단 탭 '내 표현'. */
@Composable
fun U08Library(
    studentName: String,
    meanings: List<MeaningEntity>,
    onOpen: (MeaningEntity) -> Unit,
    onRegister: () -> Unit,
    onArchive: () -> Unit,
    onHome: () -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
    onUse: (MeaningEntity) -> Unit = onOpen,
    onListen: (MeaningEntity) -> Unit = {},
    onFavorite: (MeaningEntity) -> Unit = {},
    aliases: Map<Long, String> = emptyMap(),
) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var favorites by rememberSaveable { mutableStateOf(false) }
    var sort by rememberSaveable { mutableStateOf(0) }
    val filtered = meanings.filter { m ->
        (category.isEmpty() || m.category == category) && (!favorites || m.favorite) &&
            (query.isBlank() || listOf(m.text, m.situation.orEmpty(), aliases[m.id].orEmpty(), meaningCategories[m.category].orEmpty())
                .any { it.contains(query.trim(), ignoreCase = true) })
    }.let { list -> when (sort) {
        1 -> list.sortedBy { it.text }
        2 -> list.sortedWith(compareBy<MeaningEntity> { it.category }.thenBy { it.text })
        else -> list.sortedWith(compareByDescending<MeaningEntity> { it.lastUsedAt }.thenByDescending { it.updatedAt })
    } }
    V2Screen {
        V2Header(onHelp = onHelp, logoStart = true)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            V2Title("함께 확인한 뜻")
            V2SubTitle("${iGa(studentName.ifBlank { "친구" })} 사용할 수 있는 뜻이에요.")
            Spacer(Modifier.height(16.dp))
            if (meanings.isEmpty()) {
                V2Banner("아직 함께 확인한 뜻이 없어요.\n지원자와 함께 등록해 주세요.", leading = { CardGlyph() })
                Spacer(Modifier.height(16.dp))
            }
            V2TextField(query, { query = it }, "뜻·평소 표현·분류 검색")
            MeaningCategoryField(category, { category = it }, allowAll = true)
            V2TextButton(if (favorites) "✓ 즐겨찾기만 보기" else "즐겨찾기만 보기", onClick = { favorites = !favorites })
            V2TextButton("정렬: ${listOf("최근 사용", "가나다", "분류")[sort]}", onClick = { sort = (sort + 1) % 3 })
            if (filtered.isEmpty() && meanings.isNotEmpty()) V2Banner("검색한 뜻이 없습니다. 검색어와 분류를 확인합니다.")
            filtered.forEach { m ->
                LibraryRow(m, chip = if (m.favorite) "★ 즐겨찾기" else meaningCategories[m.category]) { onUse(m) }
                V2TextButton("소리 듣기", onClick = { onListen(m) })
                V2TextButton(if (m.favorite) "즐겨찾기 해제" else "즐겨찾기", onClick = { onFavorite(m) })
                V2TextButton("등록 내용 관리 · 지원자", onClick = { onOpen(m) })
                Spacer(Modifier.height(14.dp))
            }
            V2PrimaryButton("뜻 함께 등록하기", icon = "⊕") { onRegister() }
            V2TextButton("미확인 보관함") { onArchive() }
            Spacer(Modifier.height(8.dp))
        }
        V2BottomNav(selected = "library", onHome = onHome, onLibrary = {}, onSettings = onSettings)
    }
}

@Composable
fun LibraryRow(m: MeaningEntity, chip: String? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(6.dp, RoundedCornerShape(26.dp), clip = false, spotColor = V2C.Shadow)
            .background(V2C.Surface, RoundedCornerShape(26.dp))
            .clip(RoundedCornerShape(26.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MeaningPicture(m.imageKey, Modifier.size(width = if (LocalDensity.current.fontScale >= 1.4f) 64.dp else 112.dp, height = 78.dp).clip(RoundedCornerShape(16.dp)), glyphSize = 28)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(m.text, style = jua(19, V2C.Ink))
            if (chip != null) {
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier.background(Color(0xFFE9E4F4), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 3.dp),
                ) { Text(chip, style = jua(12, Color(0xFF6B6485))) }
            }
            Spacer(Modifier.height(8.dp))
            Text("→", style = jua(20, V2C.Title), modifier = Modifier.align(Alignment.End))
        }
    }
}
