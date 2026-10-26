package kr.voicemate.malitda.domain

import kr.voicemate.malitda.data.db.MeaningEntity
import org.junit.Assert.*
import org.junit.Test

class LibraryBrowseTest {
    private fun meaning(id: Long, text: String = "뜻 $id", profileId: Long = 1, confirmed: Boolean = true,
                        active: Boolean = true, favorite: Boolean = false, situation: String? = null,
                        category: String = "often", updatedAt: Long = id, lastUsedAt: Long = 0, isSample: Boolean = false) =
        MeaningEntity(id = id, profileId = profileId, text = text, imageKey = null, situation = situation,
            confirmed = confirmed, active = active, createdAt = 0, updatedAt = updatedAt,
            useCount = 0, isSample = isSample, category = category, favorite = favorite, lastUsedAt = lastUsedAt)

    private fun browse(items: List<MeaningEntity>, section: LibraryBrowseSection = LibraryBrowseSection.ALL,
                       query: String = "", category: String = "", situation: String? = null,
                       sort: LibraryBrowseSort = LibraryBrowseSort.RECENT, aliases: Map<Long, String> = emptyMap(),
                       labels: Map<String, String> = emptyMap()) =
        LibraryBrowse.filter(items, 1, aliases, query, section, category, situation, sort, labels)

    @Test fun isolatesProfileBeforeSearchingTextAndAliases() {
        val mine = meaning(1, text = "내 뜻")
        val other = meaning(2, text = "다른 학생의 뜻", profileId = 2, situation = "other-only")
        val aliases = mapOf(1L to "내 별칭", 2L to "private alias")
        assertEquals(listOf(mine), browse(listOf(mine, other), aliases = aliases))
        assertTrue(browse(listOf(mine, other), query = "private alias", aliases = aliases).isEmpty())
        assertTrue(LibraryBrowse.situations(listOf(mine, other), 1).isEmpty())
        assertTrue(LibraryBrowse.filter(listOf(mine, other), 0, aliases, "", LibraryBrowseSection.ALL).isEmpty())
    }

    @Test fun excludesUnconfirmedAndInactiveIncludingInactiveSamples() {
        val confirmed = meaning(1)
        val sample = meaning(4, confirmed = false, isSample = true)
        val result = browse(listOf(confirmed, meaning(2, confirmed = false), meaning(3, active = false),
            sample, meaning(5, confirmed = false, isSample = true, active = false)))
        assertEquals(setOf(1L, 4L), result.map { it.id }.toSet())
        assertTrue(result.single { it.id == 4L }.isSample)
        assertFalse(result.single { it.id == 4L }.confirmed)
    }

    @Test fun favoritesContainsOnlyActualUsableFavorites() {
        val favorite = meaning(1, favorite = true)
        assertEquals(listOf(favorite), browse(listOf(favorite, meaning(2), meaning(3, favorite = true, active = false),
            meaning(4, favorite = true, profileId = 2)), LibraryBrowseSection.FAVORITES))
    }

    @Test fun hiddenModeFiltersDoNotCarryAcrossSections() {
        val first = meaning(1, category = "request", favorite = true, situation = "학교")
        val second = meaning(2, category = "places", situation = "집")
        val items = listOf(first, second)
        assertEquals(listOf(first), browse(items, category = "request", situation = "집"))
        assertEquals(listOf(first), browse(items, LibraryBrowseSection.FAVORITES, category = "places", situation = "집"))
        assertEquals(listOf(first), browse(items, LibraryBrowseSection.SITUATIONS, category = "places", situation = "학교"))
    }

    @Test fun situationSelectionUsesTrimmedCaseInsensitiveExactTag() {
        val school = meaning(1, situation = " 학교 ")
        val lunch = meaning(2, situation = "학교급식")
        val english = meaning(3, situation = " School ")
        assertEquals(listOf(school), browse(listOf(school, lunch), LibraryBrowseSection.SITUATIONS, situation = "학교"))
        assertEquals(listOf(english), browse(listOf(english), LibraryBrowseSection.SITUATIONS, situation = "school"))
        assertTrue(browse(listOf(school), LibraryBrowseSection.SITUATIONS, situation = "").isEmpty())
    }

    @Test fun situationMenuDeduplicatesButKeepsFirstTrimmedSpelling() {
        val items = listOf(meaning(1, situation = " ZOO "), meaning(2, situation = "zoo"),
            meaning(3, situation = " Alpha "), meaning(4, situation = "학교"),
            meaning(5, situation = "숨김", active = false), meaning(6, situation = "다른 학생", profileId = 2))
        assertEquals(listOf("Alpha", "ZOO", "학교"), LibraryBrowse.situations(items, 1))
    }

    @Test fun allSituationsExcludesBlankAndUnspecifiedWithoutChangingText() {
        val original = meaning(1, text = "  그대로 둔 문장!  ", situation = " 집 ")
        val result = browse(listOf(original, meaning(2, situation = " "), meaning(3)), LibraryBrowseSection.SITUATIONS)
        assertEquals(listOf(original), result)
        assertSame(original, result.single())
        assertEquals("  그대로 둔 문장!  ", result.single().text)
        assertEquals(" 집 ", result.single().situation)
    }

    @Test fun querySearchesOwnAliasSituationTextAndCategoryLabel() {
        val item = meaning(1, text = "물을 마시고 싶어요", situation = "쉬는 시간", category = "request")
        val aliases = mapOf(1L to "목말라, WATER")
        val labels = mapOf("request" to "요청")
        listOf("물", "쉬는", " 목말라 ", "water", "요청").forEach { query ->
            assertEquals(query, listOf(item), browse(listOf(item), query = query, aliases = aliases, labels = labels))
        }
        assertTrue(browse(listOf(item), query = "다른 뜻", aliases = aliases, labels = labels).isEmpty())
    }

    @Test fun sortingUsesActualRecentUseAlphabeticalTextAndCategory() {
        val a = meaning(1, text = "가", category = "request", lastUsedAt = 10, updatedAt = 20)
        val b = meaning(2, text = "나", category = "daily", lastUsedAt = 10, updatedAt = 30)
        val c = meaning(3, text = "다", category = "daily", lastUsedAt = 20, updatedAt = 1)
        assertEquals(listOf(c, b, a), browse(listOf(a, b, c)))
        assertEquals(listOf(a, b, c), browse(listOf(c, b, a), sort = LibraryBrowseSort.ALPHABETICAL))
        assertEquals(listOf(b, c, a), browse(listOf(a, c, b), sort = LibraryBrowseSort.CATEGORY))
    }

    @Test fun emptyAndUnusableLibrariesHaveNoSyntheticFallback() {
        assertTrue(browse(emptyList()).isEmpty())
        assertTrue(LibraryBrowse.situations(emptyList(), 1).isEmpty())
        LibraryBrowseSection.entries.forEach { section ->
            assertTrue(browse(listOf(meaning(1, confirmed = false)), section).isEmpty())
        }
    }
}
