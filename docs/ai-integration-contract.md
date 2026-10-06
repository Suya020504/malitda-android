# 의미 후보 AI 연결 규칙

이 문서는 앱 담당자와 AI 담당자가 별도 작업을 합칠 때 사용하는 구현 계약이다. 기본 동작은 기기 안에서 정규화한 표현의 완전일치로 사전을 검색하고 기존 후보 순서를 유지한다. 유사검색은 이 기준선과 별도로 비교할 선택 항목이다. 외부 API, 인터넷 권한, sLLM 모델 파일은 이번 계약에 추가하지 않았다.

## 현재 소스에서 확인한 AI

| 항목 | 확인한 구현 | 역할과 범위 |
|---|---|---|
| Whisper | `stt/WhisperSttEngine.kt`, `stt/WhisperNative.kt`, `app/src/main/cpp/whisper_jni.cpp` | 기기 내 `whisper.cpp` 음성인식. 음성을 한국어 STT 결과로 변환 |
| Vosk | `stt/VoskSttEngine.kt`, `stt/ModelInstaller.kt` | 기기 내 한국어 모델과 `Recognizer`. 실제 인식 후보 제공 |
| 엔진 선택 | `stt/SttRouter.kt`, `di/AppContainer.kt` | Whisper·Vosk 중 하나를 준비하고 동일한 STT 인터페이스로 실행 |
| M1 승인 교정 | `domain/Candidates.kt`, `domain/Approval.kt` | 완전일치 교정과 실제 STT 후보 재정렬, 문장 승인 확인. 언어모델 추론 구현과 구분 |
| 의미 순위 연결 | `domain/MeaningRanking.kt` | 이번에 추가한 교체 가능한 계약과 규칙 기본 구현. sLLM·Jev 어댑터는 미구현 |

기존 앱에는 음성인식 AI가 있다. 기존 STT를 추가 학습한 M0·M2 모델의 Android 탑재나 의미 후보용 sLLM·Jev 연결을 이 소스만으로 완료했다고 말할 수는 없다. `AndroidManifest.xml`에는 인터넷 권한이 없고, `README.md`도 외부 STT API·클라우드 전환이 없는 로컬 구성을 명시한다.

기준 자료는 팀 공유 계획서 `말잇다_중간심사_공식피드백_반영계획_팀공유용_20261002.md`의 4.2절·6절·7절이다. 이 계획은 규칙 검색을 먼저 만들고 반복 실패가 확인된 뒤 sLLM을 비교하도록 정했다. Jev는 사용자가 말한 모델이 TypeSafe Jev인 경우의 검토안이며, 계획서도 외부 API 도입을 확정하지 않았다. 이 문서 작성 중 해당 서비스의 최신 사양이나 API를 호출해 검증하지 않았다.

## AI가 받을 것과 반환할 것

코드의 정확한 공개 타입은 아래와 같다. 숫자는 앱 내부의 로컬 식별자이며 실명·연락처·기관 식별번호가 아니다. 예시는 모두 가상 데이터다.

```kotlin
data class MeaningRankingCandidate(
    val id: Long,
    val version: Long,
    val displayText: String,
    val contextLabel: String?,
    val matchedAlias: String,
)

data class MeaningRankingRequest(
    val profileId: Long,
    val sessionId: Long,
    val rawText: String,
    val candidates: List<MeaningRankingCandidate>,
    val context: String?,
)

data class MeaningRankingResult(
    val orderedMeaningIds: List<Long>,
    val abstain: Boolean = false,
    val provider: String = "rules",
)

fun interface MeaningCandidateRanker {
    suspend fun rank(request: MeaningRankingRequest): MeaningRankingResult
}

class RuleMeaningCandidateRanker : MeaningCandidateRanker

fun validateMeaningRankingResult(
    request: MeaningRankingRequest,
    result: MeaningRankingResult?,
): MeaningRankingResult
```

앱 담당자는 같은 사용자에게 등록·확인된 `CONFIRMED` 후보만 요청에 넣는다. 비활성·삭제·미확인 항목과 다른 사용자 항목은 검색 단계에서 제외한다. 후보 ID는 요청 안에서 중복되지 않아야 한다. `version`은 요청 시점의 항목 버전으로, 의미 내용의 수정·상태 변경을 감지하는 데 사용한다.

