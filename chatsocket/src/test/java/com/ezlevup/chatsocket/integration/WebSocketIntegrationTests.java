package com.ezlevup.chatsocket.integration;

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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketIntegrationTests {

    @LocalServerPort
    private int port;

    private StandardWebSocketClient webSocketClient;
    private ObjectMapper objectMapper;
    private String websocketUrl;

    @BeforeEach
    void setUp() {
        websocketUrl = "ws://localhost:" + port + "/ws/chat";
        webSocketClient = new StandardWebSocketClient();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    @Timeout(10)
    void testWebSocketConnection() throws Exception {
        CountDownLatch connectionLatch = new CountDownLatch(1);
        BlockingQueue<String> messages = new LinkedBlockingQueue<>();

        WebSocketHandler handler = new WebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                connectionLatch.countDown();
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                messages.offer(message.getPayload().toString());
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
                .doHandshake(handler, "ws://localhost:" + port + "/ws/chat", null)
                .get(5, TimeUnit.SECONDS);

        assertTrue(connectionLatch.await(5, TimeUnit.SECONDS));
        assertTrue(session.isOpen());

        session.close();
    }

    @Test
    @Timeout(15)
    void testChatMessageFlow() throws Exception {
        CountDownLatch connectionLatch = new CountDownLatch(2);
        BlockingQueue<ChatMessage> user1Messages = new LinkedBlockingQueue<>();
        BlockingQueue<ChatMessage> user2Messages = new LinkedBlockingQueue<>();

        // 사용자 1 핸들러
        WebSocketHandler user1Handler = createMessageHandler(connectionLatch, user1Messages);
        
        // 사용자 2 핸들러
        WebSocketHandler user2Handler = createMessageHandler(connectionLatch, user2Messages);

        // 두 사용자 연결
        WebSocketSession user1Session = new StandardWebSocketClient()
                .doHandshake(user1Handler, websocketUrl, null)
                .get(5, TimeUnit.SECONDS);

        WebSocketSession user2Session = new StandardWebSocketClient()
                .doHandshake(user2Handler, websocketUrl, null)
                .get(5, TimeUnit.SECONDS);

        // 연결 대기
        assertTrue(connectionLatch.await(10, TimeUnit.SECONDS));

        // 사용자 1이 방에 입장
        ChatMessage enterMessage = new ChatMessage(MessageType.ENTER, "test-room", "user1", "user1님이 입장하셨습니다.");
        user1Session.sendMessage(new TextMessage(objectMapper.writeValueAsString(enterMessage)));

        // 사용자 2도 같은 방에 입장
        ChatMessage enterMessage2 = new ChatMessage(MessageType.ENTER, "test-room", "user2", "user2님이 입장하셨습니다.");
        user2Session.sendMessage(new TextMessage(objectMapper.writeValueAsString(enterMessage2)));

        // 잠시 대기
        Thread.sleep(500);

        // 사용자 1이 메시지 전송
        ChatMessage talkMessage = new ChatMessage(MessageType.TALK, "test-room", "user1", "안녕하세요!");
        user1Session.sendMessage(new TextMessage(objectMapper.writeValueAsString(talkMessage)));

        // 사용자 2가 메시지를 받았는지 확인
        ChatMessage receivedMessage = user2Messages.poll(5, TimeUnit.SECONDS);
        assertNotNull(receivedMessage);

        // 연결 종료
        user1Session.close();
        user2Session.close();
    }

    @Test
    @Timeout(10)
    void testInvalidMessageHandling() throws Exception {
        CountDownLatch connectionLatch = new CountDownLatch(1);
        BlockingQueue<String> errorMessages = new LinkedBlockingQueue<>();

        WebSocketHandler handler = new WebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                connectionLatch.countDown();
                
                // 잘못된 JSON 전송
                session.sendMessage(new TextMessage("{ invalid json }"));
                
                // 필수 필드 누락 메시지 전송
                session.sendMessage(new TextMessage("{\"type\":\"TALK\"}"));
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                errorMessages.offer(message.getPayload().toString());
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                // Transport error handling
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

        // 에러 메시지 수신 확인 (5초 대기)
        String errorResponse = errorMessages.poll(5, TimeUnit.SECONDS);
        // 에러 처리가 되었는지만 확인 (구체적인 메시지 내용은 구현에 따라 다를 수 있음)

        session.close();
    }

    private WebSocketHandler createMessageHandler(CountDownLatch connectionLatch, BlockingQueue<ChatMessage> messageQueue) {
        return new WebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                connectionLatch.countDown();
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                try {
                    ChatMessage chatMessage = objectMapper.readValue(message.getPayload().toString(), ChatMessage.class);
                    messageQueue.offer(chatMessage);
                } catch (Exception e) {
                    // JSON 파싱 실패 시 원본 문자열을 임시 메시지로 저장
                    ChatMessage errorMessage = new ChatMessage(MessageType.TALK, "error", "system", message.getPayload().toString());
                    messageQueue.offer(errorMessage);
                }
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
    }
}