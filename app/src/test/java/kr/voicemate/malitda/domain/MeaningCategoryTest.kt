package kr.voicemate.malitda.domain

import org.junit.Assert.*
import org.junit.Test

class MeaningCategoryTest {
    @Test fun currentAndLegacyKeysHaveAnExplicitMapping() {
        MeaningCategory.keys.forEach { assertEquals(it, MeaningCategory.normalize(it)) }
        assertEquals("people", MeaningCategory.normalize("name"))
        assertEquals("places", MeaningCategory.normalize("place"))
        assertEquals("daily", MeaningCategory.normalize("message"))
        assertEquals("often", MeaningCategory.normalize("unknown"))
        assertTrue(MeaningCategory.isIdentifier("people"))
        assertTrue(MeaningCategory.isIdentifier("places"))
        assertTrue(MeaningCategory.isIdentifier("name"))
        assertFalse(MeaningCategory.isIdentifier("daily"))
    }
}
