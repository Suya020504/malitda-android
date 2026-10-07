package kr.voicemate.malitda.ui.vm

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecognitionSessionGuardTest {
    @Test
    fun cancelRejectsThePendingResult() {
        val guard = RecognitionSessionGuard()
        val pending = guard.begin(7L)
        assertTrue(guard.accepts(pending, 7L))

        guard.invalidate()

        assertFalse(guard.accepts(pending, 7L))
    }

    @Test
    fun startingAnotherRequestRejectsOldCallbacksForTheSameProfile() {
        val guard = RecognitionSessionGuard()
        val oldRequest = guard.begin(7L)
        val newRequest = guard.begin(7L)

        assertFalse(guard.accepts(oldRequest, 7L))
        assertTrue(guard.accepts(newRequest, 7L))
    }

    @Test
    fun matchingRequestNumberCannotBypassProfileIsolation() {
        val guard = RecognitionSessionGuard()
        val request = guard.begin(7L)

        assertFalse(guard.accepts(request, 9L))
        assertTrue(guard.accepts(request, 7L))
    }

    @Test
    fun returningToTheOriginalProfileDoesNotReviveItsOldRequest() {
        val guard = RecognitionSessionGuard()
        val original = guard.begin(7L)
        guard.invalidate() // 다른 프로필로 바꿀 때 세션 초기화.
        val other = guard.begin(9L)
        guard.invalidate() // 원래 프로필로 돌아와도 새 요청만 허용.
        val current = guard.begin(7L)

        assertFalse(guard.accepts(original, 7L))
        assertFalse(guard.accepts(other, 7L))
        assertTrue(guard.accepts(current, 7L))
    }

    @Test
    fun resultReturningAfterASuspendedLookupMustRecheckTheRequest() = runBlocking {
        val guard = RecognitionSessionGuard()
        val token = guard.begin(7L)
        val lookup = CompletableDeferred<Unit>()
        val delivery = async {
            if (!guard.accepts(token, 7L)) return@async false
            lookup.await()
            guard.accepts(token, 7L)
        }
        yield() // 요청이 조회 완료를 기다리는 사이 사용자가 취소한다.
        guard.invalidate()
        lookup.complete(Unit)

        assertFalse(delivery.await())
    }
}
