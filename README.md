# 말잇다 (Malitda) — Android

2026 장애인 분야 해커톤 「장애 플러스 기술」 분야1(디지털 포용) · 팀 VOICE MATE.
개인마다 다른 표현의 뜻을 함께 등록하고, 학생이 지금 전하려는 뜻을 확인해 전달하는 앱이다. **말하기·카드 선택 → 개인 의미 사전 검색 → 뜻 선택 → 학생 확인 → 문장·그림·소리로 전달**을 연결한다. 발음 치료나 자동 의도 확정을 제공하지 않는다.

`0.2.0`은 중간심사 피드백을 반영한 개인 의미 사전의 첫 구현이다. Whisper·Vosk 음성인식 AI는 기존 기능을 유지하며, 새로운 sLLM·Jev 의미 판단 모델은 아직 연결하지 않았다. [앱·AI·디자인 협업 안내](docs/ai-collaboration-handoff-ko.md)와 [AI 연결 계약](docs/ai-integration-contract.md)을 기준으로 추가 어댑터를 개발한다.

## 구성

| 항목 | 내용 |
|---|---|
| 언어·UI | Kotlin 2.2 · Jetpack Compose(Material3) · Navigation |
| 저장 | Room + **SQLCipher**(키는 Android Keystore가 감싼 무작위 32바이트), DataStore |
| STT(기기 내) | **whisper.cpp**(ggml-base-q5_1, JNI) · **Vosk**(vosk-model-small-ko-0.22) — 설정에서 전환, 한 번에 하나만 메모리에 올림 |
| TTS | 시스템 TextToSpeech(오프라인 한국어 음성 우선) |
| 네트워크 | **인터넷 권한 없음.** 외부 STT API·클라우드 전환 없음 |
| 화면 | 개인 의미 사전 `M_*` 흐름 + 기존 음성 입력 도구 `S01~S27` |

## 개인 의미 사전

- 지원자와 표현·전할 뜻·그림·상황을 준비한다. 뜻마다 표현을 여러 개 연결할 수 있고 같은 표현에 여러 뜻도 등록할 수 있다.
- 저장만 한 뜻은 **미확인**이다. 학생의 확인 방법을 기록하고 함께 확인한 뒤 카드와 후보에 나타난다.
- 사용할 때는 지금 전하려는 뜻을 다시 확인한다. ‘맞아요’ 이후에만 소리·공유를 사용할 수 있다.
- 학생 변경, 다시 선택, 뜻·그림 수정, 사용 중지·삭제 시 이전 승인은 해제된다. 수정과 재사용은 다시 미확인 상태를 거친다.
- 등록한 표현과 음성 입력을 완전일치로 검색한다. 일치하지 않으면 카드 선택·다시 말하기·함께 등록으로 돌아간다. 유사검색과 언어모델은 별도 실험이다.
- 한 학생당 뜻 50개, 뜻당 표현 5개까지 보관한다. 학생별 사전을 분리한다.

기존 표현·교정·활동 기록은 Room 1→2 변경 과정에서 보존한다. 개인 의미 선택은 기존 STT 교정 이력으로 자동 저장하지 않는다. 기존 음성 입력 도구는 홈의 ‘음성 입력 도구 열기’에서 사용할 수 있다.

핵심 규칙(`domain/`):
- `TextNormalizer` — M1 완전일치 판정용 정규화 3규칙(NFC·공백·문장 끝 부호).
- `SentenceCleanup` — 기획서의 "문장 정리 규칙": 비발화 토큰 제거 + 조사·어미 1회 결합. 모든 엔진·조건에 동일 적용.
- `CandidateBuilder` — 원문 + 실제 N-best(최대 3) + M1 규칙1(완전일치 승인문장)·규칙2(실제 후보 내 재정렬). 유사도로 새 문장을 만들지 않는다.
- `Approval` — 승인 토큰 = 문장 해시. 한 글자라도 바뀌면 공유 잠김.
- `MeaningApprovalSnapshot` — 학생·뜻·버전·문장·그림이 모두 같고 확인된 등록 상태일 때만 전달한다.
- `MeaningCandidateRanker` — 등록된 후보 ID 전체의 순서만 반환한다. 기본 구현은 기존 순서를 유지하며, 응답 오류·시간초과 시 같은 후보로 돌아간다.
- `EvalMetrics` — CER·WER·완전일치·Top-3·무응답·처리시간(설정 → 평가 도구, CSV 저장).

