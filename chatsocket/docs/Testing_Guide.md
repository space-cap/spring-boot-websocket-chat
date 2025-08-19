# 테스트 가이드

## 개요

ChatSocket 프로젝트의 효과적인 테스트 작성 및 실행 방법을 설명합니다. 단위 테스트, 통합 테스트, 성능 테스트까지 포괄적으로 다룹니다.

## 테스트 전략

### 테스트 피라미드

```
        /\
       /  \
      / E2E \     <- End-to-End Tests (최소)
     /______\
    /        \
   /Integration\ <- Integration Tests (적당)
  /__________\
 /            \
/  Unit Tests  \ <- Unit Tests (최대)
/______________\
```

### 테스트 레벨

1. **단위 테스트 (Unit Tests)** - 개별 메소드, 클래스 테스트
2. **통합 테스트 (Integration Tests)** - 컴포넌트 간 상호작용 테스트
3. **성능 테스트 (Performance Tests)** - 부하, 스트레스 테스트

## 테스트 환경 설정

### 테스트 의존성

```xml
<!-- pom.xml에 이미 포함된 테스트 의존성 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- 추가 권장 의존성 -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

### 테스트 프로필 설정

```properties
# src/test/resources/application-test.properties
spring.application.name=chatsocket-test
spring.profiles.active=test

# 테스트용 인메모리 데이터베이스
spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
spring.jpa.hibernate.ddl-auto=create-drop

# 테스트 로깅 설정
logging.level.org.springframework.test=DEBUG
logging.level.org.springframework.web.socket=DEBUG
logging.level.com.ezlevup.chatsocket=DEBUG

# WebSocket 테스트 설정
websocket.test.timeout=5000
```

## 단위 테스트 (Unit Tests)

### 1. Controller 테스트

```java
@WebMvcTest(ChatController.class)
class ChatControllerTests {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ChatRoomRepository chatRoomRepository;
    
    @Test
    @DisplayName("모든 채팅룸 조회 성공")
    void shouldGetAllRoomsSuccessfully() throws Exception {
        // Given
        List<ChatRoom> mockRooms = Arrays.asList(
            new ChatRoom("room1", "General"),
            new ChatRoom("room2", "Random")
        );
        when(chatRoomRepository.findAll()).thenReturn(mockRooms);
        
        // When & Then
        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rooms", hasSize(2)))
                .andExpect(jsonPath("$.rooms[0].roomName", is("General")))
                .andExpect(jsonPath("$.rooms[1].roomName", is("Random")));
    }
    
    @Test
    @DisplayName("새 채팅룸 생성 성공")
    void shouldCreateRoomSuccessfully() throws Exception {
        // Given
        CreateRoomRequest request = new CreateRoomRequest("Test Room");
        ChatRoom mockRoom = new ChatRoom("room123", "Test Room");
        when(chatRoomRepository.createRoom(anyString())).thenReturn(mockRoom);
        
        // When & Then
        mockMvc.perform(post("/api/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId", is("room123")))
                .andExpect(jsonPath("$.roomName", is("Test Room")));
    }
    
    @Test
    @DisplayName("존재하지 않는 채팅룸 조회 시 404 반환")
    void shouldReturn404WhenRoomNotFound() throws Exception {
        // Given
        when(chatRoomRepository.findById("nonexistent"))
                .thenReturn(Optional.empty());
        
        // When & Then
        mockMvc.perform(get("/api/rooms/nonexistent"))
                .andExpect(status().isNotFound());
    }
}
```

### 2. Service 테스트

```java
@ExtendWith(MockitoExtension.class)
class ChatRoomCleanupServiceTests {
    
    @Mock
    private ChatRoomRepository chatRoomRepository;
    
    @InjectMocks
    private ChatRoomCleanupService cleanupService;
    
    @Test
    @DisplayName("빈 채팅룸 정리 성공")
    void shouldCleanupEmptyRoomsSuccessfully() {
        // Given
        ChatRoom emptyRoom = new ChatRoom("empty", "Empty Room");
        ChatRoom activeRoom = new ChatRoom("active", "Active Room");
        activeRoom.addUser("user1");
        
        List<ChatRoom> rooms = Arrays.asList(emptyRoom, activeRoom);
        when(chatRoomRepository.findAll()).thenReturn(rooms);
        
        // When
        cleanupService.cleanupEmptyRooms();
        
        // Then
        verify(chatRoomRepository).remove("empty");
        verify(chatRoomRepository, never()).remove("active");
    }
}
```

### 3. Model 테스트

```java
class ChatModelTests {
    
