# 코드 컨벤션 가이드

## 개요

이 문서는 ChatSocket 프로젝트의 일관성 있는 코드 작성을 위한 규칙과 가이드라인을 제공합니다.

## 네이밍 규칙

### Java 클래스 및 인터페이스

```java
// 클래스명: PascalCase
public class ChatWebSocketHandler { }
public class ChatRoomRepository { }

// 인터페이스명: PascalCase, 'I' 접두사 사용하지 않음
public interface MessageHandler { }

// 추상 클래스: Abstract 접두사 사용
public abstract class AbstractChatHandler { }

// 예외 클래스: Exception 접미사
public class ChatRoomNotFoundException extends RuntimeException { }
```

### 메소드 및 변수

```java
// 메소드명: camelCase, 동사로 시작
public void sendMessage() { }
public ChatRoom findRoomById(String id) { }
public boolean isUserConnected() { }

// 변수명: camelCase
private String roomId;
private List<ChatMessage> messageHistory;

// 상수: UPPER_SNAKE_CASE
public static final String DEFAULT_ROOM_NAME = "General";
public static final int MAX_MESSAGE_LENGTH = 1000;

// boolean 변수: is/has/can 접두사
private boolean isConnected;
private boolean hasPermission;
private boolean canSendMessage;
```

### 패키지 네이밍

```
com.ezlevup.chatsocket
├── config          // 설정 클래스
├── controller       // REST 컨트롤러
├── handler          // WebSocket 핸들러
├── model            // 데이터 모델
├── service          // 비즈니스 로직
├── repository       // 데이터 접근
├── dto              // 데이터 전송 객체
├── exception        // 커스텀 예외
└── util             // 유틸리티 클래스
```

## 코딩 스타일

### 클래스 구조 순서

```java
public class ChatController {
    // 1. 상수
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    // 2. 필드 (private)
    private final ChatRoomRepository chatRoomRepository;
    
    // 3. 생성자
    public ChatController(ChatRoomRepository chatRoomRepository) {
        this.chatRoomRepository = chatRoomRepository;
    }
    
    // 4. 공개 메소드 (public)
    public ResponseEntity<RoomListResponse> getAllRooms() {
        // ...
    }
    
    // 5. 보호된 메소드 (protected)
    protected void validateRoom(ChatRoom room) {
        // ...
    }
    
    // 6. 비공개 메소드 (private)
    private void logRoomCreation(String roomId) {
        // ...
    }
}
```

### 브레이스 스타일

```java
// 올바른 방식 (K&R 스타일)
if (condition) {
    doSomething();
} else {
    doSomethingElse();
}

// 메소드
public void methodName() {
    // 코드
}

// 클래스
public class ClassName {
    // 내용
}
```

### 들여쓰기 및 공백

```java
public class Example {
    // 들여쓰기: 4 스페이스
    public void method() {
        if (condition) {
            statement1;
            statement2;
        }
        
        // 연산자 앞뒤 공백
        int result = a + b * c;
        
        // 메소드 호출 시 콤마 뒤 공백
        method(param1, param2, param3);
        
        // 배열 초기화
        int[] array = {1, 2, 3, 4, 5};
    }
}
```

## 주석 작성 가이드

### JavaDoc 주석

```java
/**
 * 채팅룸을 생성하고 관리하는 컨트롤러
 * 
 * @author 개발자명
 * @since 1.0.0
 */
public class ChatController {
    
    /**
     * 새로운 채팅룸을 생성합니다.
     * 
     * @param request 채팅룸 생성 요청 정보
     * @return 생성된 채팅룸 정보
     * @throws IllegalArgumentException 룸 이름이 비어있는 경우
     */
    public ResponseEntity<RoomInfo> createRoom(@RequestBody CreateRoomRequest request) {
        // 구현
    }
}
```

### 인라인 주석

```java
public void processMessage(ChatMessage message) {
    // 메시지 유효성 검증
    if (message.getContent().trim().isEmpty()) {
        return;
    }
    
    // TODO: 메시지 필터링 기능 추가 예정
    String filteredContent = message.getContent();
    
    // FIXME: 성능 최적화 필요 - 대량 메시지 처리 시 병목
    broadcastToRoom(message.getRoomId(), filteredContent);
}
```

