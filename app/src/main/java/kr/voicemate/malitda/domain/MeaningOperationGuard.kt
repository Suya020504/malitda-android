package kr.voicemate.malitda.domain

import java.util.concurrent.atomic.AtomicLong

/** 다시 선택·취소·학생 변경 이후 도착한 결과는 현재 선택을 바꿀 수 없다. */
internal class MeaningOperationGuard {
    data class Token(val epoch: Long, val profileId: Long)
    private val epoch = AtomicLong()
    fun begin(profileId: Long) = Token(epoch.incrementAndGet(), profileId)
    fun accepts(token: Token, currentProfileId: Long) =
        token.epoch == epoch.get() && token.profileId == currentProfileId
}
