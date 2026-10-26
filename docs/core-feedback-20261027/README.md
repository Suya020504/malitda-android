# 말잇다 0.3.3 · 기본 서비스와 피드백 통합

말하거나 직접 입력한 문장을 본인이 확인하고, 메시지·검색어·AI 질문으로 활용하는 기본 흐름을 보완했다. 개인표현 사전과 지원자 등록은 이 흐름에 함께 연결한다. 기존 홈·로고·주아와 기본 100% 배율을 유지하며, 일반 서비스의 실증 메뉴·수집은 계속 꺼져 있다.

- [화면·흐름·검증 PDF](말잇다_기본서비스와피드백_통합보고_20261027.pdf)
- [구현 내용과 개발 인계](구현과_검증_인계.md)
- [기획서·피드백 반영 대조](반영대조.csv)
- [검증 조건·APK 해시](verification.json)
- [검수 범위표](qa-coverage.csv)
- [Figma 기본 입력 흐름](https://www.figma.com/design/RFGqsEDVAjLYPTwWhqOtN0?node-id=413-13537)
- [기존 홈부터 클릭](https://www.figma.com/proto/RFGqsEDVAjLYPTwWhqOtN0?node-id=395-13325&page-id=271%3A2&scaling=scale-down&starting-point-node-id=395%3A13325)
- [기존 PR2](https://github.com/choihyungyu0/malitda-android/pull/2)

이번 추가는 선택적 수정 저장과 다음 후보 반영, 쉬운 말 비교·적용 후 재확인, 모름·중단, 학생별 수정 삭제, 초기 로딩·학생 전환 상태 정리다. 실제 당사자의 이해·의사 전달 효과와 실제 휴대전화의 인식 속도를 모두 검증했다는 뜻은 아니다.

공개 검증용 APK에는 공급자 키를 넣지 않았다. AI 그림의 실제 운영 연결과 유료 생성 품질은 별도 검증이 필요하다. APK는 로컬 `dist/core-feedback-20261027/`에 보관하며 팀 정식 서명·배포본과 구분한다.
