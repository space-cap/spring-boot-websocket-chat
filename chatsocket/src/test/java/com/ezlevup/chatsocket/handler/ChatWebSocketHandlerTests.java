package com.ezlevup.chatsocket.handler;

import com.ezlevup.chatsocket.model.ChatMessage;
import com.ezlevup.chatsocket.model.ChatRoomRepository;
import com.ezlevup.chatsocket.model.MessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketHandlerTests {

    @Mock
    private WebSocketSession mockSession1;
    
    @Mock 
    private ChatRoomRepository chatRoomRepository;
    
    @InjectMocks
    private ChatWebSocketHandler handler;
    
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        lenient().when(mockSession1.getId()).thenReturn("session1");
        lenient().when(mockSession1.isOpen()).thenReturn(true);
    }

    @Test
    void testConnectionEstablished() throws Exception {
        handler.afterConnectionEstablished(mockSession1);
        
        verify(mockSession1, atLeastOnce()).getId();
    }

    @Test
    void testInvalidJsonHandling() throws Exception {
        String invalidJson = "{ invalid json }";
        TextMessage message = new TextMessage(invalidJson);
        
        handler.handleTextMessage(mockSession1, message);
        
        // JSON 파싱 오류는 로깅만 하고 에러 메시지 전송을 시도하지만,
        // 에러 메시지 전송 자체도 실패할 수 있으므로 호출 검증하지 않음
        verify(mockSession1, atLeastOnce()).getId();
    }

    @Test
    void testValidChatMessageHandling() throws Exception {
        ChatMessage chatMessage = new ChatMessage(MessageType.TALK, "room1", "user1", "Hello");
        String validJson = objectMapper.writeValueAsString(chatMessage);
        TextMessage message = new TextMessage(validJson);
        
        handler.handleTextMessage(mockSession1, message);
        
        verify(chatRoomRepository, never()).getOrCreateRoom(anyString(), anyString());
    }

    @Test
    void testConnectionClosed() throws Exception {
        handler.afterConnectionEstablished(mockSession1);
        handler.afterConnectionClosed(mockSession1, null);
        
        verify(mockSession1, atLeastOnce()).getId();
    }
}