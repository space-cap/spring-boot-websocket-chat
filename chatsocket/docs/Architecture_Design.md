# 아키텍처 설계 문서

## 개요

ChatSocket은 Spring Boot와 WebSocket을 기반으로 한 실시간 채팅 애플리케이션입니다. 멀티룸 채팅을 지원하며, RESTful API와 WebSocket을 통해 실시간 메시지 교환을 제공합니다.

## 전체 시스템 구조

```
┌─────────────────────────────────────────────────────────────┐
│                    Client (Browser)                         │
├─────────────────────────────────────────────────────────────┤
│  HTML/CSS/JavaScript (Thymeleaf Templates + Static Assets)  │
└─────────────────────┬───────────────────────────────────────┘
                      │ HTTP/WebSocket
┌─────────────────────▼───────────────────────────────────────┐
│                Spring Boot Application                      │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐  │
│  │   Controller    │  │     Handler     │  │   Config    │  │
│  │   (REST API)    │  │   (WebSocket)   │  │             │  │
│  └─────────────────┘  └─────────────────┘  └─────────────┘  │
│  ┌─────────────────┐  ┌─────────────────┐                   │
│  │     Service     │  │      Model      │                   │
│  │   (비즈니스 로직)   │  │   (데이터 모델)   │                   │
│  └─────────────────┘  └─────────────────┘                   │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                  H2 Database                                │
│              (In-Memory Database)                           │
└─────────────────────────────────────────────────────────────┘
```

## 주요 컴포넌트

### 1. Controller Layer
- **ChatController**: 채팅룸 생성, 조회 등의 REST API 제공
- **PageController**: 웹 페이지 라우팅 처리

### 2. WebSocket Handler
- **ChatWebSocketHandler**: WebSocket 연결 및 메시지 처리
- **WebSocketConfig**: WebSocket 설정 및 핸들러 등록

### 3. Service Layer
- **ChatRoomCleanupService**: 비어있는 채팅룸 정리 작업

### 4. Model Layer
- **ChatMessage**: 채팅 메시지 데이터 구조
- **ChatRoom**: 채팅룸 데이터 구조
- **ChatRoomRepository**: 채팅룸 데이터 관리

### 5. Configuration
- **WebSocketConfig**: WebSocket 연결 설정
- **PerformanceConfig**: 성능 최적화 설정

## 데이터 플로우

### 채팅룸 생성 플로우
```
Client → PageController → ChatController → ChatRoomRepository → H2 Database
   ↓
Response ← ← ← ← ←
```

### 실시간 메시지 플로우
```
Client A → WebSocket → ChatWebSocketHandler → ChatRoom
                                    ↓
Client B ← WebSocket ← ← ← ← ← ← ← ← ← ←
Client C ← WebSocket ← ← ← ← ← ← ← ← ← ←
```

### 채팅룸 정리 플로우
```
ChatRoomCleanupService → ChatRoomRepository → 빈 채팅룸 제거
         ↑
    Scheduled Task
```

## 핵심 설계 원칙

### 1. 관심사의 분리
- Controller: HTTP 요청/응답 처리
- Handler: WebSocket 연결 및 메시지 라우팅
- Service: 비즈니스 로직
- Repository: 데이터 접근
- Model: 데이터 구조 정의

### 2. 실시간 통신
- WebSocket을 통한 양방향 통신
- 세션 기반 연결 관리
- 브로드캐스트 메시지 전송

### 3. 메모리 효율성
- In-memory 데이터 저장 (H2)
- 자동 채팅룸 정리
- 세션 기반 사용자 관리

### 4. 확장성 고려
- 컴포넌트 기반 모듈 구조
- 설정 외부화
- 테스트 가능한 구조

## 보안 고려사항

### 1. WebSocket 보안
- 세션 기반 인증
- 메시지 유효성 검증
- XSS 방지

### 2. 데이터 보안
- 입력 데이터 검증
- HTML 이스케이핑
- 메모리 기반 저장 (영구 저장 없음)

## 성능 최적화

### 1. 메모리 관리
- 자동 채팅룸 정리
- 세션 기반 사용자 관리
- 효율적인 브로드캐스팅

### 2. 네트워크 최적화
- WebSocket 연결 재사용
- JSON 기반 메시지 포맷
- 클라이언트 사이드 캐싱

## 기술 스택

- **Backend**: Spring Boot 3.5.4, Spring WebSocket
- **Database**: H2 In-Memory Database
- **Frontend**: Thymeleaf, HTML5, JavaScript
- **Build Tool**: Maven
- **Java Version**: 21
- **Testing**: JUnit 5, Spring Boot Test

## 배포 아키텍처

```
┌─────────────────────────────────────────────┐
│              Load Balancer                   │
├─────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐           │
│  │   Server 1  │  │   Server 2  │  ...      │
│  │(Spring Boot)│  │(Spring Boot)│           │
│  └─────────────┘  └─────────────┘           │
└─────────────────────────────────────────────┘
```

> 참고: 현재 구현은 단일 서버 환경을 대상으로 하며, 스케일 아웃 시 세션 공유 및 메시지 브로드캐스트 동기화가 필요합니다.