AI 담당자는 후보 전체의 ID 순서를 반환할 수 있다. `displayText`, `rawText`, 등록 상태, 사용자, 버전을 수정할 수 없다. 새 문장·새 의미 ID를 만들어 반환하거나 사전에 추가하지 않는다. 등록 확인과 이번 대화의 당사자 선택·최종 승인은 서로 다른 절차다. 순위가 첫 번째라는 이유로 자동 선택·승인·TTS·공유하지 않는다.

`rawText`는 `SttResult.raw`에서 받은 STT 인식 결과다. 엔진이 현재 공통 문장 정리를 적용하므로 원음의 정답 전사나 가공 전 원음 자체를 뜻하지 않는다. 이 인식 결과를 기록된 의미 문장으로 덮어쓰지 않는다. `displayText`는 사용자가 확인해 등록한 의미이며 화면에서도 인식된 표현과 구분한다.

요청 예시:

```kotlin
val request = MeaningRankingRequest(
    profileId = 7L,
    sessionId = 42L,
    rawText = "바람",
    candidates = listOf(
        MeaningRankingCandidate(11L, 1L, "산책하고 싶어요", "밖에 나가기", "바람"),
        MeaningRankingCandidate(12L, 3L, "바람이 불어요", "날씨", "바람"),
    ),
    context = "날씨를 이야기하는 중",
)
```

유효한 응답과 판단 보류 예시:

```kotlin
val ranked = MeaningRankingResult(
    orderedMeaningIds = listOf(12L, 11L),
    provider = "local-experiment",
)
val abstained = MeaningRankingResult(
    orderedMeaningIds = emptyList(),
    abstain = true,
    provider = "local-experiment",
)
```

`abstain=true`는 AI가 의미를 확정하지 못했다는 뜻이다. 검증 함수는 반환 순서를 적용하지 않고 원래 후보를 모두 유지하며 `abstain` 표시를 보존한다. 당사자가 후보·다른 뜻·직접 선택·취소 중 다음 행동을 선택한다. 판단 보류를 후보 없음이나 자동 거절·승인으로 처리하지 않는다.

## 결과 검증과 실패 처리

유효한 재정렬 결과는 요청 ID 전체를 정확히 한 번씩 포함한 순열이어야 한다. 다른 ID, 중복 ID, 누락 ID, 과도한 ID, 기존 후보가 있는데 빈 목록을 반환한 결과는 전체 거절한다. 일부 ID만 골라 적용하거나 빠진 후보를 조용히 없애지 않는다. 검증 실패와 `null` 결과는 원래 후보 전체 순서와 `provider="rules"`로 복원한다.

앱 실행부는 AI 호출에 1,500ms 제한을 적용하고 일반 예외도 규칙 결과로 되돌린다. 화면 종료·사용자 전환 등으로 발생한 상위 작업 취소는 그대로 전달한다. 예시:

```kotlin
val proposed: MeaningRankingResult? = try {
    withTimeoutOrNull(1_500L) { ranker.rank(request) }
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    null
}
val safe = validateMeaningRankingResult(request, proposed)
```

어댑터는 취소 가능한 coroutine 작업으로 구현한다. 오래 걸리는 블로킹 추론은 작업 스레드로 보내고 취소·중단 방법을 갖춘다. coroutine 제한만으로 중단 불가능한 네이티브 호출이 강제로 종료된다고 가정하지 않는다. 실패 문구나 로그에 원문·개인 의미·API 키를 넣지 않는다.

최종 적용은 앱의 세션 제어부가 맡는다. 응답 직전에 활성 `profileId`·`sessionId`가 요청과 같은지, 입력·선택·상황 변경으로 요청이 폐기되지 않았는지 확인한다. 각 후보를 현재 저장소에서 다시 확인하고 요청 때와 같은 `version`이며 여전히 같은 사용자의 `CONFIRMED` 항목인지 검사한다. 이미 선택·승인한 화면은 늦은 순위 결과로 바꾸지 않는다.