### 주석 작성 원칙

1. **코드가 무엇을 하는지보다 왜 그렇게 하는지 설명**
2. **복잡한 비즈니스 로직에 대한 설명**
3. **임시 해결책이나 알려진 이슈에 대한 TODO/FIXME**
4. **외부 API나 라이브러리 사용 시 참조 정보**

## 예외 처리

### 예외 네이밍

```java
// 도메인별 예외 클래스
public class ChatRoomNotFoundException extends RuntimeException {
    public ChatRoomNotFoundException(String roomId) {
        super("Chat room not found: " + roomId);
    }
}

public class InvalidMessageException extends RuntimeException {
    public InvalidMessageException(String message) {
        super("Invalid message: " + message);
    }
}
```

### 예외 처리 방식

```java
// 컨트롤러에서의 예외 처리
@ExceptionHandler(ChatRoomNotFoundException.class)
public ResponseEntity<String> handleRoomNotFound(ChatRoomNotFoundException e) {
    return ResponseEntity.notFound().build();
}

// 서비스에서의 예외 처리
public void joinRoom(String roomId, String userId) {
    ChatRoom room = chatRoomRepository.findById(roomId)
        .orElseThrow(() -> new ChatRoomNotFoundException(roomId));
    
    room.addUser(userId);
}
```

## 테스트 코드 컨벤션

### 테스트 클래스 네이밍

```java
// 단위 테스트: 클래스명 + Tests
public class ChatControllerTests {
    
    // 테스트 메소드명: given_when_then 패턴
    @Test
    public void givenValidRoomId_whenFindRoom_thenReturnRoom() {
        // 테스트 구현
    }
    
    @Test
    public void givenInvalidRoomId_whenFindRoom_thenThrowException() {
        // 테스트 구현
    }
}
```

### 테스트 구조

```java
@Test
public void shouldCreateRoomSuccessfully() {
    // Given (준비)
    CreateRoomRequest request = new CreateRoomRequest("Test Room");
    
    // When (실행)
    ResponseEntity<RoomInfo> response = chatController.createRoom(request);
    
    // Then (검증)
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().getRoomName()).isEqualTo("Test Room");
}
```

## JSON 및 API 컨벤션

### JSON 필드 네이밍

```json
{
  "roomId": "room-123",
  "roomName": "General Chat",
  "messageCount": 42,
  "createdAt": "2023-12-01T10:00:00Z",
  "isActive": true
}
```

### API 엔드포인트 네이밍

```
GET    /api/rooms           // 전체 룸 목록
POST   /api/rooms           // 룸 생성
GET    /api/rooms/{id}      // 특정 룸 조회
DELETE /api/rooms/{id}      // 룸 삭제
POST   /api/rooms/{id}/join // 룸 참가
```

## 설정 및 프로퍼티

### application.properties

```properties
# 애플리케이션 설정
spring.application.name=chatsocket
server.port=8080

# 웹소켓 설정
websocket.max-sessions=100
websocket.message-size-limit=1024

# 로깅 설정
logging.level.com.ezlevup.chatsocket=DEBUG
logging.pattern.console=%d{HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

## 코드 품질 체크리스트

### 커밋 전 체크사항

- [ ] 네이밍 규칙 준수
- [ ] 적절한 주석 작성
- [ ] 예외 처리 구현
- [ ] 테스트 코드 작성
- [ ] 코드 포맷팅 적용
- [ ] 불필요한 import 제거
- [ ] 하드코딩된 값 상수화

### 코드 리뷰 포인트

- [ ] 비즈니스 로직의 적절성
- [ ] 성능 고려사항
- [ ] 보안 취약점 검토
- [ ] 확장성 고려
- [ ] 코드 재사용성
- [ ] 단일 책임 원칙 준수

## IDE 설정 권장사항

### IntelliJ IDEA 설정

```
Code Style > Java:
- Indent: 4 spaces
- Continuation indent: 8 spaces
- Tab size: 4
- Use tab character: 체크 해제

Inspections:
- Java > Naming conventions: 활성화
- Java > Code style issues: 활성화
- Java > Probable bugs: 활성화
```

이 컨벤션을 준수하여 일관성 있고 유지보수 가능한 코드를 작성해주세요.