    @Test
    @DisplayName("채팅 메시지 생성 성공")
    void shouldCreateChatMessageSuccessfully() {
        // Given
        String content = "Hello, World!";
        String sender = "testUser";
        String roomId = "room123";
        
        // When
        ChatMessage message = new ChatMessage(content, sender, roomId);
        
        // Then
        assertThat(message.getContent()).isEqualTo(content);
        assertThat(message.getSender()).isEqualTo(sender);
        assertThat(message.getRoomId()).isEqualTo(roomId);
        assertThat(message.getTimestamp()).isNotNull();
        assertThat(message.getType()).isEqualTo(MessageType.CHAT);
    }
    
    @Test
    @DisplayName("채팅룸에 사용자 추가/제거 성공")
    void shouldAddAndRemoveUsersSuccessfully() {
        // Given
        ChatRoom room = new ChatRoom("room1", "Test Room");
        
        // When
        room.addUser("user1");
        room.addUser("user2");
        
        // Then
        assertThat(room.getUserCount()).isEqualTo(2);
        assertThat(room.getUsers()).containsExactly("user1", "user2");
        
        // When
        room.removeUser("user1");
        
        // Then
        assertThat(room.getUserCount()).isEqualTo(1);
        assertThat(room.getUsers()).containsExactly("user2");
    }
}
```

## 통합 테스트 (Integration Tests)

### 1. WebSocket 통합 테스트

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketIntegrationTests {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @LocalServerPort
    private int port;
    
    private StompSession stompSession;
    private final BlockingQueue<String> messages = new LinkedBlockingDeque<>();
    
    @BeforeEach
    void setup() throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(
            Arrays.asList(new WebSocketTransport(new StandardWebSocketClient()))));
        
        StompSessionHandler sessionHandler = new TestStompSessionHandler();
        stompSession = stompClient.connect(
            "ws://localhost:" + port + "/websocket", sessionHandler).get();
    }
    
    @Test
    @DisplayName("WebSocket 메시지 전송/수신 테스트")
    void shouldSendAndReceiveMessages() throws Exception {
        // Given
        String roomId = "test-room";
        ChatMessage message = new ChatMessage("Hello WebSocket!", "testUser", roomId);
        
        // 구독 설정
        stompSession.subscribe("/topic/room/" + roomId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }
            
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messages.offer((String) payload);
            }
        });
        
        // When
        stompSession.send("/app/chat/" + roomId, message);
        
        // Then
        String receivedMessage = messages.poll(5, TimeUnit.SECONDS);
        assertThat(receivedMessage).isNotNull();
        assertThat(receivedMessage).contains("Hello WebSocket!");
    }
    
    private class TestStompSessionHandler extends StompSessionHandlerAdapter {
        @Override
        public void handleException(StompSession session, StompCommand command, 
                                   StompHeaders headers, byte[] payload, Throwable exception) {
            exception.printStackTrace();
        }
    }
}
```

### 2. REST API 통합 테스트

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
class ChatControllerIntegrationTests {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    
    @BeforeEach
    void setUp() {
        chatRoomRepository.clear(); // 테스트 간 데이터 초기화
    }
    
    @Test
    @DisplayName("채팅룸 전체 워크플로우 통합 테스트")
    void shouldHandleCompleteRoomWorkflow() {
        // 1. 채팅룸 생성
        CreateRoomRequest createRequest = new CreateRoomRequest("Integration Test Room");
        ResponseEntity<RoomInfo> createResponse = restTemplate.postForEntity(
            "/api/rooms", createRequest, RoomInfo.class);
        
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody().getRoomName()).isEqualTo("Integration Test Room");
        
        String roomId = createResponse.getBody().getRoomId();
        
        // 2. 채팅룸 목록 조회
        ResponseEntity<RoomListResponse> listResponse = restTemplate.getForEntity(
            "/api/rooms", RoomListResponse.class);
        
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody().getRooms()).hasSize(1);
        assertThat(listResponse.getBody().getRooms().get(0).getRoomId()).isEqualTo(roomId);
        
        // 3. 특정 채팅룸 조회
        ResponseEntity<RoomInfo> getResponse = restTemplate.getForEntity(
            "/api/rooms/" + roomId, RoomInfo.class);
        
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().getRoomId()).isEqualTo(roomId);
    }
}
```

## 성능 테스트 (Performance Tests)

### 1. 부하 테스트

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoadTests {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    @DisplayName("동시 채팅룸 생성 부하 테스트")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldHandleConcurrentRoomCreation() throws Exception {
        int numberOfThreads = 10;
        int requestsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads * requestsPerThread);
        List<Future<ResponseEntity<RoomInfo>>> futures = new ArrayList<>();
        
        // When
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            Future<ResponseEntity<RoomInfo>> future = executor.submit(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    try {
                        CreateRoomRequest request = new CreateRoomRequest(
                            "LoadTest Room " + threadId + "-" + j);
                        ResponseEntity<RoomInfo> response = restTemplate.postForEntity(
                            "/api/rooms", request, RoomInfo.class);
                        latch.countDown();
                        return response;
                    } catch (Exception e) {
                        latch.countDown();
                        throw new RuntimeException(e);
                    }
                }
                return null;
            });
            futures.add(future);
        }
        
        // Then
        latch.await(30, TimeUnit.SECONDS);
        
        ResponseEntity<RoomListResponse> listResponse = restTemplate.getForEntity(
            "/api/rooms", RoomListResponse.class);
        
        assertThat(listResponse.getBody().getRooms().size())
            .isEqualTo(numberOfThreads * requestsPerThread);
        
        executor.shutdown();
    }
}
```

