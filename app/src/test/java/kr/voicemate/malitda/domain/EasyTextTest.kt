package kr.voicemate.malitda.domain

import org.junit.Assert.*
import org.junit.Test

class EasyTextTest {
    @Test fun familiarWordsAreSuggestedWithoutChangingTheOriginal() {
        val original = "휴식을 취하고 싶습니다."
        assertEquals(EasyTextSuggestion(original, "쉬고 싶어요."), EasyText.suggest(original))
    }
    @Test fun formalEndingAndNegationArePreserved() {
        val result = EasyText.suggest("약을 먹고 싶지 않습니다.")!!
        assertEquals("약을 먹고 싶지 않아요.", result.text)
        assertFalse(CommunicationSafety.sensitiveChange(result.original, result.text))
    }
    @Test fun numbersDatesAndNamesStayIntact() {
        assertEquals("10월 28일 3시에 홀트에 가고 싶어요.", EasyText.suggest("10월 28일 3시에 홀트에 가고 싶습니다.")?.text)
    }
    @Test fun unknownPersonalExpressionIsNeverGuessed() {
        assertNull(EasyText.suggest("바람"))
        assertNull(EasyText.suggest("홀트"))
        assertNull(EasyText.suggest("물이 필요해요"))
    }
    @Test fun quotedTitlesLinksAndCommandsAreLeftUnchanged() {
        assertNull(EasyText.suggest("\"휴식을 취하고 싶습니다\"를 검색합니다."))
        assertNull(EasyText.suggest("https://example.com/있습니다"))
        assertNull(EasyText.suggest("test@example.com으로 전달하고 싶습니다."))
    }
    @Test fun embeddedWordIsNotBlindlyReplaced() {
        assertNull(EasyText.suggest("없습니다라는 말을 듣고 싶어요"))
        assertNull(EasyText.suggest("필요합니다학교"))
    }
    @Test fun unknownClauseAndConditionsAreNotSummarized() {
        val original = "비가 오면 가지 않고, 2번 버스를 타고 싶습니다."
        assertEquals("비가 오면 가지 않고, 2번 버스를 타고 싶어요.", EasyText.suggest(original)?.text)
    }
    @Test fun explanationOfUseCanBeMadeFamiliar() {
        assertEquals("쓰는 방법을 알려 주세요.", EasyText.suggest("이용 방법을 알려 주세요.")?.text)
    }
    @Test fun blankAndLongTextRemainForManualReview() {
        assertNull(EasyText.suggest(""))
        assertNull(EasyText.suggest("가".repeat(200) + " 필요합니다."))
    }
}