## 빌드

```bash
# 1) 모델·소스 받기(git에 없음)
bash scripts/fetch-model.sh      # Vosk 한국어 모델 → app/src/main/assets/model-ko (253MB)
bash scripts/fetch-whisper.sh    # whisper.cpp 1.9.4 → third_party/, ggml-base-q5_1.bin → app/src/main/assets/whisper (60MB)

# 2) 빌드 (JDK 17, Android SDK 36, NDK 27.2, CMake 3.22 필요; 프로젝트 경로는 ASCII 여야 함)
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease -PsubmitAbis=arm   # 제출용(arm64-v8a + armeabi-v7a), keystore.properties 필요
```

Windows: `local.properties`에 `sdk.dir=C\:/Android/Sdk` 형식으로 적는다. 슬래시를 쓰고 드라이브 뒤 콜론은 역슬래시로 이스케이프한다.

JDK 17은 해당 빌드 프로세스의 `JAVA_HOME`으로 지정한다. 최신 Android Studio의 내장 Java 버전이 프로젝트와 다를 수 있다. 메모리가 부족한 PC에서는 `--max-workers=2`를 붙여 빌드한다. 디버그 APK는 `kr.voicemate.malitda.debug`로 설치되어 기존 제출 앱과 함께 시험할 수 있다. 최종 제출용 업데이트는 팀의 기존 서명키로 릴리스 빌드를 해야 한다.

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest  # 연결한 시험 기기/에뮬레이터에서 DB 변경·저장 검증
```

## 평가 도구

디버그·릴리스 공통. 앱 전용 폴더 `files/testaudio/`에 16kHz mono PCM16 WAV와 `refs.txt`(`파일명<TAB>참조문`)를 넣고 **설정 → 전체 음원 평가 실행**. 결과는 화면과 `files/eval/eval-*.csv`.

```bash
adb push x.wav /data/local/tmp/ && adb shell "cat /data/local/tmp/x.wav | run-as kr.voicemate.malitda.debug sh -c 'cat > files/testaudio/x.wav'"
```

## 개인정보

마이크 원음성 파일과 일반 인식 문장 로그는 저장하지 않는다. 개발자가 넣은 평가 음원은 평가용 파일로 별도 취급한다. 공유창을 연 사실을 전송 성공으로 기록하지 않으며, 상대방·공유 이력을 저장하지 않는다. DB는 SQLCipher로 저장하고 Android 백업에서 제외한다. 선택한 사진은 외부 원본을 바꾸지 않고 앱 전용 폴더에 크기를 줄여 복사한다. 사진은 DB 암호화와 별개인 앱 전용 파일이다. 개인 의미 사전은 학생별로 구분하고 앱 안에서 개별 삭제할 수 있다.

## 서드파티 라이선스

- [whisper.cpp](https://github.com/ggml-org/whisper.cpp) — MIT · ggml 모델 `ggml-base-q5_1`(OpenAI Whisper, MIT)
- [Vosk](https://alphacephei.com/vosk/) `vosk-android`, `vosk-model-small-ko-0.22` — Apache-2.0
- [SQLCipher for Android](https://github.com/sqlcipher/sqlcipher-android) — BSD
- [Pretendard](https://github.com/orioncactus/pretendard) — SIL OFL 1.1 (`app/PRETENDARD-LICENSE.txt`)
- IBM Plex Sans KR — SIL OFL 1.1 (`app/IBM-PLEX-SANS-KR-LICENSE.txt`), 개인 의미 사전 화면에 사용
- 캐릭터·로고 이미지는 팀 VOICE MATE 디자인 자산이며 별도 허락 없이 재사용할 수 없습니다.

앱 소스 자체의 라이선스는 팀 결정 전까지 명시하지 않습니다(All rights reserved).
