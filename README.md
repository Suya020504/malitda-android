# 말잇다 (Malitda) — Android

2026 장애인 분야 해커톤 「장애 플러스 기술」 분야1(디지털 포용) · 팀 VOICE MATE.
당사자의 고유한 표현을 주변 사람이 이해할 수 있게 연결하는 **온디바이스 의사소통 지원 앱**.
지원자와 함께 등록한 개인 표현("바람")을 로컬 STT 인식 결과와 맞춰 뜻 후보("산책하고 싶어요")를 제시하고,
당사자가 그림·문장·소리로 확인한 뜻만 상대방에게 전달한다. 확인 전에는 아무것도 밖으로 나가지 않는다.

## 구성 (v2 기준)

| 항목 | 내용 |
|---|---|
| 언어·UI | Kotlin 2.2 · Jetpack Compose(Material3) · 서체 Jua(v2 전 화면, Regular 단일 굵기) |
| 저장 | Room + **SQLCipher**(키는 Android Keystore가 감싼 무작위 32바이트), DataStore. **DB v2**: 개인 의미 사전 `meanings`·`meaning_expressions` 추가, 기존 데이터를 보존하는 1→2 마이그레이션 |
| STT(기기 내) | **whisper.cpp**(ggml-base-q5_1, JNI) · **Vosk**(vosk-model-small-ko-0.22) — 설정에서 전환, 한 번에 하나만 메모리에 올림 |
| TTS | 시스템 TextToSpeech(오프라인 한국어 음성 우선) — 확인된 뜻을 소리로 들려주는 선택 기능 |
| 네트워크 | **인터넷 권한 없음.** 외부 STT API·클라우드 전환 없음 |
| 화면 | **v2 42화면 체계(U00~U13)**, 디자인 전달 자료 `design/v2/` 기준. 1단계로 학생 핵심 흐름 U00~U04·U06과 예외 변형(U02-D, U03-E/M/I/V, U04-F/S) 구현 — 홈 → 말하기/카드 선택 → 뜻 확인 → 대면 전달. 등록(U05·U07)·내 표현(U08~U11)·설정(U12)·소리 확인(U13)은 다음 단계. 기존 S01~S27 구현정본은 `ui/figma/`에 유지 |

아키텍처 변경도(`design/v2` 설계도 04) 기준 유지·변경·신규:

| 구분 | 내용 |
|---|---|
| 유지 | Whisper/Vosk 음성 입력, `CandidateBuilder`(M1 승인 교정), Room+SQLCipher 기존 테이블 |
| 변경 | `SessionViewModel`(v2 의미 흐름 상태 추가), 뜻 확인·전달 흐름, TTS 전달 |
| 신규 | `MeaningResolver`, `MeaningRepository`, 개인 의미 사전 테이블(DB 1→2) |

핵심 규칙(`domain/`):
- `TextNormalizer` — M1 완전일치 판정용 정규화 3규칙(NFC·공백·문장 끝 부호).
- `SentenceCleanup` — 기획서의 "문장 정리 규칙": 비발화 토큰 제거 + 조사·어미 1회 결합. 모든 엔진·조건에 동일 적용.
- `MeaningResolver` — v2 의미 후보 결정: 인식 원문·N-best를 정규화해 등록된 표현과 맞춘다(완전일치 → 포함, 결정론적 규칙만). 등록되지 않은 뜻을 임의로 만들지 않고, 후보가 없으면 "등록된 뜻 없음" 흐름으로 보낸다.
- `CandidateBuilder` — 원문 + 실제 N-best(최대 3) + M1 규칙1(완전일치 승인문장)·규칙2(실제 후보 내 재정렬). 유사도로 새 문장을 만들지 않는다.
- `Approval` — 승인 토큰 = 문장 해시. 한 글자라도 바뀌면 공유 잠김.
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

Windows: `local.properties`에 `sdk.dir=C:/Android/Sdk` 형식(슬래시)으로 적는다.

## 평가 도구

디버그·릴리스 공통. 앱 전용 폴더 `files/testaudio/`에 16kHz mono PCM16 WAV와 `refs.txt`(`파일명<TAB>참조문`)를 넣고 **설정 → 전체 음원 평가 실행**. 결과는 화면과 `files/eval/eval-*.csv`.

```bash
adb push x.wav /data/local/tmp/ && adb shell "cat /data/local/tmp/x.wav | run-as kr.voicemate.malitda.debug sh -c 'cat > files/testaudio/x.wav'"
```

## 개인정보

원음성 파일 미저장, 인식 문장 로그 미기록, 공유 이력·상대방 미저장, 사용자별 분리 저장, 앱 안에서 개별 삭제·전체 초기화, 백업 제외(`data_extraction_rules`).

## 서드파티 라이선스

- [whisper.cpp](https://github.com/ggml-org/whisper.cpp) — MIT · ggml 모델 `ggml-base-q5_1`(OpenAI Whisper, MIT)
- [Vosk](https://alphacephei.com/vosk/) `vosk-android`, `vosk-model-small-ko-0.22` — Apache-2.0
- [SQLCipher for Android](https://github.com/sqlcipher/sqlcipher-android) — BSD
- [Jua](https://github.com/google/fonts/tree/main/ofl/jua) — SIL OFL 1.1 (`design/v2/handoff/03_fonts/Jua/OFL.txt`) · v2 전 화면 서체
- [Pretendard](https://github.com/orioncactus/pretendard) — SIL OFL 1.1 (`app/PRETENDARD-LICENSE.txt`)
- 캐릭터·로고 이미지는 팀 VOICE MATE 디자인 자산이며 별도 허락 없이 재사용할 수 없습니다.

앱 소스 자체의 라이선스는 팀 결정 전까지 명시하지 않습니다(All rights reserved).
