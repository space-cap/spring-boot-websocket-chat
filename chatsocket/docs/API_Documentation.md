# ChatSocket API Documentation

실시간 채팅 애플리케이션의 REST API 엔드포인트와 WebSocket 통신 방식에 대한 상세 문서입니다.

## 목차
1. [REST API 엔드포인트](#rest-api-엔드포인트)
2. [WebSocket 통신](#websocket-통신)
3. [데이터 모델](#데이터-모델)
4. [에러 처리](#에러-처리)
5. [제한사항](#제한사항)

---

## REST API 엔드포인트

### 기본 정보
- **Base URL**: `http://localhost:8080`
- **Content-Type**: `application/json`
- **HTTP Methods**: GET, POST, DELETE

### 1. 채팅방 목록 조회
채팅방 목록을 조회합니다.

```http
GET /chat/rooms
```

**응답 예시:**
```json
{
  "rooms": [
    {
      "roomId": "room123",
      "name": "일반 채팅방",
      "userCount": 5
    },
    {
      "roomId": "room456", 
      "name": "개발자 모임",
      "userCount": 12
    }
  ]
}
```

**응답 코드:**
- `200 OK`: 성공
- `500 Internal Server Error`: 서버 오류

### 2. 채팅방 생성
새로운 채팅방을 생성합니다.

```http
POST /chat/room
```

**요청 본문:**
```json
{
  "name": "새로운 채팅방"
}
```

**응답 예시:**
```json
{
  "roomId": "room789",
  "name": "새로운 채팅방", 
  "userCount": 0
}
```

**응답 코드:**
- `201 Created`: 생성 성공
- `400 Bad Request`: 잘못된 요청 (방 이름이 비어있거나 null)
- `500 Internal Server Error`: 서버 오류

### 3. 특정 채팅방 조회
특정 채팅방의 정보를 조회합니다.

```http
GET /chat/room/{roomId}
```

**경로 매개변수:**
- `roomId`: 조회할 채팅방 ID

**응답 예시:**
```json
{
  "roomId": "room123",
  "name": "일반 채팅방",
  "userCount": 5
}
```

**응답 코드:**
- `200 OK`: 조회 성공
- `404 Not Found`: 채팅방이 존재하지 않음
- `500 Internal Server Error`: 서버 오류

### 4. 채팅방 삭제
특정 채팅방을 삭제합니다.

```http
DELETE /chat/room/{roomId}
```

**경로 매개변수:**
- `roomId`: 삭제할 채팅방 ID

**응답:**
- 응답 본문 없음

**응답 코드:**
- `204 No Content`: 삭제 성공
- `404 Not Found`: 채팅방이 존재하지 않음
- `500 Internal Server Error`: 서버 오류

---

## 페이지 라우팅 엔드포인트

### 1. 메인 페이지
채팅방 목록을 표시하는 메인 페이지입니다.

```http
GET /
```

**응답**: HTML 페이지 (`index.html`)

### 2. 채팅방 페이지
특정 채팅방에 입장하는 페이지입니다.

```http
GET /room/{roomId}
```

**응답**: HTML 페이지 (`chatroom.html`)

### 3. 채팅방 생성 페이지
새로운 채팅방을 생성하는 페이지입니다.

```http
GET /create
```

**응답**: HTML 페이지 (`create-room.html`)

### 4. 테스트 페이지
WebSocket 연결 테스트 페이지입니다.

```http
GET /test
```

**응답**: HTML 페이지 (`test.html`)

### 5. 에러 페이지
오류 발생 시 표시되는 페이지입니다.

```http
GET /error
```

**응답**: HTML 페이지 (`error.html`)

---

## WebSocket 통신

### 연결 정보
- **WebSocket URL**: `ws://localhost:8080/ws/chat`
- **SockJS URL**: `http://localhost:8080/ws/chat` (SockJS 사용 시)
- **Protocol**: WebSocket / SockJS 
- **Message Format**: JSON

### 연결 설정
WebSocket은 다음과 같이 설정되어 있습니다:

- **최대 세션 수**: 1,000개
- **메시지 크기 제한**: 1KB (1024 bytes)
- **메시지 내용 제한**: 500자
- **세션 타임아웃**: 10분
- **하트비트 주기**: 25초
- **연결 해제 지연**: 5초

### 메시지 타입

#### 1. 입장 메시지 (ENTER)
사용자가 채팅방에 입장할 때 전송합니다.

**클라이언트 → 서버:**
```json
{
  "type": "ENTER",
  "roomId": "room123",
  "sender": "사용자닉네임",
  "message": null
}
```

**서버 → 모든 클라이언트:**
```json
{
  "type": "ENTER",
  "roomId": "room123", 
  "sender": "사용자닉네임",
  "message": "사용자닉네임님이 입장하셨습니다.",
  "timestamp": "2024-01-15 14:30:25"
}
```

#### 2. 채팅 메시지 (TALK)
사용자가 채팅 메시지를 전송할 때 사용합니다.

**클라이언트 → 서버:**
```json
{
  "type": "TALK",
  "sender": "사용자닉네임",
  "message": "안녕하세요!"
}
```

**서버 → 모든 클라이언트 (전송자 제외):**
```json
{
  "type": "TALK",
  "roomId": "room123",
  "sender": "사용자닉네임", 
  "message": "안녕하세요!",
  "timestamp": "2024-01-15 14:31:10"
}
```

#### 3. 퇴장 메시지 (QUIT)
사용자가 채팅방에서 퇴장할 때 전송합니다.

**클라이언트 → 서버:**
```json
{
  "type": "QUIT",
  "sender": "사용자닉네임"
}
```

**서버 → 모든 클라이언트:**
```json
{
  "type": "QUIT",
  "roomId": "room123",
  "sender": "사용자닉네임",
  "message": "사용자닉네임님이 퇴장하셨습니다.",
  "timestamp": "2024-01-15 14:35:45"
}
```

#### 4. PING 메시지
연결 상태 확인용 메시지입니다 (처리되지만 응답하지 않음).

```json
{
  "type": "PING"
}
```

### WebSocket 연결 생명주기

#### 1. 연결 수립
1. 클라이언트가 WebSocket 연결 요청
2. 서버가 최대 세션 수 확인 (1,000개 제한)
3. 연결 수락 후 세션 관리 시작

#### 2. 메시지 교환
1. 클라이언트가 ENTER 메시지로 채팅방 입장
2. 서버가 세션을 해당 채팅방에 등록
3. 채팅 메시지(TALK) 교환
4. 서버가 같은 채팅방의 모든 사용자에게 브로드캐스트

#### 3. 연결 종료
1. 클라이언트가 QUIT 메시지 전송 또는 연결 해제
2. 서버가 해당 사용자를 채팅방에서 제거
3. 다른 사용자들에게 퇴장 알림
4. 채팅방이 비어있으면 자동 삭제

---

## 데이터 모델

### ChatMessage
채팅 메시지의 기본 구조입니다.

```json
{
  "type": "ENTER|TALK|QUIT|PING",
  "roomId": "string",
  "sender": "string", 
  "message": "string",
  "timestamp": "yyyy-MM-dd HH:mm:ss"
}
```

**필드 설명:**
- `type`: 메시지 타입 (필수)
- `roomId`: 채팅방 ID (ENTER 시 필수)
- `sender`: 발신자 이름 (필수)
- `message`: 메시지 내용 (TALK 시 필수)
- `timestamp`: 메시지 생성 시간 (서버에서 자동 설정)

### RoomInfo
채팅방 정보 구조입니다.

```json
{
  "roomId": "string",
  "name": "string",
  "userCount": "number"
}
```

**필드 설명:**
- `roomId`: 채팅방 고유 ID
- `name`: 채팅방 이름
- `userCount`: 현재 접속 중인 사용자 수

### CreateRoomRequest
채팅방 생성 요청 구조입니다.

```json
{
  "name": "string"
}
```

**필드 설명:**
- `name`: 생성할 채팅방 이름 (필수, 공백 불가)

### RoomListResponse
채팅방 목록 응답 구조입니다.

```json
{
  "rooms": [
    {
      "roomId": "string",
      "name": "string", 
      "userCount": "number"
    }
  ]
}
```

---

## 에러 처리

### REST API 에러
모든 REST API는 적절한 HTTP 상태 코드를 반환합니다.

**공통 에러 코드:**
- `400 Bad Request`: 잘못된 요청
- `404 Not Found`: 리소스 없음
- `500 Internal Server Error`: 서버 내부 오류

### WebSocket 에러
WebSocket 통신 중 발생하는 에러는 시스템 메시지로 전달됩니다.

**에러 메시지 형식:**
```json
{
  "type": "TALK",
  "roomId": "system",
  "sender": "System",
  "message": "에러 메시지 내용",
  "timestamp": "yyyy-MM-dd HH:mm:ss"
}
```

**주요 에러 상황:**
- 메시지 크기 초과 (1KB 제한)
- 메시지 내용 길이 초과 (500자 제한)  
- 채팅방 입장 전 메시지 전송 시도
- 잘못된 JSON 형식
- 필수 필드 누락

### 연결 에러
- **최대 세션 수 초과**: 연결 즉시 종료 (SERVICE_OVERLOAD)
- **전송 오류**: 해당 세션 자동 제거
- **타임아웃**: 10분 후 자동 연결 해제

---

## 제한사항

### 성능 제한
- **최대 동시 연결**: 1,000개 세션
- **메시지 크기**: 최대 1KB (1024 bytes)
- **메시지 내용**: 최대 500자
- **세션 타임아웃**: 10분
- **메시지 전송 타임아웃**: 5초

### 보안 제한
- **CORS**: 모든 도메인 허용 (`*`)
- **입력 검증**: 메시지 크기 및 내용 길이 검증
- **세션 관리**: 자동 세션 정리

### 기능 제한
- **채팅 기록**: 저장되지 않음 (인메모리)
- **사용자 인증**: 구현되지 않음
- **파일 전송**: 지원하지 않음
- **개인 메시지**: 지원하지 않음 (채팅방 단위만)

---

## 사용 예시

### JavaScript WebSocket 연결 예시

```javascript
// WebSocket 연결
const socket = new WebSocket('ws://localhost:8080/ws/chat');

// 연결 성공
socket.onopen = function(event) {
    console.log('WebSocket 연결 성공');
    
    // 채팅방 입장
    const enterMessage = {
        type: 'ENTER',
        roomId: 'room123',
        sender: 'myNickname'
    };
    socket.send(JSON.stringify(enterMessage));
};

// 메시지 수신
socket.onmessage = function(event) {
    const message = JSON.parse(event.data);
    console.log('메시지 수신:', message);
    
    // UI에 메시지 표시
    displayMessage(message);
};

// 메시지 전송
function sendMessage(text) {
    const message = {
        type: 'TALK',
        sender: 'myNickname',
        message: text
    };
    socket.send(JSON.stringify(message));
}

// 연결 종료
socket.onclose = function(event) {
    console.log('WebSocket 연결 종료');
};

// 에러 처리
socket.onerror = function(error) {
    console.error('WebSocket 에러:', error);
};
```

### REST API 호출 예시

```javascript
// 채팅방 목록 조회
fetch('/chat/rooms')
    .then(response => response.json())
    .then(data => {
        console.log('채팅방 목록:', data.rooms);
    });

// 새 채팅방 생성
fetch('/chat/room', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({
        name: '새로운 채팅방'
    })
})
.then(response => response.json())
.then(room => {
    console.log('생성된 채팅방:', room);
});

// 채팅방 삭제
fetch('/chat/room/room123', {
    method: 'DELETE'
})
.then(response => {
    if (response.ok) {
        console.log('채팅방 삭제 완료');
    }
});
```

---

이 문서는 ChatSocket 애플리케이션의 API를 완전히 설명합니다. 추가 질문이나 개선 사항이 있으면 개발팀에 문의해주세요.