현재성 검사가 실패하면 그 응답을 버리고 현재 상태로 다시 검색하거나 사용자 선택을 유지한다. 오래된 후보를 일부만 필터링해 이전 순위 결과를 적용하지 않는다. ID 목록만 반환하는 계약 자체에는 세션·버전을 다시 검증할 정보가 없으므로 요청 스냅샷을 실행부가 함께 보관해야 한다.

## 개인정보와 외부 API 경계

현재 구현은 오프라인 기본이다. 이 계약은 녹음 파일·PCM 원음·실명·연락처·기관 식별번호를 받지 않으며 외부로 보내지 않는다. 로컬 숫자 ID도 사용자 신원과 결합된 외부 식별자로 취급하지 않는다. 개인 고유 표현·의미 문장·사용 상황도 개인정보가 될 수 있으므로 미래의 외부 전송은 별도 승인과 데이터 범위 결정이 필요하다.

외부 API를 시험하기로 별도 결정한다면 로컬 타입을 그대로 외부 요청으로 직렬화하지 않는다. 실험용으로 새로 붙인 임시 후보 식별자를 쓰고 최소한의 허용된 예시만 전달한다. 실제 사용자 ID·실제 원음·동의 없는 개인 표현은 전송하지 않는다. API 키는 APK·앱 자산·저장소·Git 기록에 넣지 않는다. 키 관리가 가능한 별도 중계 서비스와 전송·보관 정책이 확정되기 전에는 온라인 어댑터를 앱에 연결하지 않는다.

## 규칙과 언어모델을 같은 조건으로 비교

먼저 규칙 구현으로 동작과 실패 사례를 기록한다. AI 어댑터 비교에는 동일한 STT 결과, 동일 사용자 사전 스냅샷, 동일 후보 집합·순서, 동일 상황 정보, 동일 확인 화면을 사용한다. STT 엔진이나 M0 모델을 동시에 바꿔 순위 개선 효과와 섞지 않는다. 시험 정답을 보고 개인 사전에 항목을 추가하지 않는다.

단일 뜻, 같은 표현의 여러 뜻, 실제 바람과 산책처럼 문맥이 갈리는 표현, 사전 미등록·빈 후보, 틀린 STT 결과, 없는 ID·중복·누락 응답, 판단 보류, 시간초과·예외, 사용자 전환·등록 수정 중 늦게 온 응답을 포함한다.

후보 Top-1·후보 내 적중률과 함께 잘못된 뜻 선택·최종 전달, 독립 확인·지원 필요·확인 불가, 판단 보류율, 전체 성공률, 대기시간과 실패 후 복구를 비교한다. 보류를 늘린 결과나 가상 시연을 실제 의사소통 효과로 설명하지 않는다. 예시·소량 실험·당사자 실증을 구분하고, 채택 또는 보류의 근거를 남긴다.

## 팀 작업 분담과 합치기

| 담당 | 소유 범위 | 합칠 때 확인할 것 |
|---|---|---|
| 앱 담당 | 사전 저장·검색, `CONFIRMED` 필터, 후보 DTO 생성, 세션·버전 검사, 확인·전달 UI, 기본 주입 | 기본 ranker는 규칙 구현, 실패에도 기존 흐름 사용, 원문·의미 분리 |
| AI 담당 | 별도 어댑터 파일, 동일 계약의 가상 사례·응답 테스트, 추론 취소와 성능 비교 기록 | 후보 ID 순열·보류만 반환, DB·승인·공유 변경 없음, 외부 API 미채택 |
| 합치는 담당 | 어댑터 선택·주입 및 코드 검토 | 후보 누락·외부 ID·오래된 응답·타임아웃 검사 후 반영 |

각 담당자는 `codex/` 등 팀이 정한 개별 작업 브랜치에서 소유 파일만 수정한다. 앱 제어부·데이터 스키마·의존성 파일을 함께 수정해야 하면 먼저 책임자와 범위를 정한다. `main`에 여러 사람이 동시에 직접 커밋하지 않고, 작은 PR로 검토한 뒤 합치는 담당자 한 명이 통합한다. 실제 Git push·PR 생성·외부 업로드는 사용자 승인 범위 안에서만 실행한다. 이번 계약 작성에서는 커밋·push·PR을 하지 않았다.