### 2. 메모리 사용량 테스트

```java
@Test
@DisplayName("대량 메시지 처리 시 메모리 사용량 테스트")
void shouldHandleMemoryUsageUnderLoad() {
    // Given
    Runtime runtime = Runtime.getRuntime();
    long initialMemory = runtime.totalMemory() - runtime.freeMemory();
    
    ChatRoom room = new ChatRoom("memory-test", "Memory Test Room");
    
    // When
    for (int i = 0; i < 10000; i++) {
        ChatMessage message = new ChatMessage(
            "Test message " + i, "user" + (i % 100), "memory-test");
        room.addMessage(message);
        
        if (i % 1000 == 0) {
            System.gc(); // 가비지 컬렉션 유도
        }
    }
    
    // Then
    long finalMemory = runtime.totalMemory() - runtime.freeMemory();
    long memoryIncrease = finalMemory - initialMemory;
    
    // 메모리 증가량이 100MB를 넘지 않아야 함
    assertThat(memoryIncrease).isLessThan(100 * 1024 * 1024);
}
```

## 테스트 실행

### Maven 명령어

```bash
# 모든 테스트 실행
./mvnw test

# 특정 테스트 클래스 실행
./mvnw test -Dtest=ChatControllerTests

# 특정 테스트 메소드 실행
./mvnw test -Dtest=ChatControllerTests#shouldCreateRoomSuccessfully

# 통합 테스트만 실행
./mvnw test -Dtest=*IntegrationTests

# 성능 테스트만 실행
./mvnw test -Dtest=*LoadTests

# 테스트 리포트와 함께 실행
./mvnw test jacoco:report
```

### IDE에서 실행

#### IntelliJ IDEA
```
1. 개별 테스트: 메소드 옆 초록색 화살표 클릭
2. 클래스 전체: 클래스명 옆 초록색 화살표 클릭
3. 패키지 전체: 패키지 우클릭 > Run Tests in 'package'
4. 전체 프로젝트: Maven 탭 > test 더블클릭
```

## 테스트 커버리지

### JaCoCo 설정

```xml
<!-- pom.xml에 추가 -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.8</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### 커버리지 확인

```bash
# 커버리지 리포트 생성
./mvnw test jacoco:report

# 리포트 위치: target/site/jacoco/index.html
```

### 커버리지 목표

- **전체 라인 커버리지**: 80% 이상
- **브랜치 커버리지**: 70% 이상
- **Controller 레이어**: 90% 이상
- **Service 레이어**: 85% 이상
- **Model 레이어**: 95% 이상

## 테스트 Best Practices

### 1. 테스트 네이밍
```java
// 좋은 테스트 이름 예시
@Test
@DisplayName("빈 채팅룸 이름으로 생성 요청 시 BadRequest 반환")
void shouldReturnBadRequestWhenRoomNameIsEmpty() { }

@Test
@DisplayName("존재하지 않는 룸ID로 조회 시 NotFound 예외 발생")
void shouldThrowNotFoundExceptionWhenRoomIdDoesNotExist() { }
```

### 2. Given-When-Then 패턴
```java
@Test
void shouldCalculateCorrectUserCount() {
    // Given (준비)
    ChatRoom room = new ChatRoom("test", "Test Room");
    
    // When (실행)
    room.addUser("user1");
    room.addUser("user2");
    
    // Then (검증)
    assertThat(room.getUserCount()).isEqualTo(2);
}
```

### 3. 테스트 데이터 빌더
```java
public class ChatMessageBuilder {
    private String content = "Default message";
    private String sender = "defaultUser";
    private String roomId = "defaultRoom";
    
    public ChatMessageBuilder withContent(String content) {
        this.content = content;
        return this;
    }
    
    public ChatMessageBuilder withSender(String sender) {
        this.sender = sender;
        return this;
    }
    
    public ChatMessage build() {
        return new ChatMessage(content, sender, roomId);
    }
}

// 사용 예시
@Test
void shouldHandleMessage() {
    ChatMessage message = new ChatMessageBuilder()
        .withContent("Hello")
        .withSender("testUser")
        .build();
    
    // 테스트 로직
}
```

이 가이드를 따라 체계적이고 효과적인 테스트를 작성하여 코드 품질을 보장해주세요.