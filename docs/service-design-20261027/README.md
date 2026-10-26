# 말잇다 0.3.2 서비스 화면 인계

기존 홈·주아·로고·캐릭터를 살리고 원안의 기본 100% 크기를 맞췄다. 네 탭은 **홈 / 내 표현 / 함께 관리 / 설정**, 내 표현은 **전체 뜻 / 즐겨찾기 / 상황별**로 나뉜다. 함께 관리에서 실증 메뉴와 수집을 제외하고 실제 표현 준비·개인 자료 관리만 남겼다.

- [화면별 설계와 흐름 PDF](말잇다_서비스구성과화면마감_20261027.pdf)
- [레퍼런스·기획서·피드백과 부족점](서비스구성과_화면마감.md)
- [빌드·APK·검증 조건](verification.json)
- [Figma 032 마감 섹션](https://www.figma.com/design/RFGqsEDVAjLYPTwWhqOtN0?node-id=395-13295)
- [온보딩 클릭 프로토타입](https://www.figma.com/proto/RFGqsEDVAjLYPTwWhqOtN0?node-id=395-13460&page-id=271%3A2&scaling=scale-down&starting-point-node-id=395%3A13460)
- [기존 개발 PR2](https://github.com/choihyungyu0/malitda-android/pull/2)

PDF의 Figma 14개 핵심 상태와 앱의 실제 동작을 구분한다. 등록·삭제 등의 세부 기능 일부는 Figma에서 설명 상태까지만 연결했다. 전체 화면 동기화와 실제 당사자의 사용 효과를 모두 입증한 결과는 아니다.

검증용 APK는 로컬 `dist/service-design-20261027/`에 있다. Debug 서명을 사용하며 배포·병합은 하지 않았다. 기존 앱과 서명이 다르면 자료를 보존한 뒤 개발자와 설치 방식을 확인한다.
