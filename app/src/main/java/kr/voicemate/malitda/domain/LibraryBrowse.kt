package kr.voicemate.malitda.domain

import kr.voicemate.malitda.data.db.MeaningEntity

enum class LibraryBrowseSection { ALL, FAVORITES, SITUATIONS }
enum class LibraryBrowseSort { RECENT, ALPHABETICAL, CATEGORY }

/** 프로필과 사용 가능 상태를 먼저 제한한다. 보이지 않는 다른 탭의 필터는 적용하지 않는다. */
object LibraryBrowse {
    fun situations(meanings: List<MeaningEntity>, profileId: Long): List<String> {
        val tags = mutableListOf<String>()
        usable(meanings, profileId).forEach { meaning ->
            val tag = meaning.situation?.trim().orEmpty()
            if (tag.isNotEmpty() && tags.none { it.equals(tag, ignoreCase = true) }) tags += tag
        }
        return tags.sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    fun filter(
        meanings: List<MeaningEntity>,
        profileId: Long,
        aliases: Map<Long, String>,
        query: String,
        section: LibraryBrowseSection,
        category: String = "",
        situation: String? = null,
        sort: LibraryBrowseSort = LibraryBrowseSort.RECENT,
        categoryLabels: Map<String, String> = emptyMap(),
    ): List<MeaningEntity> {
        val search = query.trim()
        val selectedSituation = situation?.trim()
        val selectedCategory = category.trim()
        val result = usable(meanings, profileId).filter { meaning ->
            val sectionMatches = when (section) {
                LibraryBrowseSection.ALL -> selectedCategory.isEmpty() || meaning.category == selectedCategory
                LibraryBrowseSection.FAVORITES -> meaning.favorite
                LibraryBrowseSection.SITUATIONS -> meaning.situation?.trim()?.takeIf { it.isNotEmpty() }?.let { tag ->
                    selectedSituation == null || tag.equals(selectedSituation, ignoreCase = true)
                } ?: false
            }
            sectionMatches && (search.isEmpty() || listOf(
                meaning.text, meaning.situation.orEmpty(), aliases[meaning.id].orEmpty(), categoryLabels[meaning.category].orEmpty(),
            ).any { it.contains(search, ignoreCase = true) })
        }
        return when (sort) {
            LibraryBrowseSort.RECENT -> result.sortedWith(compareByDescending<MeaningEntity> { it.lastUsedAt }.thenByDescending { it.updatedAt })
            LibraryBrowseSort.ALPHABETICAL -> result.sortedBy { it.text }
            LibraryBrowseSort.CATEGORY -> result.sortedWith(compareBy<MeaningEntity> { it.category }.thenBy { it.text })
        }
    }

    private fun usable(meanings: List<MeaningEntity>, profileId: Long): List<MeaningEntity> =
        if (profileId <= 0L) emptyList() else meanings.filter {
            it.profileId == profileId && it.active && (it.confirmed || it.isSample)
        }
}
