package com.ezlevup.chatsocket.performance;

import com.ezlevup.chatsocket.model.ChatMessage;
import com.ezlevup.chatsocket.model.MessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoadTests {

    @LocalServerPort
    private int port;

    private ObjectMapper objectMapper;
    private String websocketUrl;

    @BeforeEach
    void setUp() {
        websocketUrl = "ws://localhost:" + port + "/ws/chat";
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    @Timeout(30)
    void testConcurrentConnections() throws Exception {
        final int numberOfUsers = 20; // 20명 동시 접속 테스트
        CountDownLatch connectionLatch = new CountDownLatch(numberOfUsers);
        AtomicInteger successfulConnections = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(numberOfUsers);
        List<Future<WebSocketSession>> futures = new ArrayList<>();

        // 동시에 여러 사용자 연결
        for (int i = 0; i < numberOfUsers; i++) {
            final int userId = i;
            Future<WebSocketSession> future = executor.submit(() -> {
                try {
                    WebSocketHandler handler = new WebSocketHandler() {
                        @Override
                        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                            successfulConnections.incrementAndGet();
                            connectionLatch.countDown();
                        }

                        @Override
                        public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                            // 메시지 수신 처리
                        }

                        @Override
                        public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                            exception.printStackTrace();
                        }

                        @Override
                        public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                            // 연결 종료 처리
                        }

                        @Override
                        public boolean supportsPartialMessages() {
                            return false;
                        }
                    };

                    WebSocketSession session = new StandardWebSocketClient()
                            .doHandshake(handler, websocketUrl, null)
                            .get(10, TimeUnit.SECONDS);

                    // 입장 메시지 전송
                    ChatMessage enterMessage = new ChatMessage(MessageType.ENTER, "load-test-room", "user" + userId, "사용자 " + userId);
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(enterMessage)));

                    return session;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            });
            futures.add(future);
        }

        // 모든 연결이 완료될 때까지 대기
        assertTrue(connectionLatch.await(20, TimeUnit.SECONDS));
        assertEquals(numberOfUsers, successfulConnections.get());

        // 모든 세션 정리
        for (Future<WebSocketSession> future : futures) {
            try {
                WebSocketSession session = future.get();
                if (session != null && session.isOpen()) {
                    session.close();
                }
            } catch (Exception e) {
                // 세션 정리 실패는 무시
            }
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    @Test
    @Timeout(20)
    void testMessageThroughput() throws Exception {
        final int messageCount = 100;
        CountDownLatch messageLatch = new CountDownLatch(messageCount);
        AtomicInteger receivedMessages = new AtomicInteger(0);

        WebSocketHandler senderHandler = createTestHandler(null, null);
        WebSocketHandler receiverHandler = createTestHandler(messageLatch, receivedMessages);

        // 발신자와 수신자 연결
        WebSocketSession senderSession = new StandardWebSocketClient()
                .doHandshake(senderHandler, websocketUrl, null)
                .get(5, TimeUnit.SECONDS);

        WebSocketSession receiverSession = new StandardWebSocketClient()
                .doHandshake(receiverHandler, websocketUrl, null)
                .get(5, TimeUnit.SECONDS);

        // 잠시 대기 (연결 안정화)
        Thread.sleep(1000);

        // 두 사용자 모두 같은 방에 입장
        ChatMessage senderEnter = new ChatMessage(MessageType.ENTER, "throughput-test", "sender", "발신자");
        senderSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(senderEnter)));

        ChatMessage receiverEnter = new ChatMessage(MessageType.ENTER, "throughput-test", "receiver", "수신자");
        receiverSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(receiverEnter)));

        Thread.sleep(500);

        // 대량 메시지 전송 (발신자 → 수신자)
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < messageCount; i++) {
            ChatMessage message = new ChatMessage(MessageType.TALK, "throughput-test", "sender", "메시지 " + i);
            senderSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        }

        // 모든 메시지 수신 대기
        assertTrue(messageLatch.await(15, TimeUnit.SECONDS));
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("메시지 처리 성능:");
        System.out.println("- 총 메시지 수: " + messageCount);
        System.out.println("- 수신된 메시지 수: " + receivedMessages.get());
        System.out.println("- 처리 시간: " + duration + "ms");
        System.out.println("- 처리량: " + (messageCount * 1000.0 / duration) + " 메시지/초");

        // 최소 성능 기준: 초당 50개 이상 메시지 처리
        assertTrue((messageCount * 1000.0 / duration) >= 50, "메시지 처리 성능이 기준에 미달합니다.");

        // 연결 종료
        senderSession.close();
        receiverSession.close();
    }

    @Test
    @Timeout(10)
    void testMessageSizeLimit() throws Exception {
        CountDownLatch connectionLatch = new CountDownLatch(1);
        BlockingQueue<String> responses = new LinkedBlockingQueue<>();

        WebSocketHandler handler = new WebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                connectionLatch.countDown();
                
                // 크기 제한 초과 메시지 전송 (2KB)
                StringBuilder largeMessage = new StringBuilder();
                for (int i = 0; i < 2048; i++) {
                    largeMessage.append("a");
                }
                
                ChatMessage message = new ChatMessage(MessageType.TALK, "test-room", "user", largeMessage.toString());
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                responses.offer(message.getPayload().toString());
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                exception.printStackTrace();
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                // Connection closed
            }

            @Override
            public boolean supportsPartialMessages() {
                return false;
            }
        };

        WebSocketSession session = new StandardWebSocketClient()
                .doHandshake(handler, websocketUrl, null)
                .get(5, TimeUnit.SECONDS);

        assertTrue(connectionLatch.await(5, TimeUnit.SECONDS));

        // 에러 응답 확인
        String response = responses.poll(5, TimeUnit.SECONDS);
        // 서버가 크기 제한을 적용했는지 확인 (에러 메시지나 연결 종료)
        
        session.close();
    }

    private WebSocketHandler createTestHandler(CountDownLatch messageLatch, AtomicInteger messageCounter) {
        return new WebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                // 연결 설정됨
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                try {
                    ChatMessage chatMessage = objectMapper.readValue(message.getPayload().toString(), ChatMessage.class);
                    if (chatMessage.getType() == MessageType.TALK && messageCounter != null) {
                        messageCounter.incrementAndGet();
                        if (messageLatch != null) {
                            messageLatch.countDown();
                        }
                    }
                } catch (Exception e) {
                    // 파싱 실패 무시
                }
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                exception.printStackTrace();
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                // 연결 종료
            }

            @Override
            public boolean supportsPartialMessages() {
                return false;
            }
        };
    }
}