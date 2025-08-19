# 기여 가이드라인

## 개요

ChatSocket 프로젝트에 기여해 주셔서 감사합니다! 이 문서는 효과적인 협업을 위한 가이드라인을 제공합니다.

## 시작하기 전에

### 1. 프로젝트 이해하기
- [README.md](../README.md) 읽기
- [아키텍처 설계 문서](Architecture_Design.md) 확인
- [코드 컨벤션 가이드](Code_Convention_Guide.md) 숙지
- 기존 이슈와 PR 검토

### 2. 개발 환경 설정
- [개발 환경 설정 가이드](Development_Environment_Setup.md) 따라하기
- Java 21, Maven 설치 확인
- IDE 설정 완료

## Git 워크플로우

### 브랜치 전략

```
main
├── develop
│   ├── feature/chat-message-filtering
│   ├── feature/user-authentication
│   └── bugfix/websocket-connection-issue
├── hotfix/security-patch
└── release/v1.1.0
```

#### 브랜치 타입

- **main**: 프로덕션 배포용 (stable)
- **develop**: 개발 통합 브랜치
- **feature/**: 새로운 기능 개발
- **bugfix/**: 버그 수정
- **hotfix/**: 긴급 수정 (main에서 직접 분기)
- **release/**: 릴리스 준비

### 브랜치 네이밍 규칙

```bash
# 기능 개발
feature/issue-number-short-description
feature/123-add-message-encryption

# 버그 수정  
bugfix/issue-number-short-description
bugfix/456-fix-memory-leak

# 핫픽스
hotfix/issue-number-critical-fix
hotfix/789-security-vulnerability

# 릴리스
release/version-number
release/v1.2.0
```

### 작업 프로세스

#### 1. 이슈 생성 및 할당
```markdown
## 이슈 제목
[FEATURE] 메시지 암호화 기능 추가

## 설명
사용자 간 메시지 전송 시 종단간 암호화 기능을 구현합니다.

## 작업 내용
- [ ] 암호화 알고리즘 선택 및 구현
- [ ] WebSocket 메시지 암호화 적용
- [ ] 클라이언트 복호화 기능 구현
- [ ] 테스트 코드 작성

## 인수 조건
- 메시지가 서버에서 암호화된 형태로 처리됨
- 클라이언트에서 올바르게 복호화됨
- 기존 기능에 영향 없음
```

#### 2. 브랜치 생성 및 작업
```bash
# develop에서 최신 코드 받기
git checkout develop
git pull origin develop

# 새 기능 브랜치 생성
git checkout -b feature/123-add-message-encryption

# 작업 수행
# ... 코드 작성 ...

# 작업 내용 커밋
git add .
git commit -m "feat: 메시지 암호화 기본 구조 구현

- AES-256 암호화 알고리즘 적용
- CryptoService 클래스 추가
- WebSocket 메시지 암호화 처리 로직 구현

Closes #123"
```

#### 3. Pull Request 생성
```bash
# 원격 브랜치에 푸시
git push origin feature/123-add-message-encryption

# GitHub에서 Pull Request 생성
```

## 커밋 메시지 규칙

### 커밋 메시지 형식

```
<type>(<scope>): <subject>

<body>

<footer>
```

#### Type (필수)
- **feat**: 새로운 기능
- **fix**: 버그 수정
- **docs**: 문서 수정
- **style**: 코드 포맷팅, 세미콜론 누락 등
- **refactor**: 코드 리팩토링
- **test**: 테스트 코드 추가/수정
- **chore**: 빌드 과정, 도구 설정 변경

#### Scope (선택)
- **controller**: 컨트롤러 관련
- **service**: 서비스 레이어
- **model**: 데이터 모델
- **config**: 설정
- **websocket**: WebSocket 관련

#### 예시
```bash
feat(websocket): 메시지 브로드캐스트 기능 구현

채팅룸 내 모든 사용자에게 메시지를 전송하는 기능을 추가했습니다.

- ChatWebSocketHandler에 브로드캐스트 로직 추가
- 메시지 타입별 처리 분기 구현
- 연결된 세션 관리 기능 개선

Closes #123
```

## Pull Request 가이드

### PR 제목 형식
```
[TYPE] 간단한 설명 (#이슈번호)

예시:
[FEATURE] 메시지 암호화 기능 추가 (#123)
[BUGFIX] WebSocket 연결 끊김 문제 수정 (#456)
[REFACTOR] 채팅룸 관리 로직 개선 (#789)
```

### PR 템플릿

```markdown
## 🚀 변경 사항
간단한 변경사항 요약을 작성해주세요.

## 📋 상세 설명
### 변경된 내용
- 구체적인 변경사항 1
- 구체적인 변경사항 2
- 구체적인 변경사항 3

### 변경 이유
왜 이 변경이 필요했는지 설명해주세요.

## 🧪 테스트
### 테스트 완료 항목
- [ ] 단위 테스트 통과
- [ ] 통합 테스트 통과
- [ ] 수동 테스트 완료
- [ ] 기존 기능 regression 테스트

### 테스트 시나리오
1. 테스트 케이스 1
2. 테스트 케이스 2

## 📸 스크린샷 (UI 변경사항이 있는 경우)
변경 전후 스크린샷을 첨부해주세요.

## ✅ 체크리스트
- [ ] 코드 컨벤션 준수
- [ ] 적절한 주석 작성
- [ ] 테스트 코드 작성
- [ ] 문서 업데이트 (필요한 경우)
- [ ] Breaking change 없음 (있다면 명시)

## 🔗 관련 이슈
Closes #이슈번호
```

### PR 크기 가이드라인
- **Small PR (권장)**: 200줄 이하, 단일 기능/수정
- **Medium PR**: 200-500줄
- **Large PR (지양)**: 500줄 초과 - 가능하면 분할

## 코드 리뷰 가이드

### 리뷰어 가이드라인

#### 1. 리뷰 체크 포인트
```markdown
## 기능성 (Functionality)
- [ ] 요구사항 충족
- [ ] 엣지 케이스 처리
- [ ] 에러 처리 적절성

## 설계 (Design)
- [ ] 코드 구조 적절성
- [ ] 디자인 패턴 적용
- [ ] 확장성 고려

## 코드 품질 (Code Quality)
- [ ] 가독성
- [ ] 중복 코드 제거
- [ ] 네이밍 적절성
- [ ] 주석 품질

## 테스트 (Testing)
- [ ] 테스트 커버리지
- [ ] 테스트 케이스 품질
- [ ] 모킹 적절성

## 성능 (Performance)
- [ ] 시간 복잡도
- [ ] 메모리 사용량
- [ ] 불필요한 연산 제거

## 보안 (Security)
- [ ] 입력 검증
- [ ] 권한 확인
- [ ] 정보 노출 방지
```

#### 2. 리뷰 댓글 가이드
```markdown
# 🔴 Must Fix (반드시 수정)
보안 취약점, 기능 오류 등 반드시 수정해야 할 사항

# 🟡 Should Fix (권장 수정)
코드 품질, 성능 개선 등 수정을 권장하는 사항

# 🔵 Could Fix (제안)
개선 아이디어, 대안 제시 등

# ✅ Good
잘 작성된 코드에 대한 긍정적 피드백

# ❓ Question
이해가 필요한 부분에 대한 질문
```

### 작성자 가이드라인

#### 1. PR 생성 전 자체 점검
```bash
# 코드 컨벤션 확인
./mvnw checkstyle:check

# 테스트 실행
./mvnw test

# 정적 분석 도구 실행 (SonarLint 등)
```

#### 2. 리뷰 피드백 대응
- 모든 댓글에 응답
- 수정 완료 시 명확한 안내
- 의견 차이가 있을 때는 토론을 통해 해결

## 이슈 관리

### 이슈 라벨 시스템

#### 타입별
- `feature`: 새로운 기능
- `bug`: 버그 리포트
- `enhancement`: 기존 기능 개선
- `documentation`: 문서 관련
- `question`: 질문

#### 우선순위별
- `priority: high`: 높은 우선순위
- `priority: medium`: 보통 우선순위
- `priority: low`: 낮은 우선순위

#### 상태별
- `status: ready`: 작업 준비 완료
- `status: in-progress`: 진행 중
- `status: blocked`: 블로킹 상태
- `status: review`: 리뷰 대기

#### 컴포넌트별
- `component: frontend`: 프론트엔드
- `component: backend`: 백엔드
- `component: websocket`: WebSocket
- `component: database`: 데이터베이스

### 이슈 템플릿

#### 버그 리포트
```markdown
## 🐛 버그 설명
명확하고 간결한 버그 설명

## 🔄 재현 단계
1. '...' 페이지로 이동
2. '...' 버튼 클릭
3. '...' 입력
4. 오류 발생

## 💭 예상 동작
어떤 동작을 예상했는지 설명

## 📸 실제 동작
실제로 발생한 동작과 스크린샷

## 🖥️ 환경 정보
- OS: [예: Windows 11]
- 브라우저: [예: Chrome 120]
- Java 버전: [예: 21]
- 프로젝트 버전: [예: v1.0.0]

## 📎 추가 정보
기타 관련 정보나 로그
```

#### 기능 요청
```markdown
## 🚀 기능 설명
원하는 기능에 대한 명확한 설명

## 💡 동기
이 기능이 필요한 이유나 해결하고자 하는 문제

## 📋 상세 요구사항
- 요구사항 1
- 요구사항 2
- 요구사항 3

## 🎨 UI/UX 제안
UI 변경이 필요한 경우 목업이나 설명

## ✅ 인수 조건
- 조건 1
- 조건 2
- 조건 3

## 📎 참고 자료
관련 링크나 자료
```

## 릴리스 프로세스

### 릴리스 계획

#### 1. 버전 넘버링 (Semantic Versioning)
```
MAJOR.MINOR.PATCH

예시:
1.0.0 - 초기 릴리스
1.1.0 - 새로운 기능 추가 (하위 호환)
1.1.1 - 버그 수정
2.0.0 - Breaking changes
```

#### 2. 릴리스 브랜치 생성
```bash
# develop에서 릴리스 브랜치 생성
git checkout develop
git pull origin develop
git checkout -b release/v1.1.0

# 버전 정보 업데이트
# pom.xml의 version 수정
# CHANGELOG.md 업데이트

git commit -m "chore: 버전 1.1.0으로 업데이트"
```

#### 3. 릴리스 테스트
```bash
# 전체 테스트 실행
./mvnw clean test

# 통합 테스트 실행
./mvnw integration-test

# 성능 테스트 실행 (필요시)
./mvnw test -Dtest=*LoadTests
```

#### 4. 릴리스 완료
```bash
# main으로 머지
git checkout main
git merge release/v1.1.0

# 태그 생성
git tag v1.1.0

# develop으로 백포트
git checkout develop
git merge release/v1.1.0

# 원격에 푸시
git push origin main
git push origin develop
git push origin v1.1.0
```

### CHANGELOG 관리

```markdown
# Changelog

## [1.1.0] - 2023-12-01

### Added
- 메시지 암호화 기능
- 사용자 온라인 상태 표시
- 채팅룸 입장/퇴장 알림

### Changed
- WebSocket 연결 성능 개선
- UI 디자인 개선

### Fixed
- 메시지 중복 전송 버그 수정
- 메모리 누수 문제 해결

### Removed
- 사용하지 않는 API 엔드포인트 제거

## [1.0.0] - 2023-11-01

### Added
- 기본 채팅 기능
- 멀티룸 지원
- REST API
```

## 도움 요청 및 소통

### 1. 질문하기
- **GitHub Issues**: 기능 관련 질문
- **GitHub Discussions**: 일반적인 토론
- **코드 리뷰**: 구현 관련 질문

### 2. 도움 받기
- 막힌 부분이 있으면 언제든 이슈나 PR에 댓글로 질문
- 메인테이너나 다른 기여자들이 도움을 드립니다
- 초보자를 위한 `good first issue` 라벨 활용

### 3. 멘토링
- 신규 기여자를 위한 멘토링 프로그램 운영
- 페어 프로그래밍 세션 제공

## 행동 강령

### 기본 원칙
- **존중**: 모든 참여자를 존중합니다
- **포용**: 다양한 배경의 사람들을 환영합니다
- **건설적**: 건설적인 피드백을 제공합니다
- **협력**: 함께 더 나은 코드를 만듭니다

### 금지 사항
- 차별적 언어나 행동
- 개인적 공격이나 괴롭힘
- 스팸이나 관련 없는 내용
- 비밀 정보 공유

---

ChatSocket 프로젝트에 기여해 주셔서 다시 한 번 감사드립니다! 
궁금한 점이 있으시면 언제든지 이슈를 생성하거나 기존 기여자들에게 문의해 주세요. 🚀